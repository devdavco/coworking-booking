# CoworkingBooking v2 — Diseño del backend

- **Fecha:** 2026-10-30
- **Estado:** pendiente de revisión
- **Alcance:** subproyecto 1 (backend). El frontend en Angular es un subproyecto aparte con su propio diseño.

## 1. Contexto y objetivo

Reescritura desde cero de `coworkingbooking` (Spring Boot + Supabase + Next.js), conservando sus conceptos: usuarios, espacios con tiempo de limpieza y reservas con bloqueo optimista.

El objetivo principal es **aprender Spring y AWS con buenas prácticas**: migraciones con Flyway, hash de contraseñas, autenticación con JWT, tests automatizados, Docker y despliegue con PostgreSQL en Amazon RDS.

Problemas del proyecto anterior que este diseño corrige:

| Problema anterior | Solución en v2 |
|---|---|
| Contraseñas en texto plano y expuestas en `/usuarios/all` | BCrypt; el hash nunca sale en respuestas |
| Sin validación de solapamiento de reservas | Validación en servicio + exclusion constraint en Postgres |
| `horaFinTotal` enviado por el cliente | Calculado por el servidor |
| Excepciones genéricas → HTTP 500 | `ProblemDetail` (RFC 7807) con códigos 400/401/403/404/409/422 |
| `ddl-auto=update` | Flyway + `ddl-auto=validate` |
| CORS con dominio fijo en cada controller | CORS global por variable de entorno |
| Sin autenticación | Spring Security + JWT, roles ADMIN/USUARIO |
| Sin tests | Unitarios, de servicio e integración con Testcontainers |

## 2. Repositorio y stack

Repositorio nuevo en `proyectos_backend/coworking-booking` (por ahora solo local):

```
coworking-booking/
  backend/     # este diseño
  frontend/    # Angular, subproyecto posterior
  docs/
```

**Stack:** Java 21, Spring Boot 3.5, Maven (wrapper), PostgreSQL 16, Flyway, Spring Data JPA, Spring Security + `oauth2-resource-server` (JWT), springdoc-openapi, Spring Boot Actuator, Lombok (solo en entidades), JUnit 5, AssertJ, Mockito, Testcontainers, Docker Compose.

## 3. Arquitectura

Organización **por funcionalidad, en capas** (controller → service → repository) dentro de cada paquete:

```
backend/src/main/java/co/devdavco/coworking/
  CoworkingApplication.java
  espacio/   Espacio, TipoEspacio, EspacioRepository, EspacioService, EspacioController, dto/
  usuario/   Usuario, Rol, UsuarioRepository, UsuarioService, UsuarioController, dto/
  reserva/   Reserva, EstadoReserva, ReservaRepository, ReservaService, ReservaController,
             ReglasHorario, ReservasProperties, dto/
  auth/      AuthController, AuthService, JwtService, AdminInicial
  common/    SecurityConfig, CorsConfig, GlobalExceptionHandler, excepciones de dominio
```

Convenciones:
- DTOs de entrada y salida como `record` de Java; las entidades nunca se exponen en la API.
- Los controllers solo traducen HTTP ↔ DTO. Las reglas viven en los servicios y en `ReglasHorario`.
- Los servicios lanzan excepciones de dominio (`RecursoNoEncontradoException`, `ConflictoException`, `ReglaNegocioException`). `GlobalExceptionHandler` las traduce a `ProblemDetail`.

## 4. Modelo de datos

El esquema lo crea Flyway (`backend/src/main/resources/db/migration/V1__esquema_inicial.sql`). Hibernate solo valida.

### usuarios
| columna | tipo | regla |
|---|---|---|
| id | bigint generated always as identity | PK |
| nombre | varchar(100) | not null |
| email | varchar(150) | not null, unique, se normaliza a minúsculas |
| password_hash | varchar(100) | not null, BCrypt |
| rol | varchar(20) | not null, CHECK in (`ADMIN`, `USUARIO`) |
| creado_en | timestamptz | not null, default now() |

### espacios
| columna | tipo | regla |
|---|---|---|
| id | bigint identity | PK |
| nombre | varchar(50) | not null, unique |
| tipo | varchar(30) | not null, CHECK in (`ESCRITORIO`, `SALA_REUNIONES`, `SALA_CONFERENCIAS`, `CABINA`) |
| capacidad | int | not null, CHECK > 0 |
| minutos_limpieza | int | not null, CHECK >= 0 |
| activo | boolean | not null, default true |

Los espacios no se borran: `DELETE` los desactiva para conservar el historial de reservas.

### reservas
| columna | tipo | regla |
|---|---|---|
| id | bigint identity | PK |
| usuario_id | bigint | not null, FK → usuarios |
| espacio_id | bigint | not null, FK → espacios |
| hora_inicio | timestamptz | not null |
| hora_fin | timestamptz | not null, CHECK hora_fin > hora_inicio |
| hora_fin_total | timestamptz | not null; `hora_fin + minutos_limpieza`, calculado por el servidor |
| estado | varchar(20) | not null, CHECK in (`CONFIRMADA`, `CANCELADA`) |
| version | int | not null, default 0 (`@Version`) |
| creado_en | timestamptz | not null, default now() |

