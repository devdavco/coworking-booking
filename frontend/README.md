# Frontend — CoworkingBooking

Web en Angular 22 para que los clientes reserven espacios del coworking y para que el ADMIN gestione espacios y reservas.

## Requisitos

- Node 24 y npm
- El backend corriendo en `http://localhost:8080` con `SPRING_PROFILES_ACTIVE=local` (ver `../backend/README.md`)

## Desarrollo local

```bash
npm install
npx ng serve            # http://localhost:4200
```

- `ng serve` usa `src/environments/environment.development.ts`, que apunta a `http://localhost:8080`.
- El perfil `local` del backend ya permite CORS desde `http://localhost:4200`.
- Admin local: `admin@coworking.local` / `admin12345`. Los clientes se registran desde `/registro`.

## Tests

```bash
npx ng test --watch=false
```

Cubren la lógica que puede fallar sin que se note: horas de Bogotá, estados de la cuadrícula, selección de bloques, sesión y mensajes de error.

## Estructura

| Carpeta | Qué hay |
|---|---|
| `src/app/config.ts` | Constantes del coworking (zona `-05:00`, 07:00–22:00, bloques de 15 min, 90 días). Deben coincidir con `app.reservas` del backend. |
| `src/app/api/` | Modelos, una función por endpoint (`ApiService`) y mensajes de error. |
| `src/app/auth/` | Login, registro, sesión en `localStorage`, interceptor del token y guards. |
| `src/app/reservar/` | Cuadrícula por fecha y la lógica de horas y bloques. |
| `src/app/mis-reservas/` | Reservas del cliente. |
| `src/app/admin/` | Espacios y todas las reservas (solo ADMIN). |

## Build para producción (AWS)

1. Cambia `apiUrl` en `src/environments/environment.ts` por la URL pública del backend, con HTTPS y sin barra final. Por ejemplo `https://api.midominio.com`.
2. Construye:
   ```bash
   npx ng build
   ```
3. Sube el **contenido** de `dist/frontend/browser/` a tu hosting estático (por ejemplo, un bucket S3 detrás de CloudFront).

### Requisitos del hosting estático

- **Rutas normales:** la app usa rutas como `/mis-reservas`, sin `#`. Si alguien recarga esa página o abre un enlace directo, el hosting debe devolver `index.html` con código 200; si no, devuelve 404.
  - En CloudFront: *Error pages* → respuestas personalizadas para **403** y **404** → ruta `/index.html`, código de respuesta **200**.
- **CORS:** el backend solo acepta peticiones de los orígenes en `APP_CORS_ORIGINS`. Agrega ahí la URL del frontend, por ejemplo `https://d1234abcd.cloudfront.net`.
- **HTTPS:** el frontend en HTTPS no puede llamar a un backend en HTTP (el navegador lo bloquea). El backend también necesita HTTPS.
