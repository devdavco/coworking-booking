# CoworkingBooking v2 — Diseño del frontend

- **Fecha:** 2026-10-30
- **Estado:** pendiente de revisión
- **Alcance:** subproyecto 2 (frontend del cliente). Consume la API descrita en `2026-10-30-backend-design.md`.

## 1. Intención

Una web sencilla para que **un cliente del coworking** cree su cuenta, inicie sesión y reserve un espacio viendo qué está libre ese día.

El objetivo de aprendizaje del proyecto es desplegarlo en AWS, y esa parte la hará David. Por eso este frontend debe ser **lo más simple posible**: pocas piezas, sin librerías extra, y fácil de construir como archivos estáticos para servirlos luego desde AWS. **El código lo escribe Claude entero**, a pedido explícito de David ("escribe tu todo el codigo, mantenlo sencillo, puesto que la verdadera práctica la haré montando el proyecto en aws"). En la etapa AWS se invierten los papeles: David hace el trabajo y Claude es tutor.

### Objetivos
- Registro, login y cierre de sesión.
- Pantalla **Reservar**: el cliente elige una fecha y ve en una cuadrícula todos los espacios con sus franjas libres y ocupadas. Selecciona un rango libre de un espacio y reserva.
- Pantalla **Mis reservas**: ver las reservas propias y cancelar las que todavía no empezaron.

### Fuera de alcance
- Pantallas de administrador (gestionar espacios, ver todas las reservas). El admin sigue usando Swagger.
- Recuperar contraseña, editar perfil, refresh tokens, varios idiomas, modo oscuro.
- El despliegue en AWS (es la etapa siguiente y la hace David).

### Anti-objetivos (sería un fracaso aunque funcione)
- Que el código sea difícil de seguir: nada de librerías de estado (NgRx), librerías de componentes (Angular Material) ni capas de abstracción innecesarias.
- Que el frontend repita reglas de negocio del backend y se desincronice con ellas. Las reglas (duración, bloques, anticipación, solapamiento) las valida el backend, y el frontend muestra su mensaje.

## 2. Stack *(decisión de Claude)*

- Angular 22 (CLI 22.2.x) con Node 24, componentes standalone y signals.
- `HttpClient` con un interceptor funcional.
- CSS propio, sin frameworks.
- El runner de tests que trae Angular CLI por defecto.
- Carpeta `frontend/` en la raíz del repo `coworking-booking`.

## 3. Pantallas y navegación

| Ruta | Acceso | Contenido |
|---|---|---|
| `/login` | pública | Email y contraseña. Enlace a registro. |
| `/registro` | pública | Nombre, email y contraseña. Al registrarse, hace login automático con esas credenciales y va a `/reservar`. |
| `/reservar` | con sesión | Selector de fecha y cuadrícula de espacios por horas (ver sección 4). Es la ruta por defecto. |
| `/mis-reservas` | con sesión | Tabla de reservas propias (ver sección 5). |

- Ruta vacía y rutas desconocidas redirigen a `/reservar`.
- Un guard envía a `/login` si no hay token o si ya expiró.
- Con sesión, una barra superior muestra "Reservar", "Mis reservas", el nombre del usuario (de `GET /api/usuarios/me`) y "Salir".

## 4. Pantalla Reservar

### Datos
1. Selector de fecha (`<input type="date">`). Por defecto muestra hoy; permite desde hoy hasta hoy + 90 días.
2. Al cargar la pantalla o cambiar la fecha: `GET /api/espacios` y, en paralelo, `GET /api/espacios/{id}/ocupacion?fecha=...` para cada espacio. Con pocos espacios, una llamada por espacio es aceptable. Si alguna de esas llamadas falla, no se muestra una cuadrícula a medias: aparece un mensaje de error con un botón "Reintentar". *(decisión de Claude)*

### Cuadrícula
- Una fila por espacio, con nombre, tipo y capacidad.
- Columnas: bloques de 15 minutos de 07:00 a 22:00 (60 bloques). Las horas en punto se rotulan arriba.
- Cada bloque tiene uno de estos estados:
  - **ocupado**: cae dentro de alguna franja de `ocupacion`. Las franjas ya incluyen la limpieza.
  - **pasado**: si la fecha es hoy, el bloque empieza antes de la hora actual.
  - **seleccionado**: está dentro de la selección del cliente.
  - **libre**: cualquier otro.
- En pantallas angostas la cuadrícula se desplaza horizontalmente.

