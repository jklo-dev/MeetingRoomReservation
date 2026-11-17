# Constitución de desarrollo — MeetingRoomReservation

Principios permanentes que definen cómo se trabaja en el proyecto.

El contexto técnico y las decisiones del proyecto están en
[`AGENTS.md`](AGENTS.md).

## 1. Especificación y autorización

Toda funcionalidad nueva debe definirse en una especificación antes de modificar código.

- La especificación requiere aprobación explícita.
- Su aprobación no autoriza la implementación.
- La implementación requiere una autorización posterior y explícita.
- Si aparece una necesidad no contemplada, se detiene el trabajo y se actualiza la especificación.

## 2. Alcance

Solo se realizan los cambios necesarios para cumplir lo aprobado.

- No se aplican refactorizaciones, limpiezas, renombrados ni mejoras adicionales sin autorización.
- Las mejoras detectadas se proponen antes de realizarlas.
- No se introducen abstracciones o configuraciones preventivas sin una necesidad aprobada.
- Las modificaciones se limitan a los archivos necesarios.

## 3. Decisiones funcionales

No se asumen reglas de negocio, validaciones, permisos ni contratos de API.

- Las ambigüedades relevantes se consultan.
- Las preguntas funcionales se hacen de una en una.
- No se pregunta por información verificable en el proyecto.
- Las decisiones no necesarias se mantienen pendientes.

## 4. Implementación

- El código debe ser claro, simple y coherente con la arquitectura y convenciones existentes.
- Las reglas de negocio pertenecen al dominio.
- Se evita duplicación, complejidad innecesaria y abstracción prematura.
- Los contratos existentes se preservan salvo cambio expresamente aprobado.
- Las migraciones aplicadas no se modifican.

## 5. Validación

- Cada cambio debe tener criterios de aceptación verificables.
- Las pruebas deben corresponder a la naturaleza del cambio.
- No se eliminan, ignoran ni debilitan pruebas para ocultar fallos.
- No se afirma que una comprobación pasó sin haberla ejecutado.
- Los fallos y las comprobaciones omitidas se informan claramente.
- No se ejecutan compilaciones, pruebas, migraciones ni servicios sin autorización previa.

## 6. Dependencias y seguridad

- No se añaden, actualizan o eliminan dependencias sin autorización explícita.
- Toda propuesta indica dependencia, versión, motivo, archivos afectados e impacto.
- No se instalan herramientas sin autorización.
- La entrada externa se valida.
- No se almacenan ni exponen secretos, credenciales, tokens o detalles internos.
- No se concatena entrada externa en consultas SQL o JPQL.

## 7. Control de cambios y documentación

- No se ejecuta ningún comando Git ni se modifican sus metadatos.
- Cualquier desviación del alcance se explica antes de realizarla.
- Especificaciones, documentación, pruebas e implementación deben permanecer alineadas.
- Una propuesta no se presenta como comportamiento implementado.
- Cada información se documenta en un único lugar y se referencia desde los demás.