Restricción anti-solapamiento (requiere `CREATE EXTENSION IF NOT EXISTS btree_gist`, soportada en RDS):

```sql
ALTER TABLE reservas ADD CONSTRAINT reservas_sin_solapamiento
  EXCLUDE USING gist (espacio_id WITH =, tstzrange(hora_inicio, hora_fin_total, '[)') WITH &&)
  WHERE (estado = 'CONFIRMADA');
```

Índices: `reservas(usuario_id)` y `reservas(espacio_id, hora_inicio)`.

"Finalizada" no se guarda como estado: se deriva (`hora_fin < now()`) y se expone como campo `finalizada` en la respuesta.

**Fechas:** se guardan en UTC (`timestamptz`), se mapean a `OffsetDateTime` y la API usa ISO-8601 con offset. Las reglas de horario y el parámetro `fecha` se interpretan en la zona horaria configurada (`America/Bogota`).

## 5. API

Prefijo `/api`. Formato JSON. Documentación en `/swagger-ui.html` (solo perfil `local`).

| Método y ruta | Acceso | Descripción | Éxito |
|---|---|---|---|
| `POST /auth/registro` | público | Crea una cuenta con rol USUARIO | 201 |
| `POST /auth/login` | público | Devuelve `{ token, expiraEn }` | 200 |
| `GET /usuarios/me` | autenticado | Datos del usuario actual | 200 |
| `GET /usuarios` | ADMIN | Lista de usuarios | 200 |
| `GET /espacios` | autenticado | Espacios activos | 200 |
| `GET /espacios/{id}` | autenticado | Detalle de un espacio activo | 200 |
| `GET /espacios/{id}/ocupacion?fecha=YYYY-MM-DD` | autenticado | Franjas `[horaInicio, horaFinTotal)` ocupadas por reservas CONFIRMADAS ese día | 200 |
| `POST /espacios` | ADMIN | Crea un espacio | 201 |
| `PUT /espacios/{id}` | ADMIN | Edita un espacio | 200 |
| `DELETE /espacios/{id}` | ADMIN | Desactiva un espacio | 204 |
| `POST /reservas` | autenticado | Crea una reserva a nombre del usuario del token | 201 |
| `GET /reservas/mias` | autenticado | Reservas propias, más recientes primero | 200 |
| `GET /reservas?espacioId=&fecha=` | ADMIN | Todas las reservas, con filtros opcionales | 200 |
| `GET /reservas/{id}` | dueño o ADMIN | Detalle | 200 |
| `POST /reservas/{id}/cancelar` | dueño (antes del inicio) o ADMIN | Cancela | 200 |

Ningún DTO de respuesta incluye `password_hash`.

### Errores
Todas las respuestas de error usan `ProblemDetail`:

| Código | Caso |
|---|---|
| 400 | Falla de `@Valid` o JSON mal formado (incluye lista de campos inválidos) |
| 401 | Sin token, token inválido o expirado; credenciales de login incorrectas |
| 403 | Rol insuficiente para la ruta |
| 404 | Recurso inexistente, espacio inactivo, o reserva ajena (para no revelar su existencia) |
| 409 | Email ya registrado; reserva solapada; cancelar una reserva ya cancelada; conflicto de `@Version` |
| 422 | Regla de horario incumplida o cancelación de una reserva ya iniciada por su dueño |

## 6. Reglas de negocio de reservas

### Crear (`ReservaService.crear`)
1. `@Valid`: `espacioId`, `horaInicio` y `horaFin` obligatorios → 400.
2. El espacio existe y está activo → si no, 404.
3. `ReglasHorario.validar(inicio, fin)` → 422 con mensaje específico si falla alguna:
   - `horaInicio` en el futuro;
   - `horaFin` posterior a `horaInicio`;
   - duración entre `duracion-minima` y `duracion-maxima`;
   - inicio y fin alineados a bloques de `bloque` minutos;
   - inicio y fin el mismo día y dentro de `[apertura, cierre]`, en la zona configurada.
4. `horaFinTotal = horaFin + minutosLimpieza`.
5. Consulta de solapamiento sobre reservas CONFIRMADAS del espacio con `[inicio, finTotal)` → si existe, 409 indicando la franja ocupada.
6. Guardar con estado `CONFIRMADA`. Si la exclusion constraint falla por concurrencia (`DataIntegrityViolationException` con el nombre `reservas_sin_solapamiento`), responder 409 con el mismo mensaje.

`ReglasHorario` no depende de Spring Data ni de la base de datos. Recibe `ReservasProperties` y un `Clock` inyectable para poder probarla con una hora fija.

