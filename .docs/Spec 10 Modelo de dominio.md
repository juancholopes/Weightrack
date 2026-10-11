# Spec 10: Modelo de dominio

Oct 10, 2026 · @Juan

| ID | Estado | Fase | Depende de |
| --- | --- | --- | --- |
| 10 | Implementada | 1 Dominio y datos | 01, 02 |

## Propósito

Esta spec diseña el modelo de dominio de Weightrack: las entidades, sus atributos, sus relaciones, sus invariantes y las preguntas que el dominio debe saber responder.

Diseña el dominio, no las tablas. Cómo se guarda cada entidad en Room 3, sus claves, índices y migraciones lo decide la Spec 20. Los casos de uso y las interfaces de repositorio los decide la Spec 30: se mantienen las interfaces de S20-R42 (ADR-30-05). El dominio vive en `com.juancholopes.weightrack.feature.catalog.domain` (`Exercise`, `ExerciseMuscle`, `Equipment`, `Muscle`) y `com.juancholopes.weightrack.feature.session.domain` (`WorkoutSession`, `ExerciseSet`), y se queda ahí (ADR-30-03). El reloj de S10-R32 lo expone el `AppContainer` (ADR-30-09). Esta spec solo fija qué preguntas deben poder responder esos repositorios.

Los nombres siguen la Spec 02: identificadores en inglés y términos en español en specs e interfaz. Las decisiones de esta spec las tomó el dueño el 2026-10-10.

## Principios del modelo

- **Dominio puro.** El modelo usa solo Kotlin, la biblioteca estándar del JDK, por ejemplo `java.time.LocalDate`, y `kotlinx.coroutines` (por ejemplo `Flow` en las interfaces de repositorio). No conoce Android, Room ni Compose (constitución, principio 4).
- **Sin textos de interfaz.** Los enums no guardan etiquetas en español. Cada valor tiene su etiqueta en `strings.xml` (constitución, convenciones de código). Las etiquetas se agregan con la primera feature que las muestra, F01, para no dejar textos sin usar.
- **Nada se resume.** Cada serie es un registro propio. "Última vez" y "Evolución" se calculan a partir de las series, no se guardan aparte (S01-R06).
- **Peso exacto.** El peso se representa como un entero de gramos para no perder precisión con números de coma flotante.

## Entidades

### Exercise (Ejercicio)

Un movimiento que el usuario entrena y del que registra el peso. Pertenece al catálogo. Una máquina no es una entidad aparte: es el equipo de un ejercicio.

| Atributo | Tipo | Unidad | Obligatorio | Notas |
| --- | --- | --- | --- | --- |
| `id` | `Long` | — | Sí | Se asigna al crear y no cambia. |
| `name` | `String` | — | Sí | Normalizado y único (S10-R04, S10-R05). |
| `equipment` | `Equipment` | — | Sí | Ver enum `Equipment`. |
| `notes` | `String?` | — | No | Nota fija del ejercicio, por ejemplo "asiento en 4". La implementa F01. |
| `archived` | `Boolean` | — | Sí | `false` al crear. Ver reglas de archivado. |

Los músculos de un ejercicio se describen con `ExerciseMuscle`.

**Objeto no guardado.** Decidido el 2026-10-10: un objeto que todavía no se guardó lleva `id = 0`, y sus hijos llevan `exerciseId = 0` o `sessionId = 0`. Al guardarlo, la Spec 20 ignora ese valor y usa el id que asigna la base de datos (S20-R17).

### ExerciseMuscle (Músculo del ejercicio)

Indica que un ejercicio trabaja un músculo y si ese músculo es principal o secundario.

| Atributo | Tipo | Unidad | Obligatorio | Notas |
| --- | --- | --- | --- | --- |
| `exerciseId` | `Long` | — | Sí | Referencia a `Exercise.id`. |
| `muscle` | `Muscle` | — | Sí | Ver enum `Muscle`. |
| `isPrimary` | `Boolean` | — | Sí | `true` para músculo principal, `false` para secundario. |

