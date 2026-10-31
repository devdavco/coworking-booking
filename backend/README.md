# Backend — CoworkingBooking

API REST de reservas de espacios de coworking (Spring Boot 3.5, Java 21, PostgreSQL 16).

## Requisitos

- JDK 21 (`/usr/libexec/java_home -v 21` debe devolver una ruta)
- Docker

## Desarrollo local

```bash
docker compose up -d                                   # PostgreSQL en localhost:5432
SPRING_PROFILES_ACTIVE=local JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run
```

No hay perfil por defecto a propósito: sin `SPRING_PROFILES_ACTIVE` la app exige `JWT_SECRET` y no arranca
con el secreto de desarrollo.

- API: http://localhost:8080/api
- Swagger: http://localhost:8080/swagger-ui.html
- Admin local: `admin@coworking.local` / `admin12345`

## Tests

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw verify
```

Los tests de integración levantan un PostgreSQL propio con Testcontainers (Docker debe estar corriendo).

## Migraciones

El esquema lo crea Flyway desde `src/main/resources/db/migration`. Para cambiarlo, agrega un archivo nuevo
`V<n>__descripcion.sql`; nunca edites una migración ya aplicada.

## Producción (perfil `prod`)

```bash
docker build -t coworking-backend .
```

| Variable | Ejemplo |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host>:5432/coworking` |
| `DB_USER` / `DB_PASSWORD` | credenciales de la base |
| `JWT_SECRET` | 32+ caracteres aleatorios (`openssl rand -base64 48`) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | administrador inicial (opcional) |
| `APP_CORS_ORIGINS` | orígenes del frontend separados por coma |