### Cancelar (`ReservaService.cancelar`)
- Reserva inexistente, o ajena y el usuario no es ADMIN → 404.
- Ya cancelada → 409.
- Dueño (no ADMIN) y la reserva ya empezó → 422.
- ADMIN puede cancelar en cualquier momento.
- Resultado: estado `CANCELADA`; deja de contar para solapamientos.

## 7. Seguridad

- Spring Security stateless; CSRF desactivado (API sin cookies).
- Contraseñas con `BCryptPasswordEncoder`. Registro: contraseña de 8 a 72 caracteres (72 es el límite de BCrypt).
- `POST /auth/login` verifica credenciales y emite un JWT HS256 firmado con `JWT_SECRET` (mínimo 32 bytes). Claims: `sub` (id), `email`, `rol`. Expiración: 1 hora (configurable).
- Validación del token con `spring-boot-starter-oauth2-resource-server` (`NimbusJwtDecoder` con clave secreta), mapeando el claim `rol` a la autoridad `ROLE_<rol>`.
- Autorización por ruta en `SecurityConfig`. La propiedad de la reserva se comprueba en el servicio.
- **ADMIN inicial:** `AdminInicial` (un `ApplicationRunner`) crea un ADMIN con `ADMIN_EMAIL` y `ADMIN_PASSWORD` si no existe ningún usuario con ese email. Si las variables no están definidas, no hace nada.
- **CORS** global: orígenes desde `app.cors.origins` (`APP_CORS_ORIGINS`, separados por coma), métodos GET/POST/PUT/DELETE y header `Authorization`.
- Fuera de alcance: refresh tokens, recuperación y cambio de contraseña.

## 8. Configuración

`application.yml` común + perfiles:

- **`local`** (por defecto en desarrollo): Postgres de `docker-compose.yml` (`localhost:5432`), `show-sql`, Swagger activo, valores por defecto de desarrollo para `JWT_SECRET` y el ADMIN inicial.
- **`prod`**: todo desde variables de entorno (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `APP_CORS_ORIGINS`). Sin valores por defecto para secretos. Swagger desactivado.
- En todos: `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`, Flyway activo.

Reglas configurables, validadas con `@ConfigurationProperties` + `@Validated` (la app no arranca si son inválidas):

```yaml
app:
  reservas:
    zona-horaria: America/Bogota
    apertura: "07:00"
    cierre: "22:00"
    duracion-minima: 30m
    duracion-maxima: 4h
    bloque: 15m
  jwt:
    expiracion: 1h
```

Actuator expone solo `health` (`/actuator/health`), usado por AWS como health check.

## 9. Estrategia de tests

| Tipo | Alcance | Herramientas |
|---|---|---|
| Unitario | `ReglasHorario`, cálculo de `horaFinTotal`, reglas de cancelación | JUnit 5, AssertJ, `Clock` fijo |
| Servicio | `ReservaService`, `EspacioService` y `AuthService` con repositorios simulados | JUnit 5, Mockito |
| Integración | Endpoints completos con Postgres real: migraciones, códigos HTTP, JSON, roles, exclusion constraint | `@SpringBootTest`, MockMvc, Testcontainers (PostgreSQL 16) |

- Flujo TDD: en cada funcionalidad se escribe primero un test que falla.
- Comando único: `./mvnw verify` (requiere Docker para Testcontainers).
- Casos mínimos de integración: CRUD de espacios con y sin rol ADMIN; registro con email duplicado → 409; login incorrecto → 401; reserva válida → 201; reserva solapada → 409; reserva fuera de horario → 422; ver la reserva de otro → 404; cancelar → 200 y luego se puede reservar esa franja.

## 10. Etapas de construcción

Cada etapa termina con `./mvnw verify` en verde y un commit.

| # | Etapa | Criterio de terminado |
|---|---|---|
| 0 | Esqueleto: proyecto en `backend/`, `docker-compose.yml`, perfiles, Flyway, Actuator | `docker compose up -d` + `./mvnw spring-boot:run` → `/actuator/health` = UP |
| 1 | Espacios: migración `V1` completa, CRUD, `GlobalExceptionHandler`, validación | CRUD de espacios funcionando y probado (sin seguridad aún) |
| 2 | Usuarios y auth: registro, login, JWT, `SecurityConfig`, ADMIN inicial, CORS | Sin token → 401; USUARIO no puede crear espacios → 403 |
| 3 | Reservas: `ReglasHorario`, crear, listar, detalle, cancelar, ocupación | Todos los casos de integración de la sección 9 pasan |
| 4 | Empaquetado: Dockerfile multi-stage, ejecución local con perfil `prod` | La imagen arranca contra el Postgres de Compose |
| 5 | AWS: RDS, Secrets Manager, despliegue | Requiere su propio mini-diseño (App Runner vs ECS, costos, red) |

## 11. Fuera de alcance

Frontend (subproyecto Angular), refresh tokens, recuperación de contraseña, emails, pagos, reservas recurrentes, límite de reservas por usuario, migración de datos desde Supabase y métricas de cobertura en CI.
