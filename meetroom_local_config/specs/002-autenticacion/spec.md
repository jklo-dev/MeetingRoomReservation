# 002 — Autenticación

**Estado**: implementada.

## 1. Objetivo

Permitir que un usuario registrado obtenga un token JWT mediante su correo y contraseña.

Depende de `001-creacion-usuarios`, que proporciona usuarios, contraseñas BCrypt, validación JWT
y manejo de errores.

## 2. Alcance

Incluye:

- Autenticación por correo y contraseña.
- Verificación de contraseñas BCrypt.
- Emisión de tokens JWT con HS256.
- Endpoint público de login.
- Protección contra enumeración de usuarios mediante verificación BCrypt señuelo.
- Ampliación de la configuración de seguridad.
- Pruebas de caso de uso, web e integración.

No incluye:

- Renovación o revocación de tokens.
- Cierre de sesión.
- Consulta del usuario autenticado.
- Cambio o recuperación de contraseña.
- Bloqueo de cuentas o limitación de intentos.
- Autenticación externa o segundo factor.

No requiere dependencias nuevas.

## 3. Componentes

La funcionalidad amplía:

- `UserRepositoryPort` para buscar por correo normalizado.
- `PasswordHasher` para verificar una contraseña contra un hash.

Añade:

- Un caso de uso de autenticación.
- Un puerto para emitir tokens.
- Un error de credenciales inválidas que no distingue la causa.
- Un adaptador JWT basado en la infraestructura disponible.
- Un reloj inyectable para calcular y comprobar la caducidad.

El dominio no depende de Spring ni de la implementación JWT.

## 4. Token

- Algoritmo: HS256.
- Clave: configuración externa compartida con la validación JWT.
- Vigencia: 24 horas.

Claims:

| Claim | Contenido |
| --- | --- |
| `sub` | UUID del usuario |
| `role` | `USER` o `ADMIN` |
| `iat` | Instante de emisión |
| `exp` | Instante de caducidad |
| `iss` | `meetroom` |

No se incluyen `aud` ni `jti`.

El rol refleja el valor existente al emitir el token. Un cambio posterior no afecta a los tokens
ya emitidos.

## 5. Endpoint

```http
POST /v1/users/login
Content-Type: application/json
```

Es público y no requiere token. Una cabecera `Authorization` con un token válido no impide el
login; un bearer inválido puede ser rechazado por el filtro de autenticación antes de llegar al
endpoint.

### Petición

```json
{
  "email": "admin@meetroom.local",
  "password": "admin"
}
```

| Campo | Reglas |
| --- | --- |
| `email` | Obligatorio y no vacío; se recorta y convierte a minúsculas |
| `password` | Obligatorio y no vacío |

No se valida el formato del correo ni la longitud de la contraseña. Una credencial
con formato desconocido se procesa como credencial inválida y devuelve `401`.

### Respuesta satisfactoria

```http
200 OK
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "3f2b9c7a-5d41-4e8b-9a0c-7e1d2f6b8a34",
    "email": "admin@meetroom.local",
    "firstName": "Admin",
    "lastName": "Sistema",
    "role": "ADMIN"
  }
}
```

- `tokenType` siempre vale `Bearer`.
- `expiresIn` se expresa en segundos.
- `user` nunca contiene la contraseña ni su hash.

### Errores

| Estado | Tipo | Motivo |
| --- | --- | --- |
| `400` | `urn:meetroom:problem:validation-error` | Falta un campo o está vacío |
| `400` | `urn:meetroom:problem:malformed-request` | El cuerpo no se puede interpretar |
| `401` | `urn:meetroom:problem:invalid-credentials` | Correo inexistente o contraseña incorrecta |

Ejemplo de validación:

```json
{
  "type": "urn:meetroom:problem:validation-error",
  "title": "Bad Request",
  "status": 400,
  "detail": "La petición contiene campos inválidos",
  "instance": "/v1/users/login",
  "errors": [
    {
      "field": "password",
      "message": "es obligatoria"
    }
  ]
}
```

Ejemplo de credenciales inválidas:

```json
{
  "type": "urn:meetroom:problem:invalid-credentials",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Credenciales invalidas",
  "instance": "/v1/users/login"
}
```

La respuesta no indica si el correo existe ni qué credencial ha fallado.

## 6. Reglas de autenticación

- El correo usa la misma normalización que la creación de usuarios.
- La contraseña se verifica mediante BCrypt.
- Si el correo no existe, se ejecuta una verificación BCrypt contra un hash señuelo constante.
- Correo inexistente y contraseña incorrecta devuelven el mismo estado y cuerpo.
- El token se firma con la misma clave utilizada para validarlo.
- Las representaciones textuales del request y del comando omiten la contraseña. El logging HTTP
  del cuerpo debe permanecer deshabilitado para garantizar que no se registre externamente.
- Un token caducado no permite acceder a endpoints protegidos.

## 7. Configuración de seguridad

- `POST /v1/users/login` es público.
- `POST /v1/users` continúa requiriendo rol `ADMIN`.
- Las demás reglas de acceso no cambian.

## 8. Casos relevantes

| Caso | Resultado |
| --- | --- |
| Correo con mayúsculas y espacios exteriores | Se normaliza antes de buscar |
| Correo inexistente | `401`, después de verificar el hash señuelo |
| Contraseña incorrecta | `401`, con la misma respuesta |
| Correo o contraseña vacíos | `400` |
| Campo obligatorio ausente | `400` |
| Correo con formato inválido | `401` |
| JSON mal formado | `400` |
| Login con un token válido en `Authorization` | `200` si las credenciales son válidas |
| Login con un bearer inválido en `Authorization` | Puede devolver `401` desde el filtro de seguridad |
| Token de administrador usado en `POST /v1/users` | Operación autorizada |
| Token de usuario usado en `POST /v1/users` | `403` |
| Token caducado | `401` |

## 9. Criterios de aceptación

- Las credenciales válidas devuelven `200` y el cuerpo definido.
- El token está firmado con HS256.
- `sub` contiene el UUID y `role` el rol del usuario.
- `exp` se encuentra 24 horas después de `iat`.
- `expiresIn` vale `86400`.
- El token de un administrador autoriza la creación de usuarios.
- El token de un usuario normal no autoriza la creación de usuarios.
- Correo inexistente y contraseña incorrecta producen la misma respuesta.
- Los campos ausentes o vacíos producen errores descriptivos.
- El correo se normaliza antes de buscar.
- El login funciona sin token.
- El dominio no depende de Spring ni de la implementación JWT.

## 10. Pruebas

Se comprobarán:

- Autenticación correcta.
- Contraseña incorrecta.
- Correo inexistente y ejecución de la verificación señuelo.
- Claims y caducidad con un reloj controlado.
- Respuestas `200`, `400` y `401`.
- Igualdad de las respuestas para credenciales incorrectas.
- Login del administrador inicial.
- Uso del token para crear un usuario.
- Login del usuario recién creado.

## 11. Limitaciones conocidas

- Los tokens duran 24 horas y no pueden renovarse ni revocarse.
- No existe limitación de intentos ni bloqueo de cuentas.
- Los cambios de rol no afectan a tokens ya emitidos.
- Se utiliza una única clave simétrica sin rotación.
- El decodificador valida firma y caducidad, pero no comprueba que `iss` sea `meetroom`.
- `issuer` y `ttl-seconds` no tienen validación explícita; una configuración vacía, cero o
  negativa puede impedir emitir tokens utilizables.
- La ausencia de logging del cuerpo depende también de mantener deshabilitado cualquier mecanismo
  externo de registro de peticiones HTTP.
