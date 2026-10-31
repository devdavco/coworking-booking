# CoworkingBooking

Reservas de espacios de un coworking: salas, cabinas y escritorios, con tiempo de limpieza entre reservas y sin cruces.

- `backend/`: API REST en Spring Boot 3.5 (Java 21), PostgreSQL 16, Flyway y JWT. Ver [`backend/README.md`](backend/README.md).
- `frontend/`: web en Angular 22 para clientes y administrador. Ver [`frontend/README.md`](frontend/README.md).
- Diseños: [backend](docs/superpowers/specs/2026-10-30-backend-design.md) y [frontend](docs/superpowers/specs/2026-10-30-frontend-design.md).

## Probarlo todo en local

Requisitos: Docker, JDK 21 y Node 24. Usa dos terminales.

```bash
# Terminal 1: base de datos y backend
cd backend
docker compose up -d
SPRING_PROFILES_ACTIVE=local JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run

# Terminal 2: frontend
cd frontend
npm install
npx ng serve
```

Abre http://localhost:4200.

- **Cliente:** crea una cuenta en "Regístrate".
- **Administrador:** `admin@coworking.local` / `admin12345`. Crea primero algunos espacios en "Espacios" para poder reservar.
- **Swagger del backend:** http://localhost:8080/swagger-ui.html

## Desplegarlo en AWS (capa gratuita)

Esta es la parte práctica del proyecto. Aquí está solo lo que hace falta; los pasos los defines tú.

| Pieza | Servicio sugerido | Qué necesita |
|---|---|---|
| Base de datos | RDS PostgreSQL 16 (`db.t4g.micro` o `db.t3.micro`) | Sin acceso público; solo el backend entra al puerto 5432. |
| Secretos | Secrets Manager o Parameter Store | `DB_PASSWORD`, `JWT_SECRET` (`openssl rand -base64 48`), `ADMIN_PASSWORD`. |
| Backend | La imagen de `backend/Dockerfile` en App Runner, ECS o una EC2 | Variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` y `APP_CORS_ORIGINS` (la URL del frontend). Perfil `prod` (ya viene en la imagen). Health check: `/actuator/health`. |
| Frontend | S3 + CloudFront | `apiUrl` en `frontend/src/environments/environment.ts` antes de `ng build`. Errores 403 y 404 → `/index.html` con código 200. |

- **HTTPS:** CloudFront sirve el frontend con HTTPS, así que el backend también debe tenerlo, o el navegador bloqueará las llamadas.
- **Costos:** revisa los límites de la capa gratuita de cada servicio antes de crearlo. Por ejemplo, App Runner no está en la capa gratuita y una EC2 `t3.micro` sí.
