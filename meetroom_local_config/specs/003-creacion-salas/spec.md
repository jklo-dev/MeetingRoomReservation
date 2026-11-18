# 003 — Creación de salas

**Estado**: implementada.

## 1. Objetivo

Permitir que un administrador cree salas de reuniones con nombre y capacidad.

Depende de las funcionalidades `001 — Creación de usuarios` y `002 — Autenticación`.

## 2. Alcance

Incluye:

- Modelo de sala.
- Persistencia mediante Flyway.
- Validación de nombre y capacidad.
- Unicidad del nombre sin distinguir mayúsculas.
- Endpoint protegido para crear salas.
- Pruebas de dominio, web e integración.

No incluye:

- Listado o consulta individual.
- Modificación, desactivación, reactivación o eliminación.
- Ubicación, planta, equipamiento u otros atributos descriptivos.
- Reservas.

No requiere dependencias nuevas.

## 3. Sala

Cada sala contiene:

| Campo | Restricciones |
| --- | --- |
| `id` | UUID generado por la aplicación |
| `name` | Obligatorio, máximo 100 caracteres y único sin distinguir mayúsculas |
| `capacity` | Entero entre 1 y 1000 |
| `status` | `ACTIVE` o `INACTIVE` |
| `createdAt` | Instante de creación con offset |

El nombre elimina los espacios exteriores, pero conserva su capitalización y espacios interiores.

Toda sala se crea con estado `ACTIVE`.

## 4. Persistencia

Tabla `rooms`:

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| `id` | `uuid` | Clave primaria |
| `name` | `varchar(100)` | No nulo |
| `capacity` | `integer` | No nulo; entre 1 y 1000 |
| `status` | `varchar(20)` | No nulo; `ACTIVE` o `INACTIVE` |
| `created_at` | `timestamptz` | No nulo |

La unicidad se garantiza mediante un índice único sobre `lower(name)`. Esto impide nombres
duplicados con distinta capitalización y conserva el nombre original.

La migración crea la tabla, sus restricciones y el índice único.

## 5. Endpoint

```http
POST /v1/rooms
Authorization: Bearer <token>
Content-Type: application/json
```

Requiere un token válido con rol `ADMIN`.

### Petición

```json
{
  "name": "Sala Azul",
  "capacity": 12
}
```

Validaciones:

- `name` es obligatorio.
- El nombre no puede quedar vacío después de recortarlo.
- El nombre admite como máximo 100 caracteres.
- `capacity` es obligatoria.
- La capacidad debe ser un entero entre 1 y 1000.
- El estado no se acepta en la petición.
- Los campos desconocidos se ignoran.

### Respuesta satisfactoria

```http
201 Created
Location: /v1/rooms/{id}
```

```json
{
  "id": "8c1f4e2a-7b93-4d16-a5e8-0f3c9d2b6514",
  "name": "Sala Azul",
  "capacity": 12,
  "status": "ACTIVE",
  "createdAt": "2026-11-16T11:34:56.789Z"
}
```

### Errores

| Estado | Tipo | Motivo |
| --- | --- | --- |
| `400` | `urn:meetroom:problem:validation-error` | Uno o más campos son inválidos |
| `400` | `urn:meetroom:problem:malformed-request` | El cuerpo no se puede interpretar |
| `401` | `urn:meetroom:problem:authentication-required` | Falta el token o no es válido |
| `403` | `urn:meetroom:problem:insufficient-role` | El usuario no tiene rol `ADMIN` |
| `409` | `urn:meetroom:problem:room-name-already-exists` | Ya existe una sala con ese nombre |

Los errores utilizan `application/problem+json`, con un `type` y un `detail` que describen la
causa concreta.

Ejemplo de validación:

```json
{
  "type": "urn:meetroom:problem:validation-error",
  "title": "Bad Request",
  "status": 400,
  "detail": "La petición contiene campos inválidos",
  "instance": "/v1/rooms",
  "errors": [
    {
      "field": "capacity",
      "message": "debe estar entre 1 y 1000"
    }
  ]
}
```

Ejemplo de conflicto:

```json
{
  "type": "urn:meetroom:problem:room-name-already-exists",
  "title": "Conflict",
  "status": 409,
  "detail": "Ya existe una sala con el nombre indicado",
  "instance": "/v1/rooms"
}
```

## 6. Reglas

- La capa web valida la longitud antes del recorte; el dominio lo recorta antes de compararlo y
  almacenarlo.
- La comparación de nombres no distingue mayúsculas.
- El nombre almacenado conserva su capitalización original.
- La unicidad se comprueba en la aplicación y en la base de datos.
- Una colisión concurrente se traduce a `409`.
- La capacidad debe ser un entero entre 1 y 1000.
- Toda sala nace con estado `ACTIVE`.
- El identificador se genera en la aplicación.
- Solo un administrador puede crear salas.
- El dominio no depende de Spring ni de JPA.

## 7. Casos relevantes

| Caso | Resultado |
| --- | --- |
| Nombre con espacios exteriores | Se recorta |
| Nombre vacío o compuesto solo por espacios | `400` |
| Nombre superior a 100 caracteres | `400` |
| Nombre existente con distinta capitalización | `409` |
| Capacidad `0` o `1001` | `400` |
| Capacidad `1` o `1000` | `201` |
| Capacidad textual | `400` |
| Capacidad decimal | `400` |
| Campo `status` o cualquier otro campo desconocido | Se ignora |
| JSON vacío o mal formado | `400` |
| Usuario autenticado sin rol `ADMIN` | `403` |
| Petición sin token | `401` |
| Dos peticiones simultáneas con el mismo nombre | Una devuelve `201` y la otra `409` |
| `Sala  Azul` frente a `Sala Azul` | Se consideran nombres diferentes |

## 8. Criterios de aceptación

- Un administrador puede crear una sala y recibe `201`, `Location` y el cuerpo definido.
- La sala se almacena sin espacios exteriores y con su capitalización original.
- La sala se crea con estado `ACTIVE`.
- Los nombres duplicados sin distinguir mayúsculas devuelven `409`.
- La base de datos también impide nombres duplicados.
- Las capacidades inválidas devuelven `400` con un mensaje descriptivo.
- Las capacidades `1` y `1000` son válidas.
- La ausencia de token devuelve `401`.
- Un usuario sin rol `ADMIN` recibe `403`.
- La migración funciona sobre una base de datos vacía.

## 9. Pruebas

Se comprobarán:

- Validación y recorte del nombre.
- Validación de capacidad y valores límite.
- Creación satisfactoria.
- Nombre duplicado con distinta capitalización.
- Respuestas `400`, `401`, `403` y `409`.
- Aplicación de la migración y funcionamiento del índice único.

## 10. Limitaciones conocidas

- Los espacios interiores múltiples no se normalizan.
- No pueden modificarse los datos de una sala.
- No existen atributos de ubicación o equipamiento.
- El nombre se valida antes de recortar espacios; un valor que exceda 100 caracteres solo por
  espacios exteriores puede rechazarse.
- Cualquier violación de integridad durante el guardado se traduce actualmente como conflicto de
  nombre duplicado.
- No existe una prueba realmente concurrente de la colisión de nombres.

## 11. Decisiones pendientes

No quedan decisiones pendientes para esta funcionalidad.