### WorkoutSession (Sesión)

Un entrenamiento concreto en un día, en el que el usuario registra series de uno o más ejercicios.

| Atributo | Tipo | Unidad | Obligatorio | Notas |
| --- | --- | --- | --- | --- |
| `id` | `Long` | — | Sí | Se asigna al crear, no cambia y crece en orden de creación (S10-R14). |
| `date` | `LocalDate` | Día del calendario | Sí | Solo el día, sin hora ni zona horaria. |
| `notes` | `String?` | — | No | Nota de la sesión, por ejemplo cómo se sintió el entrenamiento. La implementa F02. |

### ExerciseSet (Serie)

Una ejecución de un ejercicio dentro de una sesión, con un peso y un número de repeticiones.

| Atributo | Tipo | Unidad | Obligatorio | Notas |
| --- | --- | --- | --- | --- |
| `id` | `Long` | — | Sí | Se asigna al crear y no cambia. |
| `sessionId` | `Long` | — | Sí | Referencia a `WorkoutSession.id`. |
| `exerciseId` | `Long` | — | Sí | Referencia a `Exercise.id`. |
| `order` | `Int` | — | Sí | Posición de la serie dentro de la sesión (S10-R09). |
| `weightGrams` | `Int` | Gramos | Sí | Múltiplo de 250 (0,25 kg). La interfaz lo muestra en kilogramos. En ejercicios de peso corporal es el peso adicional (S10-R02). |
| `reps` | `Int` | Repeticiones | Sí | Entero mayor que 0. |

Ejemplo: 62,5 kg × 10 repeticiones se guarda como `weightGrams = 62500`, `reps = 10`, y se muestra como "62,5 kg".

## Enums

### Equipment (Equipo)

| Valor en código | Etiqueta en la interfaz |
| --- | --- |
| `MACHINE` | máquina |
| `CABLE` | polea |
| `DUMBBELL` | mancuerna |
| `BARBELL` | barra |
| `BODYWEIGHT` | peso corporal |

### Muscle (Músculo)

Lista fija de 14 grupos musculares medianos. El usuario no crea músculos nuevos.

| Valor en código | Etiqueta en la interfaz |
| --- | --- |
| `CHEST` | pecho |
| `UPPER_BACK` | espalda alta |
| `LATS` | dorsales |
| `LOWER_BACK` | espalda baja |
| `SHOULDERS` | hombros |
| `BICEPS` | bíceps |
| `TRICEPS` | tríceps |
| `FOREARMS` | antebrazos |
| `ABS` | abdomen |
| `QUADRICEPS` | cuádriceps |
| `HAMSTRINGS` | isquiotibiales |
| `GLUTES` | glúteos |
| `CALVES` | pantorrillas |
| `ADDUCTORS_ABDUCTORS` | aductores/abductores |

Agregar, quitar o renombrar un valor de un enum exige actualizar esta spec y revisar el impacto en la Spec 20, porque los valores quedan guardados.

## Relaciones y cardinalidad

| Relación | Cardinalidad | Notas |
| --- | --- | --- |
| `WorkoutSession` → `ExerciseSet` | 1 a N (N ≥ 0) | Una sesión recién creada puede no tener series. |
| `ExerciseSet` → `WorkoutSession` | N a 1 | Toda serie pertenece a una sola sesión. |
| `ExerciseSet` → `Exercise` | N a 1 | Toda serie es de un solo ejercicio. |
| `Exercise` → `ExerciseSet` | 1 a N (N ≥ 0) | Un ejercicio puede no tener series todavía. |
| `Exercise` ↔ `Muscle` | N a M, vía `ExerciseMuscle` | Cada ejercicio tiene al menos un músculo principal. |
| `WorkoutSession` ↔ `Exercise` | N a M, vía `ExerciseSet` | Una sesión contiene uno o más ejercicios, y un ejercicio aparece en muchas sesiones. No hay una entidad aparte para "ejercicio dentro de la sesión". |