### Selección y reserva
- Primer clic en un bloque libre: marca el inicio. Segundo clic en un bloque libre de la **misma fila** y posterior o igual al inicio: marca el último bloque. El rango va del inicio del primer bloque al final del último.
- Clic en otra fila, en un bloque anterior al inicio de la misma fila, o rango que atraviesa un bloque ocupado o pasado: la selección empieza de nuevo desde ese clic. *(decisión de Claude)*
- Con un rango seleccionado aparece un resumen ("Sala Andes · 11:00 – 12:30") y el botón **Reservar**, que llama a `POST /api/reservas`.
- Éxito: mensaje de confirmación, se borra la selección y se recarga la cuadrícula.
- Error: se muestra el `detail` del ProblemDetail tal cual (por ejemplo, "La reserva debe durar al menos 30 minutos"), y se recarga la cuadrícula.
- La duración mínima y máxima no se valida en el frontend, porque el backend ya responde con un mensaje claro.

### Zona horaria
El coworking está en Bogotá, que no tiene horario de verano. El frontend trabaja siempre en hora de Bogotá, sin importar la zona del navegador:
- envía las horas como `YYYY-MM-DDTHH:mm:00-05:00`;
- muestra las horas que llegan de la API convertidas a `-05:00`.

El desfase `-05:00` y el rango 07:00–22:00 son constantes en un solo archivo de configuración del frontend.

## 5. Pantalla Mis reservas

- `GET /api/reservas/mias`.
- Tabla con espacio, fecha, horario (inicio y fin, sin limpieza) y estado: "Confirmada", "Cancelada" o "Finalizada" (usa el campo `finalizada`).
- Botón **Cancelar** solo en reservas CONFIRMADAS que todavía no empezaron. Pide confirmación con `confirm()` y llama a `POST /api/reservas/{id}/cancelar`. Luego recarga la lista.
- Si no hay reservas, un mensaje con un enlace a `/reservar`.

## 6. Autenticación y errores

- El login guarda `token` y `expiraEn` en `localStorage`. La sesión es válida mientras `expiraEn` sea futuro.
- Un interceptor agrega `Authorization: Bearer <token>` a toda petición a la API, excepto login y registro.
- Si la API responde 401 a una petición con token, se borra la sesión y se navega a `/login` con el mensaje "Tu sesión expiró, vuelve a iniciar sesión".
- **Formularios** (login y registro): los errores de campo (`errores` del ProblemDetail 400) se muestran bajo cada campo; los demás, como texto arriba del formulario usando `detail`.
- Si la API no responde (error de red): "No se pudo conectar con el servidor".

## 7. Configuración y build

- `src/environments/environment.ts` (desarrollo): `apiUrl: 'http://localhost:8080'`.
- `environment.prod.ts`: `apiUrl` con un valor a reemplazar en la etapa AWS.
- `ng serve` usa el puerto 4200, que ya está en `APP_CORS_ORIGINS` del perfil `local` del backend.
- `ng build` genera archivos estáticos en `frontend/dist/frontend/browser/` (la ruta por defecto de Angular), listos para servir desde S3 + CloudFront u otro hosting estático. *(decisión de Claude)*
- **Rutas normales** (`/reservar`, `/mis-reservas`), sin `#`. Decisión de David. Consecuencia: el hosting estático debe devolver `index.html` para cualquier ruta desconocida; si no, recargar `/mis-reservas` da 404. En S3 + CloudFront se resuelve con una respuesta de error personalizada (403/404 → `/index.html` con código 200). Configurarlo es parte de la práctica de AWS de David, con servicios dentro de la capa gratuita y con fines académicos.
- `frontend/README.md` explica cómo instalar, correr y construir, y deja anotado este requisito del hosting.

## 8. Tests

Pocos y solo para la lógica que se puede romper sin que se note:
- cálculo de bloques y de su estado a partir de las franjas de ocupación y de la hora actual;
- validación de una selección (misma fila, sin bloques ocupados ni pasados) y su conversión a `horaInicio` y `horaFin`;
- validez de la sesión según `expiraEn`.

Además, una prueba manual completa contra el backend local antes de terminar: registro, login, reservar, ver el error de cruce, cancelar y cerrar sesión.

## 9. Lo que queda a criterio de quien construya
Nombres de componentes y servicios, estructura de carpetas dentro de `src/app`, estilos y colores, y textos menores de la interfaz.
