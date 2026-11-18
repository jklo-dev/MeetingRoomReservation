# 004 — Listado de salas

**Estado**: implementada.

## 1. Objetivo

Permitir consultar salas mediante filtros, ordenación y paginación.

Depende de `003 — Creación de salas`, que define el modelo y la persistencia de salas.

## 2. Alcance

Incluye:

- Listado público de salas.
- Filtros por estado, capacidad mínima y nombre.
- Ordenación por nombre o capacidad.
- Paginación.
- Validación de parámetros.
- Pruebas de dominio, web y composición de filtros.

No incluye:

- Consulta individual.
- Modificación, desactivación, reactivación o eliminación.
- Consulta de disponibilidad.
- Búsqueda por relevancia.

No requiere dependencias nuevas.

## 3. Endpoint

```http
GET /v1/rooms
```

Es público y no requiere autenticación. Si se envía una cabecera `Authorization`, el filtro de
seguridad la evalúa: un token válido se acepta y uno inválido o caducado devuelve `401`.

### Parámetros

Todos son opcionales y acumulativos.

| Parámetro | Reglas | Valor predeterminado |
| --- | --- | --- |
| `minCapacity` | Entero entre 1 y 1000 | Sin filtro |
| `status` | `ACTIVE`, `INACTIVE` o `ALL` | `ACTIVE` |
| `name` | Búsqueda parcial sin distinguir mayúsculas | Sin filtro |
| `sortBy` | `name` o `capacity` | `name` |
| `direction` | `asc` o `desc` | `asc` |
| `page` | Entero mayor o igual a 0 | `0` |
| `size` | Entero mayor o igual a 1, sin máximo | `20` |

`status`, `sortBy` y `direction` no distinguen mayúsculas.

Ejemplo:

```http
GET /v1/rooms?minCapacity=8&name=azul&status=ALL&sortBy=capacity&direction=desc&page=0&size=10
```

## 4. Respuesta

Una petición válida devuelve `200`, aunque no existan resultados.

```json
{
  "content": [
    {
      "id": "8c1f4e2a-7b93-4d16-a5e8-0f3c9d2b6514",
      "name": "Sala Azul",
      "capacity": 12,
      "status": "ACTIVE",
      "createdAt": "2026-11-16T11:34:56.789Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

| Campo | Descripción |
| --- | --- |
| `content` | Salas de la página solicitada |
| `page` | Índice de página, empezando en 0 |
| `size` | Tamaño aplicado |
| `totalElements` | Total de coincidencias |
| `totalPages` | Total de páginas |
| `first` | Indica si es la primera página |
| `last` | Indica si es la última página |

Los elementos de `content` mantienen el contrato definido en `003 — Creación de salas`.

Sin resultados:

- `content` es un array vacío.
- `totalElements` y `totalPages` valen `0`.
- `first` y `last` valen `true`.

## 5. Errores

Los parámetros inválidos devuelven `400` con `application/problem+json`.

| Tipo | Motivo |
| --- | --- |
| `urn:meetroom:problem:validation-error` | Un parámetro no cumple sus reglas |

```json
{
  "type": "urn:meetroom:problem:validation-error",
  "title": "Bad Request",
  "status": 400,
  "detail": "La petición contiene parametros invalidos",
  "instance": "/v1/rooms",
  "errors": [
    {
      "field": "sortBy",
      "message": "debe ser uno de: name, capacity"
    }
  ]
}
```

Cada parámetro inválido genera un elemento descriptivo en `errors`, excepto `minCapacity` fuera
del rango 1–1000, que actualmente devuelve el mismo tipo sin ese array.

El endpoint no devuelve `403`. Devuelve `401` si se envía un bearer inválido o caducado.

## 6. Reglas

- Sin parámetros se devuelven salas `ACTIVE`, ordenadas por nombre ascendente.
- `minCapacity` incluye salas con capacidad igual o superior.
- `status=ALL` desactiva el filtro de estado.
- `name` busca por contenido y no distingue mayúsculas.
- El valor de `name` se recorta; si queda vacío, el filtro se omite.
- `%` y `_` se tratan como texto, no como comodines.
- El orden por nombre usa su representación en minúsculas.
- Todo orden incluye `id` como desempate para estabilizar la paginación.
- Una página posterior a la última devuelve `200` con `content` vacío.
- El endpoint no requiere token, pero el filtro de seguridad procesa cualquier bearer enviado.

## 7. Casos relevantes

| Caso | Resultado |
| --- | --- |
| Sin parámetros | Salas activas, nombre ascendente, página 0 y tamaño 20 |
| `status=all` | Se acepta |
| Estado desconocido | `400` |
| `minCapacity=0`, `1001` o texto | `400` |
| `name` vacío después de recortarlo | Se omite el filtro |
| `name=%` | Busca el carácter `%` literalmente |
| Campo de ordenación desconocido | `400` |
| Página negativa | `400` |
| Tamaño `0` | `400` |
| Tamaño muy grande | Se acepta |
| Página fuera de rango | `200` con contenido vacío |
| Inventario vacío | `200` con paginación vacía |
| Token válido | Se acepta |
| Token inválido o caducado | `401` |

## 8. Diseño

La paginación no expone tipos de Spring al dominio.

- El caso de uso recibe filtros, orden y paginación mediante tipos propios.
- El dominio utiliza un resultado paginado independiente de Spring Data.
- La infraestructura traduce entre esos tipos y Spring Data.
- La respuesta HTTP utiliza un DTO paginado.
- Los filtros opcionales se implementan mediante `Specification`.

Los tipos de paginación podrán reutilizarse en la futura funcionalidad de listado de reservas.

## 9. Criterios de aceptación

- `GET /v1/rooms` funciona sin token.
- Un token válido se acepta y uno inválido o caducado devuelve `401`.
- Sin parámetros devuelve únicamente salas activas.
- Los filtros por estado, capacidad y nombre funcionan de forma individual y combinada.
- La búsqueda por nombre no distingue mayúsculas.
- La ordenación funciona en ambos sentidos.
- La paginación es estable cuando varios registros tienen el mismo valor de ordenación.
- Los parámetros inválidos devuelven errores descriptivos.
- Una página fuera de rango devuelve contenido vacío.
- El dominio no depende de Spring Data.

## 10. Pruebas

Se comprobarán:

- Valores predeterminados.
- Filtros individuales y combinados.
- Búsqueda por nombre sin distinguir mayúsculas.
- Tratamiento literal de `%` y `_`.
- Ordenación por nombre y capacidad.
- Estabilidad de paginación.
- Página vacía y página fuera de rango.
- Parámetros inválidos.
- Acceso público con token ausente, válido o caducado.

No existe una prueba de integración del listado contra PostgreSQL. Los filtros, la ordenación y
la paginación JPA no se comprueban actualmente sobre datos reales.

## 11. Limitaciones conocidas

- El tamaño de página no tiene máximo.
- La búsqueda parcial puede requerir un recorrido completo.
- La búsqueda no ignora acentos.
- El total de elementos requiere una consulta de recuento.
- `minCapacity` fuera de rango devuelve `validation-error` sin el array `errors`.
- El filtro `name` no aplica un límite máximo de longitud.
- La ordenación mediante `lower(name)` depende de la collation configurada en PostgreSQL.
- `totalPages` se convierte a `int` y puede desbordarse con cantidades extremas de resultados.