```
Exercise 1 ── N ExerciseMuscle (muscle, isPrimary)
Exercise 1 ── N ExerciseSet N ── 1 WorkoutSession
```

## Invariantes

Toda creación o edición respeta estas reglas. Una operación que las rompa se rechaza y no cambia nada.

| ID | Requisito |
| --- | --- |
| S10-R01 | El peso de una serie es mayor o igual que 0 y múltiplo de 0,25 kg: `weightGrams >= 0` y `weightGrams % 250 == 0`. |
| S10-R02 | En cualquier ejercicio, el peso es el peso externo usado. En un ejercicio con equipo `BODYWEIGHT` es el peso adicional (por ejemplo un disco colgado) y vale 0 cuando no hay peso adicional. No se suma el peso corporal. |
| S10-R03 | Las repeticiones son un entero mayor que 0. |
| S10-R04 | El nombre de un ejercicio se normaliza antes de validarlo y guardarlo: se quitan los espacios de los extremos y cada grupo de espacios internos se reduce a uno solo. El nombre normalizado no puede quedar vacío. |
| S10-R05 | El nombre normalizado es único entre todos los ejercicios, incluidos los archivados, sin distinguir mayúsculas de minúsculas (comparación con `lowercase(Locale.ROOT)`). Las tildes sí se distinguen. |
| S10-R06 | Al crear un ejercicio son obligatorios el nombre, el equipo y al menos un músculo. |
| S10-R07 | Todo ejercicio tiene al menos un músculo principal (`isPrimary = true`). Puede tener varios principales y cualquier número de secundarios. |
| S10-R08 | Un ejercicio no repite un músculo: cada `Muscle` aparece como máximo una vez en sus `ExerciseMuscle`, sea principal o secundario. |
| S10-R09 | El `order` de una serie es un entero mayor que 0 y único dentro de su sesión. Una serie nueva recibe el mayor `order` de la sesión más 1. No se exige que sea contiguo: borrar una serie no renumera las demás. |
| S10-R10 | Toda serie referencia una sesión existente y un ejercicio existente. |
| S10-R11 | No se pueden agregar series de un ejercicio archivado ni cambiar el ejercicio de una serie a uno archivado. Las series ya registradas de un ejercicio archivado se pueden corregir o eliminar. |
| S10-R12 | Las notas (`Exercise.notes` y `WorkoutSession.notes`) se guardan sin espacios en los extremos. Una nota vacía o solo con espacios se guarda como `null`. |
| S10-R13 | La fecha de una sesión es un día del calendario (`LocalDate`), obligatoria, sin hora. |
| S10-R14 | Los `id` se asignan al crear y no cambian. El `id` de `WorkoutSession` crece en orden de creación: una sesión creada después tiene un `id` mayor. La Spec 20 garantiza esta propiedad al persistir. |
| S10-R15 | Límites superiores: nombre de ejercicio de hasta 60 caracteres, notas de hasta 500 caracteres, repeticiones hasta 999 y peso hasta 1000 kg (`weightGrams <= 1_000_000`). |
| S10-R16 | El código del modelo de dominio no importa paquetes `android.*`, `androidx.*` (incluidos Room y Compose) ni librerías de terceros. Solo usa Kotlin, el JDK y `kotlinx.coroutines`, incluido `Flow` (enmienda del 2026-10-10, Spec 20). |

Dependencia de la Spec 20 sobre S10-R04 y S10-R05: la columna `normalized_name` de la tabla `exercise` se calcula con las funciones del dominio `normalizeExerciseName` y `nameKey` (S20-R12), no con una copia. Cambiar cualquiera de las dos cambia la clave de unicidad guardada y exige una migración en la Spec 20. `nameKey` sigue `internal` porque la Spec 30 mantiene un solo módulo (ADR-30-02). Separar módulos exigiría hacerlo público.

Por qué `order` no es contiguo: el orden solo sirve para mostrar las series en el orden en que se hicieron. Exigir que sea contiguo obligaría a reescribir las demás series cada vez que se borra una, sin ningún beneficio para las preguntas del dominio.

## Reglas de archivado y borrado

