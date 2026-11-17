# 001 — Creación de usuarios

**Estado**: pendiente de aprobación.

Las decisiones marcadas como **[Propuesta]** requieren aprobación.

## 1. Objetivo

Permitir que un administrador cree usuarios con nombre, apellido, correo, contraseña y rol.

## 2. Alcance

Incluye:

- Modelo de usuario y roles `USER` y `ADMIN`.
- Persistencia en PostgreSQL mediante Flyway.
- Contraseñas almacenadas con BCrypt.
- Endpoint protegido para crear usuarios.
- Validación de tokens JWT.
- Usuario administrador inicial.
- Manejo de errores HTTP.
- Pruebas de dominio, web e integración.

No incluye:

- Login o emisión de tokens.
- Consulta, modificación, desactivación o eliminación de usuarios.
- Registro público.
- Recuperación o cambio de contraseña.
- Renovación o revocación de tokens.

Mientras no exista login, los tokens solo podrán generarse mediante herramientas externas o
utilidades de prueba compatibles con la configuración JWT.

## 3. Usuario

Cada usuario contiene:

| Campo | Restricciones |
| --- | --- |
| `id` | UUID generado por la aplicación |
| `email` | Obligatorio, máximo 255 caracteres y único tras normalizar |
| `passwordHash` | Hash BCrypt; nunca se expone |
| `firstName` | Obligatorio, máximo 100 caracteres |
| `lastName` | Obligatorio, máximo 100 caracteres |
| `role` | `USER` o `ADMIN` |
| `createdAt` | Instante de creación en UTC |

El correo se normaliza eliminando espacios exteriores y convirtiéndolo a minúsculas.

Nombre y apellido eliminan espacios exteriores. Los espacios interiores se conservan.

## 4. Persistencia

**[Propuesta]** Tabla `users`:

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| `id` | `uuid` | Clave primaria |
| `email` | `varchar(255)` | No nulo y único |
| `password_hash` | `varchar(255)` | No nulo |
| `first_name` | `varchar(100)` | No nulo |
| `last_name` | `varchar(100)` | No nulo |
| `role` | `varchar(20)` | No nulo; `USER` o `ADMIN` |
| `created_at` | `timestamptz` | No nulo |

Las migraciones crean la tabla y el usuario administrador inicial.

**[Propuesta]** Usuario inicial:

- Correo: `admin@meetroom.local`
- Contraseña: pendiente de definir.
- Nombre: `Admin`
- Apellido: `Sistema`
- Rol: `ADMIN`

La contraseña se almacena únicamente como hash BCrypt.

## 5. Endpoint

```http
POST /v1/users
Authorization: Bearer <token>
Content-Type: application/json
```

Requiere un token válido con rol `ADMIN`.

### Petición

```json
{
  "email": "ana.lopez@empresa.com",
  "password": "clave-segura",
  "firstName": "Ana",
  "lastName": "López",
  "role": "USER"
}
```

Validaciones:

- Todos los campos son obligatorios.
- El correo debe tener formato válido.
- La contraseña debe tener entre **[Pendiente]** y 72 bytes en UTF-8.
- Nombre y apellido no pueden quedar vacíos después de recortarlos.
- El rol distingue mayúsculas y debe ser `USER` o `ADMIN`.
- Los campos JSON desconocidos **[Propuesta]** se rechazan.

### Respuesta satisfactoria

```http
201 Created
Location: /v1/users/{id}
```

```json
{
  "id": "3f2b9c7a-5d41-4e8b-9a0c-7e1d2f6b8a34",
  "email": "ana.lopez@empresa.com",
  "firstName": "Ana",
  "lastName": "López",
  "role": "USER",
  "createdAt": "2026-11-16T11:34:56.789Z"
}
```

La contraseña y el hash nunca se incluyen en la respuesta.

### Errores

| Estado | Tipo | Motivo |
| --- | --- | --- |
| `400` | `/problems/validation-error` | Uno o más campos no cumplen sus reglas |
| `400` | `/problems/malformed-json` | El cuerpo no contiene JSON válido |
| `400` | `/problems/unknown-field` | El cuerpo contiene un campo no admitido |
| `401` | `/problems/authentication-required` | Falta el token o no es válido |
| `403` | `/problems/insufficient-permissions` | El usuario no tiene rol `ADMIN` |
| `409` | `/problems/user-email-conflict` | Ya existe el correo normalizado |

Los errores usan `application/problem+json`. `type` identifica el problema concreto y `detail`
explica la causa específica de esa respuesta.

Ejemplo de validación:

```json
{
  "type": "/problems/validation-error",
  "title": "Datos de usuario inválidos",
  "status": 400,
  "detail": "La petición contiene 2 campos inválidos",
  "instance": "/v1/users",
  "errors": [
    {
      "field": "email",
      "message": "El correo debe tener un formato válido"
    },
    {
      "field": "firstName",
      "message": "El nombre es obligatorio"
    }
  ]
}
```

Cada campo inválido genera un elemento en `errors` con un mensaje que indica exactamente la regla
incumplida. Los errores ajenos a campos concretos no incluyen `errors`, pero mantienen un `type`,
un `title` y un `detail` específicos.

## 6. Seguridad

- JWT firmado con HS256.
- La clave se obtiene de configuración externa.
- El rol se obtiene del claim `role`.
- El claim debe contener exactamente `USER` o `ADMIN`.
- Un token sin un rol válido no autoriza la operación.
- La API no utiliza sesiones.
- La contraseña nunca se registra ni se devuelve.

## 7. Reglas

- La unicidad del correo se garantiza en el dominio y en la base de datos.
- Dos creaciones simultáneas con el mismo correo producen un `201` y un `409`.
- El identificador se genera en la aplicación.
- `createdAt` se genera y devuelve en UTC.
- El dominio no depende de Spring ni de JPA.

## 8. Criterios de aceptación

- Un administrador puede crear un usuario y recibe `201`, `Location` y el cuerpo definido.
- El correo queda almacenado normalizado.
- La contraseña queda almacenada como hash BCrypt.
- Un correo duplicado devuelve `409` con el tipo `/problems/user-email-conflict`.
- Cada error devuelve un tipo y un detalle que describen su causa concreta.
- Una petición con campos inválidos devuelve `400` y un mensaje por cada campo afectado.
- La ausencia de token devuelve `401`.
- Un usuario sin rol `ADMIN` recibe `403`.
- Las migraciones funcionan sobre una base de datos vacía.
- La contraseña y su hash nunca aparecen en respuestas o registros.

## 9. Pruebas

Se comprobarán:

- Normalización y validaciones del dominio.
- Hasheado de la contraseña.
- Creación satisfactoria.
- Correo duplicado, incluida concurrencia.
- Tipos, detalles y mensajes específicos de cada error.
- Respuestas `400`, `401`, `403` y `409`.
- Aplicación de migraciones con PostgreSQL.

## 10. Decisiones pendientes

- Longitud mínima de la contraseña.
- Contraseña o mecanismo de configuración del administrador inicial.
- Confirmar si los campos JSON desconocidos se rechazan.
