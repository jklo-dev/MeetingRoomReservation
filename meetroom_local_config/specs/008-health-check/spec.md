# 008 — Endpoint de salud

**Estado**: pendiente de aprobación.

---

## 1. Objetivo

Exponer un endpoint que permita saber si la aplicación está viva y si puede acceder a la base de
datos, para balanceadores, orquestadores y monitores de uptime.

## 2. Contexto

La cadena de seguridad ya permite `/actuator/health` desde la funcionalidad 001, pero **el
endpoint no existe**: la dependencia de Actuator nunca se añadió, así que ese *matcher* no
coincide con nada y la ruta devuelve 403 como cualquier otra no mapeada. Esta funcionalidad cierra
ese cabo suelto.

## 3. Alcance

### Incluido

- Dependencia de Actuator.
- Endpoint de salud público que comprueba la base de datos.
- Configuración que expone **solo** el endpoint de salud y oculta el detalle de componentes.

### Excluido

- Métricas, `info`, `env`, `loggers`, volcados de hilos y el resto de endpoints de Actuator.
- Endpoint propio bajo `/v1`: se descartó por reimplementar algo que el framework ya ofrece.
- Comprobaciones de dependencias externas: no hay ninguna más que la base de datos.
- Separación entre *liveness* y *readiness*: solo tiene sentido con un orquestador que las
  distinga.

## 4. Decisión: Actuator en lugar de endpoint propio

**[Decidido]** Se usa el endpoint de Actuator.

Motivo: comprueba el `DataSource` de serie, devuelve exactamente los códigos acordados y no
requiere escribir controlador, lógica de comprobación ni tests. Reimplementar a mano algo que el
framework provee añadiría código que mantener sin ninguna ganancia.

Contrapartida aceptada: el cuerpo sigue el formato de Spring (`{"status":"UP"}`) y no el estilo del
resto de la API, y la ruta cuelga de `/actuator` y no de `/v1`. Es coherente con la distinción de
fondo: **Actuator sirve para operar la aplicación, la API `/v1` es el contrato de negocio**. Los
consumidores son distintos —un balanceador frente a una aplicación cliente— y por eso viven en
rutas separadas.

## 5. Contrato

### Ruta

```
GET /actuator/health
```

### Autenticación

**No requiere autenticación.** Un monitor o un balanceador no pueden autenticarse.

### Respuestas

**200 OK** — la aplicación está viva y accede a la base de datos.

```json
{
  "status": "UP"
}
```

**503 Service Unavailable** — la aplicación está viva pero **no** accede a la base de datos.

```json
{
  "status": "DOWN"
}
```

**Sin respuesta** — si el proceso no está vivo, el cliente recibe un error de conexión. No hay
código HTTP que devolver.

### Sobre el 503

Se eligió 503 y no 400. El 400 afirma que la petición estaba mal formada, y aquí el cliente no
hizo nada incorrecto. La diferencia es práctica: balanceadores, Kubernetes y los monitores de
uptime tratan 5xx como «no sano» y sacan la instancia de rotación, mientras que un 4xx lo leen
como error del cliente y pueden seguir enviándole tráfico.

### Sobre el cuerpo sin detalle

**[Decidido]** El cuerpo muestra solo el estado global, que es el comportamiento por defecto de
Actuator.

El detalle por componente revelaría el motor de base de datos, su versión y el estado de las
migraciones de Flyway a cualquiera que pregunte, sin autenticación. Un balanceador solo mira el
código HTTP, así que no pierde nada.

## 6. Configuración

**[Propuesta]** En `application.yaml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
```

`include: health` restringe la exposición **solo** al endpoint de salud. Por defecto Actuator ya
expone únicamente `health` e `info` por HTTP, pero declararlo de forma explícita evita que una
versión futura o una dependencia añadida expongan algo más sin que nadie lo note.

`show-details: never` es ya el valor por defecto; se declara por lo mismo.

No hace falta tocar `SecurityConfig`: la regla `permitAll` para `/actuator/health` existe desde la
001.

## 7. Dependencia a autorizar

| Artefacto | Versión | Configuración | Motivo |
| --- | --- | --- | --- |
| `org.springframework.boot:spring-boot-starter-actuator` | La del BOM 4.1.2 | `implementation` | Aporta el endpoint de salud con comprobación automática del `DataSource` |

Impacto: añade el módulo de Actuator al artefacto final. La superficie expuesta queda limitada por
la configuración de la sección 6.

## 8. Casos límite

| Caso | Resultado |
| --- | --- |
| Aplicación viva, base de datos accesible | 200 con `status: UP` |
| Aplicación viva, base de datos caída o inalcanzable | 503 con `status: DOWN` |
| Aplicación no arrancada | Error de conexión, sin respuesta HTTP |
| Petición sin token | 200 o 503: el endpoint es público |
| Petición con token válido | Lo mismo: el token se ignora |
| Petición con token caducado | **[Propuesta]** 401. Mismo comportamiento que los demás endpoints públicos: `BearerTokenAuthenticationFilter` se ejecuta antes de la comprobación de autorización, así que una cabecera `Authorization` defectuosa falla antes de llegar al `permitAll` |
| `GET /actuator/env` u otro endpoint de Actuator | 403: la configuración no los expone y la cadena los deniega |

## 9. Criterios de aceptación

1. `GET /actuator/health` sin token devuelve 200 con `{"status":"UP"}` cuando la base de datos es
   accesible.
2. Con la base de datos caída devuelve 503 con `{"status":"DOWN"}`.
3. El cuerpo **no** incluye detalle por componente en ninguno de los dos casos.
4. `GET /actuator/env` y `GET /actuator/metrics` devuelven 403.
5. La aplicación arranca sin cambios en `SecurityConfig`.

## 10. Pruebas

| Nivel | Qué cubre |
| --- | --- |
| Web con MockMvc | 200 con `status: UP` y sin detalle; que `/actuator/env` devuelve 403 |
| Integración con Testcontainers | 200 real contra PostgreSQL vivo |

**[Propuesta]** El caso del 503 **no se prueba automáticamente**: provocarlo exige parar la base de
datos a mitad del test o sustituir el `DataSource` por uno roto, y ninguna de las dos cosas encaja
bien en esta batería. Es comprobable a mano parando el contenedor, y queda recogido como
limitación en lugar de fingir que está cubierto.

## 11. Deuda técnica que genera

- **El 503 no está cubierto por tests**, por el motivo de la sección anterior.
- **Sin distinción entre *liveness* y *readiness***. Un orquestador las trata distinto: la primera
  decide si reiniciar el contenedor, la segunda si enviarle tráfico. Hoy una base de datos caída
  marcaría la instancia como no sana, lo que en Kubernetes podría provocar reinicios en bucle
  cuando el problema no está en la aplicación.
- **Sin autenticación en el endpoint.** Es intencionado, y por eso el cuerpo no lleva detalle.
- **Sin `info` ni métricas**, que serían los siguientes extras naturales.

## 12. Decisiones pendientes

- Validar las etiquetas `[Propuesta]`: la configuración exacta, el comportamiento con token
  caducado y la decisión de no probar el 503.
- Autorizar la dependencia de la sección 7.
- Al aprobar, actualizar `AGENTS.md`: la sección 12 lista Actuator entre los extras pendientes y
  pasaría a estar implementado.