Archivar oculta un ejercicio del catálogo sin perder su historial. Cumple S01-R07.

| ID | Requisito |
| --- | --- |
| S10-R17 | Eliminar un ejercicio que tiene al menos una serie no lo borra: lo archiva (`archived = true`). Sus series, su nombre y sus músculos se conservan. |
| S10-R18 | Un ejercicio archivado no aparece en el catálogo activo ni en el selector de ejercicios al registrar una sesión, pero sus series siguen en el historial, en la última vez y en la evolución. |
| S10-R19 | Un ejercicio archivado se puede desarchivar (`archived = false`) y vuelve al catálogo con su historial. |
| S10-R20 | Eliminar un ejercicio sin series lo borra de forma definitiva, junto con sus `ExerciseMuscle`. |
| S10-R21 | Crear un ejercicio cuyo nombre normalizado coincide con el de un ejercicio archivado se rechaza por S10-R05. El dominio informa que existe un archivado con ese nombre, para que la interfaz ofrezca desarchivarlo. |
| S10-R22 | Renombrar un ejercicio o cambiar su equipo, sus músculos o su nota conserva su `id` y todas sus series. |
| S10-R23 | Eliminar una sesión elimina todas sus series. Los ejercicios no cambian, aunque queden sin series. La usa F04. |
| S10-R24 | Eliminar una serie no cambia la sesión ni el ejercicio. Una sesión puede quedar sin series. |

## Preguntas que el dominio debe responder

Las interfaces de repositorio y los casos de uso que las responden se diseñan en las Specs 20 y 30. Esta spec fija qué deben devolver.

Orden de las sesiones: una sesión es más reciente que otra si su fecha es posterior. Con la misma fecha, es más reciente la de mayor `id` (la creada después, S10-R14). Este desempate vale para todas las preguntas de abajo.

| ID | Requisito |
| --- | --- |
| S10-R25 | **Última vez.** Para un ejercicio, el dominio devuelve la sesión más reciente que contiene al menos una serie de ese ejercicio, con su fecha y todas las series de ese ejercicio en esa sesión, ordenadas por `order` ascendente. Si el ejercicio no tiene series, devuelve vacío. |
| S10-R26 | **Última vez al registrar.** Cuando se pide la última vez desde una sesión que se está registrando, se considera solo las sesiones anteriores a ella según el orden de sesiones, para no mostrar las series que se acaban de anotar. |
| S10-R27 | **Evolución.** Para un ejercicio, el dominio devuelve todas sus series con la fecha de su sesión, ordenadas por fecha ascendente, luego por `id` de sesión ascendente y luego por `order` ascendente. Es la base de las gráficas de progreso futuras. |
| S10-R32 | **Sin fechas futuras.** La fecha de una sesión no puede ser posterior a hoy. El dominio obtiene la fecha de hoy de un reloj inyectado para poder probarlo. |
| S10-R28 | **Historial.** El dominio devuelve la lista de sesiones ordenada de la más reciente a la más antigua: fecha descendente y, con la misma fecha, `id` descendente. |
| S10-R29 | **Series de una sesión.** Para una sesión, el dominio devuelve todas sus series ordenadas por `order` ascendente, cada una con su ejercicio, aunque esté archivado. |
| S10-R30 | **Catálogo activo.** El dominio devuelve los ejercicios no archivados ordenados por nombre sin distinguir mayúsculas. Se puede filtrar, de forma opcional, por un `Muscle` (principal o secundario) y por un `Equipment`. Con ambos filtros, se aplican los dos. |
| S10-R31 | **Archivados.** El dominio devuelve los ejercicios archivados ordenados por nombre, para poder desarchivarlos. |
| S10-R33 | **Búsqueda por nombre.** Un ejercicio coincide con un texto buscado si la clave de búsqueda del texto aparece dentro de la clave de búsqueda de su nombre. La clave de búsqueda se obtiene así: se normaliza como en S10-R04, se pasa a minúsculas con `lowercase(Locale.ROOT)`, se descompone en forma NFD (`java.text.Normalizer`) y se quitan las marcas diacríticas (categoría Unicode `Mn`). Así, "ñ" cuenta como "n" (decisión "Ñ en la búsqueda"). Un texto vacío o solo con espacios coincide con todos. Es distinta de la unicidad de S10-R05, que sí distingue tildes. La función es pura y vive en `feature/catalog/domain/`. Se implementa en F02, la primera spec que la usa (pestaña Ejercicios, Spec 42). |
| S10-R34 | **Peso más alto de cada sesión.** Para un ejercicio, el dominio devuelve, por cada sesión que tiene al menos una serie suya, la sesión (`id` y fecha) y el mayor `weightGrams` de sus series de ese ejercicio en esa sesión. Se calcula a partir de la evolución de S10-R27 y sigue su orden: fecha ascendente y luego `id` de sesión ascendente. En `BODYWEIGHT` es el mayor peso adicional y puede ser 0 (S10-R02). Sin series, devuelve vacío. Es una función pura sobre una lista de series; no guarda nada (S01-R06). Se implementa en F05. F02 usa el mismo concepto para el número de la tarjeta, sobre la sesión más reciente, con la lectura de la Spec 20. |
| S10-R35 | **Récord personal.** Una sesión es récord personal de un ejercicio si su peso más alto (S10-R34) es mayor que el peso más alto de todas las sesiones anteriores de ese ejercicio, según el orden de sesiones. La primera sesión no es récord. Un empate no es récord. El récord vigente en una sesión es el mayor peso más alto hasta ella, incluida. Es una función pura sobre el resultado de S10-R34. Se implementa en F05. |

