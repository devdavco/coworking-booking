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
- Para el **ADMIN**, dos pantallas más: **Espacios** (crear, editar, desactivar, reactivar) y **Todas las reservas** (filtrar y cancelar). Agregadas a pedido de David, manteniéndolas igual de sencillas.

### Fuera de alcance
- Gestión de usuarios desde la web: cambiar roles, crear otros administradores o borrar cuentas.
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
| `/admin/espacios` | ADMIN | Gestión de espacios (ver sección 5b). |
| `/admin/reservas` | ADMIN | Todas las reservas (ver sección 5b). |

- Ruta vacía y rutas desconocidas redirigen a `/reservar`.
- Un guard envía a `/login` si no hay token o si ya expiró.
- Con sesión, una barra superior muestra "Reservar", "Mis reservas", el nombre del usuario (de `GET /api/usuarios/me`) y "Salir". Si el rol de `/usuarios/me` es ADMIN, muestra además "Espacios" y "Todas las reservas".
- Un segundo guard protege `/admin/*`: si el usuario no es ADMIN, lo envía a `/reservar`. El backend vuelve a comprobar el rol en cada petición, así que el guard solo evita mostrar pantallas inútiles.
- El ADMIN también puede reservar desde `/reservar` como cualquier cliente.

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

## 5b. Pantallas de administrador

### Espacios (`/admin/espacios`)
- Tabla con todos los espacios, activos e inactivos: nombre, tipo, capacidad, minutos de limpieza y estado.
- Arriba de la tabla, un formulario único con nombre, tipo (lista desplegable con los 4 tipos), capacidad y minutos de limpieza:
  - sin espacio seleccionado, **Crear** llama a `POST /api/espacios`;
  - al pulsar **Editar** en una fila, el formulario se llena con sus datos y **Guardar** llama a `PUT /api/espacios/{id}`. Un botón **Cancelar** vuelve al modo crear.
- En cada fila: **Desactivar** (con `confirm()`, `DELETE /api/espacios/{id}`) si está activo, o **Reactivar** (`POST /api/espacios/{id}/activar`) si está inactivo.
- Los errores se muestran como en los demás formularios: por campo para el 400 y con `detail` para el resto, por ejemplo el 409 de nombre duplicado.

### Todas las reservas (`/admin/reservas`)
- Filtros: fecha (por defecto hoy, se puede vaciar para ver todas) y espacio (lista desplegable opcional). Llama a `GET /api/reservas?fecha=&espacioId=`.
- Tabla con espacio, cliente, fecha, horario y estado. El cliente se muestra con nombre y email, cruzando `usuarioId` con `GET /api/usuarios`.
- **Cancelar** (con `confirm()`) en toda reserva CONFIRMADA, aunque ya haya empezado: el ADMIN puede hacerlo según el backend.

### Cambio necesario en el backend *(decisión de Claude)*
`GET /api/espacios` solo devuelve espacios activos, así que sin un cambio el ADMIN no podría ver ni reactivar los inactivos. Se agrega:
- `GET /api/espacios/todos`, solo ADMIN, con activos e inactivos ordenados por nombre;
- el campo `activo` en `EspacioResponse` (campo nuevo, no rompe a quien ya lo usa).

Se construye con test primero, como el resto del backend, y se agrega al diseño del backend.

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
- validez de la sesión según `expiraEn`;
- en el backend, el endpoint nuevo `GET /api/espacios/todos` (incluye inactivos, 403 para USUARIO).

Además, una prueba manual completa contra el backend local antes de terminar:
- como cliente: registro, login, reservar, ver el error de cruce, cancelar y cerrar sesión;
- como ADMIN (`admin@coworking.local`): crear, editar, desactivar y reactivar un espacio, ver todas las reservas y cancelar la de un cliente;
- un cliente que escribe `/admin/espacios` a mano termina en `/reservar`.

## 8b. Cómo existe el ADMIN

No hay registro de administradores desde la web. Lo crea el backend al arrancar (`AdminInicial`) con las variables `ADMIN_EMAIL` y `ADMIN_PASSWORD`, si ese email todavía no existe:
- en local, el perfil `local` ya trae `admin@coworking.local` / `admin12345`;
- en AWS, David define esas dos variables en el servicio donde corra el backend, con el valor guardado en Secrets Manager.

El ADMIN inicia sesión en la misma pantalla de login que un cliente; el frontend sabe que es ADMIN por el rol que devuelve `/usuarios/me`.

## 9. Lo que queda a criterio de quien construya
Nombres de componentes y servicios, estructura de carpetas dentro de `src/app`, estilos y colores, y textos menores de la interfaz.