*S10-R33 a S10-R35 se agregaron el 2026-10-11 por las Specs 40 y 42. Aplicado el 2026-10-11. No cambian el código ya implementado de esta spec; los implementan las specs F indicadas.*

## Cómo cumple S01-R06 a S01-R09

Lo pide S01-A05. La Spec 20 completa esta tabla para la persistencia.

| Requisito de la Spec 01 | Cómo lo cumple el dominio |
| --- | --- |
| S01-R06 | `ExerciseSet` es un registro propio con `exerciseId`, `weightGrams`, `reps` y `sessionId`, y la fecha sale de su `WorkoutSession`. No hay atributos de resumen ni promedios: la última vez y la evolución se calculan (S10-R25, S10-R27). |
| S01-R07 | Renombrar conserva `id` y series (S10-R22). Eliminar un ejercicio con historial lo archiva (S10-R17 a S10-R19), y solo se borra de forma definitiva uno sin series (S10-R20). |
| S01-R08 | El dominio no conoce el esquema (S10-R16), así que puede evolucionar sin ataduras a tablas. Todo cambio del dominio que afecte lo guardado, incluidos los valores de los enums, exige una migración versionada en la Spec 20. |
| S01-R09 | Cada entidad tiene una feature dueña: `Exercise` y `ExerciseMuscle` son de F01; `WorkoutSession` y `ExerciseSet` son de F02. F03 y F04 leen esos datos, y F04 corrige y elimina sesiones y series con las mismas reglas de F02. El único contrato entre F01 y F02 es que `ExerciseSet` referencia a `Exercise` por `id` y respeta S10-R11. Las features futuras agregan sus propias entidades sin modificar estas. |

## Fuera de alcance de esta spec

- Tablas, claves, índices, DAOs y migraciones: Spec 20.
- Casos de uso, interfaces de repositorio, inyección de dependencias y cambios de paquete: Spec 30. El dominio se queda en su paquete (ADR-30-03), las interfaces de S20-R42 se mantienen (ADR-30-05), el reloj de S10-R32 viene del `AppContainer` (ADR-30-09) y `nameKey` sigue `internal` porque hay un solo módulo (ADR-30-02).
- Pantallas, textos y flujos: Specs 40 a 42 y specs F.
- Rutinas y mediciones corporales: visión a futuro de la Spec 01. Las gráficas de progreso pasan a F05 (Spec 01, 2026-10-11) y usan la evolución de S10-R27.

## Criterios de aceptación

Salvo que se indique otra cosa, la evidencia es una prueba unitaria del dominio que corre con `./gradlew test`, sin emulador.

| ID | Criterio | Comprueba | Evidencia |
| --- | --- | --- | --- |
| S10-A01 | Una serie con peso negativo o no múltiplo de 250 g se rechaza. Una con 0 g, 250 g y 62 500 g se acepta. | S10-R01 | Prueba unitaria |
| S10-A02 | Una serie de un ejercicio `BODYWEIGHT` con 0 g se acepta y representa "sin peso adicional". | S10-R02 | Prueba unitaria |
| S10-A03 | Una serie con 0 o menos repeticiones se rechaza. | S10-R03 | Prueba unitaria |
| S10-A04 | "  Press   de  banca " se guarda como "Press de banca". Un nombre vacío o solo con espacios se rechaza. | S10-R04 | Prueba unitaria |
| S10-A05 | Con "Press de banca" en el catálogo, crear "press  DE banca" se rechaza, también si el existente está archivado. En ese caso el rechazo indica que hay un archivado con ese nombre. | S10-R05, S10-R21 | Prueba unitaria |
| S10-A06 | Crear un ejercicio sin equipo, sin músculos o sin ningún músculo principal se rechaza. Uno con dos principales y un secundario se acepta. | S10-R06, S10-R07 | Prueba unitaria |
| S10-A07 | Un ejercicio con el mismo músculo dos veces, aunque una sea principal y otra secundaria, se rechaza. | S10-R08 | Prueba unitaria |
| S10-A08 | En una sesión con series de `order` 1, 2 y 3, borrar la 2 deja 1 y 3, y la siguiente serie nueva recibe 4. | S10-R09, S10-R24 | Prueba unitaria |
| S10-A09 | Agregar una serie a un ejercicio archivado se rechaza. Corregir el peso de una serie existente de ese ejercicio se acepta. | S10-R11 | Prueba unitaria |
| S10-A10 | Una nota "  asiento en 4 " se guarda como "asiento en 4", y una nota "   " se guarda como `null`. | S10-R12 | Prueba unitaria |
| S10-A11 | Eliminar un ejercicio con series lo deja archivado, fuera del catálogo activo y con sus series intactas. Desarchivarlo lo devuelve al catálogo. | S10-R17 a S10-R19 | Prueba unitaria |
| S10-A12 | Eliminar un ejercicio sin series lo borra junto con sus músculos. | S10-R20 | Prueba unitaria |
| S10-A13 | Renombrar un ejercicio con series conserva su `id`, su última vez y su evolución. | S10-R22, S01-R07 | Prueba unitaria |
| S10-A14 | Eliminar una sesión elimina sus series y no cambia ningún ejercicio. | S10-R23 | Prueba unitaria |
| S10-A15 | Con dos sesiones del mismo día que contienen el ejercicio, la última vez devuelve todas las series de la de mayor `id`, en orden de `order`. Desde una sesión en curso, devuelve la sesión anterior a ella. | S10-R25, S10-R26, S10-R14 | Prueba unitaria |
| S10-A16 | La evolución de un ejercicio con series en tres sesiones las devuelve todas, ordenadas por fecha, `id` de sesión y `order`. | S10-R27 | Prueba unitaria |
| S10-A17 | El historial ordena por fecha descendente y, en el mismo día, por `id` descendente. | S10-R28 | Prueba unitaria |
| S10-A18 | Las series de una sesión salen en orden de `order` e incluyen las de ejercicios archivados. | S10-R29 | Prueba unitaria |
| S10-A19 | El catálogo activo excluye archivados, ordena por nombre y filtra por músculo (principal o secundario), por equipo o por ambos. Los archivados se listan aparte. | S10-R30, S10-R31 | Prueba unitaria |
| S10-A20 | Ningún archivo del modelo de dominio importa `android.`, `androidx.` ni librerías de terceros. | S10-R16 | `grep -rnE "^import (android\|androidx)\." <paquete de dominio>` sobre `feature/catalog/domain` y `feature/session/domain`, sin resultados |
| S10-A21 | El documento de esta spec incluye la tabla de cumplimiento de S01-R06 a S01-R09 con sus IDs. | S01-R06 a R09 | Revisión de esta spec |
| S10-A22 | Crear una sesión con fecha posterior a la del reloj inyectado es rechazado; con la fecha de hoy es aceptado. | S10-R32 | Prueba unitaria |
| S10-A23 | "PRESS", "press  de" y "prés" encuentran "Press de banca". "pena" y "peña" encuentran "Remo de peña". Un texto vacío encuentra todos. | S10-R33 | Prueba unitaria (F02) |
| S10-A24 | Con series de 60, 62,5 y 60 kg en una sesión y de 65 kg en otra posterior, el peso más alto es 62,5 kg y luego 65 kg, en orden de sesión. Un ejercicio sin series da vacío. | S10-R34 | Prueba unitaria (F05) |
| S10-A25 | Con pesos más altos de 60, 62,5, 62,5 y 65 kg en sesiones sucesivas, solo la segunda y la cuarta son récord. La primera no lo es. | S10-R35 | Prueba unitaria (F05) |

S10-R10, S10-R13 y S10-R14 se comprueban también en la Spec 20, porque dependen de cómo se persiste. S10-R15 y S10-R32 se comprueban con pruebas unitarias del dominio.

## Decisiones abiertas

- [x] **Máquina y ejercicio.** Decidido el 2026-10-10: una máquina no es una entidad aparte. "Máquina" es un valor de `Equipment` de un ejercicio, como dice la Spec 02.
- [x] **Alcance de la nota.** Decidido el 2026-10-10: solo hay dos notas, la fija del ejercicio (`Exercise.notes`, F01) y la de la sesión (`WorkoutSession.notes`, F02). No hay nota del ejercicio dentro de una sesión ni nota por serie.
- [x] **Alcance de "Última vez".** Decidido el 2026-10-10: todas las series del ejercicio en la sesión más reciente que lo contiene (S10-R25).
- [x] **Representación del peso.** Decidido el 2026-10-10: kilogramos con precisión de 0,25 kg, guardados como entero de gramos (`weightGrams`).
- [x] **Límites superiores.** Aprobados el 2026-10-10 tal como están en S10-R15.
- [x] **Fechas futuras.** Decidido el 2026-10-10: no se permiten (S10-R32).
- [x] **Ajustes por la Spec 20.** Aprobado el 2026-10-10: S10-R16 permite `kotlinx.coroutines`, un objeto no guardado lleva `id = 0` y `normalized_name` de la Spec 20 depende de `normalizeExerciseName` y `nameKey`.
- [x] **Última vez desde una sesión en curso.** Confirmado el 2026-10-10: S10-R26 excluye la sesión en curso y las posteriores a ella.
- [x] **Ñ en la búsqueda.** Decidido el 2026-10-11: A, la "ñ" cuenta como "n" en la búsqueda (el dueño aceptó de antemano las recomendaciones). A: quitar todas las marcas diacríticas, incluida la de la "ñ". B: quitar las tildes pero conservar la "ñ" como letra propia. Por qué A: una búsqueda tolerante solo muestra algún resultado de más, nunca esconde el que se busca; sirve con teclados sin "ñ"; y es una sola regla (NFD sin `Mn`) sin casos especiales. B es más fiel al alfabeto, pero obliga a un caso especial y esconde "Remo de peña" a quien escribe "pena". Costo: "ano" encuentra también "año". La unicidad de S10-R05 no cambia: allí la "ñ" sigue siendo distinta.
- [x] **Ajustes por la Spec 30.** Aprobado el 2026-10-11: el dominio se queda en su paquete (ADR-30-03), las interfaces de S20-R42 se mantienen (ADR-30-05), el reloj de S10-R32 viene del `AppContainer` (ADR-30-09) y `nameKey` sigue `internal` (ADR-30-02).
