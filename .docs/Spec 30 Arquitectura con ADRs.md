# Spec 30: Arquitectura con ADRs

Oct 11, 2026 · @Juan

| ID | Estado | Fase | Depende de |
| --- | --- | --- | --- |
| 30 | Aprobada | 2 Arquitectura | 00, 10 |

## Propósito

Esta spec fija el detalle de la arquitectura que la constitución deja a la Spec 30: capas, paquetes, módulos, inyección de dependencias, repositorios y casos de uso, errores, estado de UI, coroutines, reloj, navegación y build. Cada decisión queda en un ADR con su contexto, sus alternativas y sus consecuencias.

No agrega funcionalidad. Toma como contrato lo que ya existe: el dominio de la Spec 10 y la persistencia de la Spec 20, tal como están en el código (commit `4ce5ffb`, revisado el 2026-10-11). Tampoco decide el diseño de la interfaz (Specs 40 a 42) ni la estrategia de pruebas (Spec 31): solo deja las costuras que esas specs necesitan.

Cómo se aplica:

- Esta spec no escribe código de producción por sí sola. Las reglas que ya se pueden comprobar sobre el código actual tienen criterios de aceptación propios (S30-A01 a S30-A11).
- Cada spec F (F00 a F04) aplica estas reglas al construir su corte vertical y presenta la evidencia de los criterios S30-A12 a S30-A19 y S30-A22 que le tocan.
- Las dependencias nuevas de la sección "Dependencias" quedan aprobadas con esta spec. Cada una entra con la primera spec que la usa.

El dueño aprobó esta spec el 2026-10-11 y aceptó las seis decisiones abiertas con la opción recomendada (A). Todos los ADR quedan aceptados en esa fecha, salvo el uso del `AppContainer` de ADR-30-04, que el dueño ya había decidido el 2026-10-10.

## Decisiones del dueño que esta spec toma como dadas

| Tema | Decisión | Fecha |
| --- | --- | --- |
| Inyección de dependencias | Manual, con un `AppContainer` creado en una `Application` propia | 2026-10-10 (Spec 20, constitución) |
| Asincronía | Coroutines y Flow | 2026-10-10 (constitución) |
| `Flow` en el dominio | El dominio puede usar `kotlinx.coroutines`, incluido `Flow` (S10-R16) | 2026-10-10 (Spec 20) |
| Interfaces de repositorio | `ExerciseRepository`, `SessionRepository` y `ExerciseWriteResult` tal como están en S20-R42 | 2026-10-10 (Spec 20) |
| Paquetes compartidos | `data/` en la raíz para la base de datos; `AppContainer` y `WeightrackApplication` en el paquete raíz | 2026-10-10 (Spec 20, Spec 03) |
| Pruebas de base de datos | Instrumentadas, en `src/androidTest` | 2026-10-10 (Spec 20) |
| Calidad de código | Solo el lint integrado de Android | 2026-10-10 (constitución) |
| Versiones | Se pueden ajustar por compatibilidad, anotando el motivo en la spec | 2026-10-11 (Spec 20) |

## Estado actual

Lo que hay en el código hoy y qué ADR lo decide.

| Elemento | En el código (commit `4ce5ffb`) | ADR |
| --- | --- | --- |
| Módulos | Solo `:app` en `settings.gradle.kts` | ADR-30-02 |
| Paquetes | Raíz (`MainActivity`, `WeightrackApplication`, `AppContainer`), `data/`, `ui/theme/`, `feature/catalog/{domain,data}` y `feature/session/{domain,data}`. No hay ningún paquete `ui/` de feature | ADR-30-03 |
| Dependencias entre features | `session` importa de `catalog`: su dominio usa `Exercise`, `normalizeNotes` y `NOTES_MAX_LENGTH`; sus datos usan `ExerciseEntity` (clave foránea y fila de `JOIN`) y el mapper `toDomain()`. `catalog` no importa nada de `session`; solo el SQL de `ExerciseDao` cuenta filas de `exercise_set` (Spec 20) | ADR-30-03 |
| Inyección | `WeightrackApplication` crea un único `AppContainer`. El contenedor construye la base de datos de forma perezosa y expone `exerciseRepository` y `sessionRepository` con el tipo de la interfaz | ADR-30-04 |
| Repositorios | Las interfaces de S20-R42 en los paquetes `domain`, implementadas por `RoomExerciseRepository` y `RoomSessionRepository` | ADR-30-05 |
| Errores | El dominio devuelve resultados sellados con enums (`ExerciseValidation`, `ExerciseSetValidation`, `WorkoutSessionValidation`, `ExerciseWriteResult`, `ExerciseDeletion`). `ExerciseRepository.delete` de un id inexistente lanza `IllegalStateException` (Spec 20) | ADR-30-06 |
| UI | Una sola Activity con `enableEdgeToEdge()` y un `Scaffold` que muestra el nombre de la app. No hay ViewModels ni estado de UI | ADR-30-07 |
| Coroutines | Room 3 ejecuta sus consultas en `Dispatchers.IO` por defecto (hechos de la Spec 20). No hay `withContext`, `Dispatchers` ni `GlobalScope` en `src/main` | ADR-30-08 |
| Reloj | `validateSession` recibe un `java.time.Clock` (S10-R32). El `AppContainer` todavía no expone ninguno | ADR-30-09 |
| Navegación | No hay librería de navegación | ADR-30-10 |
| Lifecycle | `lifecycle-runtime-ktx` 2.11.0. No están `lifecycle-viewmodel-compose` ni `lifecycle-runtime-compose` | ADR-30-04, ADR-30-07 |
| Serialización | `kotlinx-serialization-core` 1.11.0 como `runtimeOnly` (Spec 20). No hay plugin de serialización | ADR-30-10 |
| Build | Kotlin DSL, `gradle/libs.versions.toml`, plugins con `alias` y `apply false` en la raíz | ADR-30-11 |

## Formato de los ADR

Cada ADR usa esta plantilla:

| Campo | Contenido |
| --- | --- |
| Título | `ADR-30-<nn>: <decisión en pocas palabras>` |
| Estado | Propuesto, Aceptado (con fecha), Reemplazado por `ADR-30-<mm>` u Obsoleto |
| Fecha | Día en que se propuso, `yyyy-MM-dd` |
| Contexto | El problema, las restricciones y lo que dicen la constitución, las specs, el código y las skills |
| Decisión | Qué se decide, en frases cortas |
| Alternativas consideradas | Tabla con cada opción, a favor y en contra |
| Consecuencias | Qué se gana, qué cuesta y qué cambia en otras specs |
| Requisitos | Los `S30-R<nn>` que la vuelven verificable |

### Índice de ADR

| ADR | Decisión | Estado |
| --- | --- | --- |
| ADR-30-01 | Los ADR viven en esta spec | Aceptado el 2026-10-11 |
| ADR-30-02 | Un solo módulo Gradle | Aceptado el 2026-10-11 |
| ADR-30-03 | Capas y paquetes por feature | Aceptado el 2026-10-11 |
| ADR-30-04 | Inyección manual con `AppContainer` y fábricas de ViewModel | Aceptado el 2026-10-10 (contenedor); Aceptado el 2026-10-11 (fábricas) |
| ADR-30-05 | Repositorios de S20-R42 y casos de uso solo para lógica compartida | Aceptado el 2026-10-11 |
| ADR-30-06 | Rechazos esperados como resultados sellados, fallos como excepciones | Aceptado el 2026-10-11 |
| ADR-30-07 | Estado de UI único con `StateFlow` y flujo unidireccional | Aceptado el 2026-10-11 |
| ADR-30-08 | Coroutines sin dispatchers escritos a mano | Aceptado el 2026-10-11 |
| ADR-30-09 | Reloj inyectado desde el `AppContainer` | Aceptado el 2026-10-11 |
| ADR-30-10 | Navegación con Navigation 3 | Aceptado el 2026-10-11 |
| ADR-30-11 | Gradle con Kotlin DSL y version catalog | Aceptado el 2026-10-11 |

## ADR-30-01: Los ADR viven en esta spec

**Estado:** Aceptado el 2026-10-11. **Fecha:** 2026-10-11.

**Contexto.** La constitución dice que el detalle de la arquitectura "se decide en la Spec 30 con ADRs" y que toda librería, módulo o patrón nuevo necesita un ADR (regla 3, principio 6). Hace falta un lugar único y fácil de leer para un agente, que ya lee la constitución y la spec de su tarea.

**Decisión.** Todos los ADR viven en esta spec, numerados `ADR-30-<nn>`. Un ADR nuevo se agrega al final como enmienda de esta spec, con la aprobación del dueño. Un ADR aceptado no se reescribe: otro ADR lo reemplaza y el viejo cambia su estado.

**Alternativas consideradas.**

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Dentro de la Spec 30 (elegida) | Un solo documento para toda la arquitectura. Coincide con lo que dice la constitución. Un agente lee una spec, no una carpeta | La spec crece con el tiempo |
| B. Un archivo por ADR (`.docs/adr/ADR-<nnn> <título>.md`) | Formato habitual en la industria. Historial por decisión. La spec no crece | Un agente tiene que leer varios archivos. Separa el ADR de los requisitos que lo vuelven verificable |
| C. Cada ADR en la spec que lo motiva | La decisión queda junto a su uso | Las decisiones quedan dispersas y es difícil saber cuáles rigen |

**Consecuencias.** Las demás specs no escriben ADR propios: proponen uno nuevo para esta spec en su tabla "Cambios que esta spec pide a otras specs". Si esta spec supera unos 20 ADR o se vuelve difícil de leer, un ADR nuevo puede mover los ADR a archivos propios (opción B).

| ID | Requisito |
| --- | --- |
| S30-R01 | Toda decisión de arquitectura (librería, módulo, patrón o abstracción nueva) se registra como ADR en esta spec, con la plantilla de "Formato de los ADR" y en el índice. Los números no se reutilizan. Un ADR aceptado solo cambia su estado; para cambiar la decisión se agrega otro ADR que lo reemplaza. |
| S30-R02 | Una spec que necesita una decisión de arquitectura la propone como ADR nuevo en su tabla "Cambios que esta spec pide a otras specs". El agente no aplica la decisión hasta que el dueño la acepta (constitución, reglas 3 y 5). |
| S30-R03 | Cada spec F lista los requisitos de esta spec que aplica e incluye, entre sus criterios, la evidencia de los criterios S30-A12 a S30-A19 y S30-A22 que le correspondan. |

## ADR-30-02: Un solo módulo Gradle

**Estado:** Aceptado el 2026-10-11. **Fecha:** 2026-10-11.

**Contexto.** El proyecto es un solo módulo `:app`. La constitución pide justificar la modularización con un ADR (principio 6). La Spec 20 descartó separar la persistencia en un módulo JVM o KMP (opción C) para no modularizar. La Spec 10 anota que, si se separan módulos, `nameKey` (hoy `internal`) tendría que ser público.

**Decisión.** La primera versión tiene un solo módulo, `:app`. Los límites entre capas y features se comprueban con los criterios de aceptación de esta spec (búsquedas de imports), no con la visibilidad entre módulos.

**Alternativas consideradas.**

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Un solo módulo (elegida) | Sin configuración extra. Build simple. Es lo que existe | Los límites dependen de revisiones y búsquedas, no del compilador |
| B. Módulos por capa (`:domain`, `:data`, `:app`) | El compilador impide que el dominio importe Android. Pruebas JVM más rápidas | Más configuración de Gradle. `nameKey` pasa a público. Sin necesidad medida |
| C. Módulos por feature | Límites entre features forzados por el compilador. Builds incrementales en proyectos grandes | Mucha estructura para cinco features y un usuario. Exige convention plugins |

**Consecuencias.** `nameKey` sigue `internal`. Se reconsidera la modularización, con un ADR que reemplace a este, cuando ocurra alguno de estos disparadores:

1. El dueño mide que el tiempo de build frena el trabajo diario.
2. Hace falta probar Room en la JVM sin dispositivo (opción C de la Spec 20).
3. Aparece un segundo destino que comparte código, por ejemplo un reloj o un widget. Hoy está fuera de alcance (Spec 01).
4. El registro de fallos del agente muestra violaciones repetidas de los límites que los criterios de esta spec no evitan.

| ID | Requisito |
| --- | --- |
| S30-R04 | El proyecto tiene un solo módulo Gradle, `:app`. Agregar otro exige un ADR que reemplace a ADR-30-02. |

## ADR-30-03: Capas y paquetes por feature

**Estado:** Aceptado el 2026-10-11. **Fecha:** 2026-10-11.

**Contexto.** La constitución fija tres capas (principio 1), dependencias hacia adentro (principio 4), cortes verticales (principio 5) y que "cada funcionalidad nueva vive en su propio paquete dentro de `feature/`". La Spec 03 dejó a esta spec el nombre de cada feature y el reparto fino entre capas. La Spec 10 dejó el dominio en `feature/catalog/domain` y `feature/session/domain` y permite moverlo con un ADR.

En Weightrack, "dominio" significa modelos, reglas, interfaces de repositorio y casos de uso. No es lo mismo que la "domain layer" de la guía de arquitectura de Google, que es opcional y contiene solo casos de uso; en esa guía los repositorios viven en la capa de datos. Weightrack declara las interfaces en el dominio por el principio 4.

F03 (última vez) no tiene datos propios: lee `SessionRepository.observeLastPerformance` y `LastPerformance`, que ya viven en `session`, y se muestra al registrar un ejercicio (S01-R03). F04 (historial) tiene pantallas propias y corrige sesiones y series "con las mismas reglas de F02" (Spec 10, tabla de S01-R09).

**Decisión.**

- Paquete por feature y, dentro, un subpaquete por capa: `domain`, `data` y `ui`.
- El dominio se queda donde lo puso la Spec 10. No se mueve nada.
- Nombres de feature: `catalog` (F01), `session` (F02), `lastperformance` (F03) y `history` (F04). Siguen la columna "Nombre en código" de la Spec 02 (`LastPerformance`, `SessionHistory`). F03 y F04 van en paquetes propios (D2).
- F03 y F04 no tienen `data/`, porque no son dueñas de tablas (S01-R09). Usan el dominio de `session` y de `catalog`.
- El shell de F00 (composable raíz y navegación) vive en el paquete raíz `ui/`, junto a `ui/theme/`, fuera de `feature/` (D3). El shell es la única pieza que conoce todas las features.
- Las features forman un grafo sin ciclos: `catalog` ← `session` ← `lastperformance` y `history`.
- F03 se muestra dentro de la pantalla de registro de F02 sin que `session` dependa de `lastperformance`: la pantalla de F02 declara en su spec un parámetro composable (un hueco) para la última vez, y el shell lo llena con el composable de F03. Si las Specs 40 a 42 deciden otra forma de mostrarla, este punto se ajusta allí.

Ubicación del código:

```
com/juancholopes/weightrack/
  MainActivity.kt               única Activity: enableEdgeToEdge() y setContent con el shell
  WeightrackApplication.kt      Application propia: crea el AppContainer
  AppContainer.kt               raíz de composición (ADR-30-04)
  data/                         WeightrackDatabase y Converters, compartidos (Spec 20)
  ui/                           shell de F00: composable raíz y navegación (D3)
    theme/                      tema Material 3 compartido (Spec 41)
  feature/
    catalog/                    F01
      domain/                   modelos, reglas, ExerciseRepository
      data/                     entidades, ExerciseDao, mappers, RoomExerciseRepository
      ui/                       pantallas, ViewModels, UiState y claves de navegación
    session/                    F02
      domain/                   modelos, reglas, SessionRepository y casos de uso compartidos con F04
      data/                     entidades, SessionDao, filas, mappers, RoomSessionRepository
      ui/
    lastperformance/            F03 (D2)
      ui/
    history/                    F04 (D2)
      ui/
```

**Alternativas consideradas.**

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Feature y luego capa (elegida) | Corte vertical visible. Las reglas entre capas se comprueban con una búsqueda por ruta. Es el esqueleto de la Spec 03 | Más carpetas |
| B. Capa y luego feature (`ui/catalog`, `domain/catalog`) | Agrupa lo técnico | Una feature queda repartida en tres árboles. Contradice el principio 5 y la convención de `feature/` |
| C. Feature sin subpaquetes de capa | Menos carpetas | No se puede comprobar con una búsqueda que la UI no importe datos |

Las opciones para F03 y F04 y para el shell están en "Decisiones abiertas" (D2 y D3).

**Consecuencias.** Agregar una feature es agregar su paquete y una línea en el shell, sin tocar otras features salvo por contratos explícitos (un método nuevo en una interfaz de repositorio o un hueco en una pantalla, siempre anotados en la spec F). La dependencia de `session` hacia `catalog` que ya existe queda aceptada como contrato: `ExerciseSet` referencia a `Exercise` (Spec 10), y `session.data` usa `ExerciseEntity` para su clave foránea y sus filas de `JOIN` (Spec 20). `normalizeNotes` y `NOTES_MAX_LENGTH` siguen en `catalog.domain`; moverlos a un paquete común sería una abstracción sin necesidad, porque `session` ya depende de `catalog`.

| ID | Requisito |
| --- | --- |
| S30-R05 | El código sigue el árbol de "Ubicación del código" de este ADR. |
| S30-R06 | Los paquetes de feature son `catalog` (F01), `session` (F02), `lastperformance` (F03) y `history` (F04), como decidió el dueño en D2. Una feature futura elige su nombre con el glosario y lo agrega a este ADR. |
| S30-R07 | Cada feature crea solo los subpaquetes `domain`, `data` y `ui` que usa, cuando la primera clase los necesita (S03-R09). Una feature tiene `data/` solo si es dueña de tablas (S01-R09). |
| S30-R08 | El dominio sigue en `feature/catalog/domain` y `feature/session/domain`, como en la Spec 10. |
| S30-R09 | Reglas entre capas: `domain` no importa `android.*`, `androidx.*` (S10-R16) ni ningún paquete `data` o `ui` de la app. `ui` importa `domain` y nunca importa paquetes `data` (de feature o de la raíz) ni `androidx.room3`. `data` implementa el `domain` de su feature. |
| S30-R10 | Reglas entre features: `catalog` no importa ninguna otra feature. `session` puede importar `catalog.domain`, y `session.data` puede importar entidades y mappers de `catalog.data` solo para claves foráneas y filas de consulta. `lastperformance` y `history` pueden importar `session.domain` y `catalog.domain`. Ninguna feature importa el `ui` de otra: solo el shell importa los paquetes `ui` de las features. Una dependencia nueva entre features es un contrato explícito anotado en la spec F. |
| S30-R11 | La app tiene una sola Activity, `MainActivity`, que llama a `enableEdgeToEdge()` (S03-R12) y muestra el composable raíz del shell. Todas las pantallas son composables; no hay Fragments. |
| S30-R12 | Cada pantalla vive en `feature/<feature>/ui/` y se compone de `<Nombre>Screen` (composable de pantalla: obtiene el ViewModel, recolecta el estado y conecta los callbacks), `<Nombre>Content` (composable sin estado, ver S30-R31), `<Nombre>ViewModel` y `<Nombre>UiState`. Cumple la convención de la constitución: composable de pantalla, ViewModel y estado de UI inmutable. |

## ADR-30-04: Inyección manual con `AppContainer` y fábricas de ViewModel

**Estado:** Aceptado el 2026-10-10 el uso del `AppContainer` (decisión del dueño en la Spec 20). Aceptado el 2026-10-11 el mecanismo de fábricas de ViewModel. **Fecha:** 2026-10-11.

**Contexto.** El dueño decidió la inyección manual el 2026-10-10. El `AppContainer` ya construye la base de datos y los repositorios. Falta definir cómo recibe sus dependencias un ViewModel. La guía oficial "Create ViewModels with dependencies" recomienda una fábrica con `viewModelFactory { initializer { ... } }` que lee la `Application` de `CreationExtras` con `APPLICATION_KEY` y se usa en Compose con `viewModel(factory = ...)`. Con Navigation 3, el decorador `rememberViewModelStoreNavEntryDecorator()` pasa a cada entrada la fábrica y los `CreationExtras` por defecto de la Activity, incluido `APPLICATION_KEY` (código fuente de AndroidX, revisado el 2026-10-11).

La skill `testing-setup` dice que, si no hay framework de inyección, se instale Hilt. Choca con la decisión del dueño; se registró como propuesta en D4 y el dueño mantuvo la inyección manual.

**Decisión.**

- El `AppContainer` es la única raíz de composición: construye la base de datos, los repositorios, el reloj (ADR-30-09) y los casos de uso (ADR-30-05), y solo expone tipos del dominio.
- Cada ViewModel recibe por constructor interfaces del dominio, casos de uso, `Clock` y los argumentos de navegación como valores simples (ids). Nunca recibe el `AppContainer`, un `Context`, la `Application` ni una clave de navegación.
- Cada ViewModel declara su fábrica en su `companion object` con `viewModelFactory { initializer { ... } }`. La fábrica lee el contenedor con una extensión `CreationExtras.appContainer`, declarada una sola vez en `WeightrackApplication.kt` por la primera spec que crea un ViewModel.
- El composable de pantalla obtiene el ViewModel con `viewModel(factory = ...)`.

Ejemplo (los nombres reales los fijan las specs F):

```kotlin
// WeightrackApplication.kt
val CreationExtras.appContainer: AppContainer
    get() = (checkNotNull(this[APPLICATION_KEY]) as WeightrackApplication).container

// feature/catalog/ui/ExerciseEditorViewModel.kt
class ExerciseEditorViewModel(
    private val exerciseId: Long,                  // 0 = ejercicio nuevo (Spec 10)
    private val exercises: ExerciseRepository,
) : ViewModel() {
    companion object {
        fun factory(exerciseId: Long) = viewModelFactory {
            initializer { ExerciseEditorViewModel(exerciseId, appContainer.exerciseRepository) }
        }
    }
}

// En la entrada de navegación
ExerciseEditorScreen(viewModel = viewModel(factory = ExerciseEditorViewModel.factory(key.exerciseId)))
```

**Alternativas consideradas.**

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Fábrica en el `companion` con `CreationExtras` (elegida) | Es la forma de la guía oficial. Sin librerías. El ViewModel se prueba construyéndolo con fakes | Un poco de código repetido por ViewModel |
| B. El `AppContainer` expone una fábrica por pantalla | Las fábricas quedan en un solo lugar | El contenedor conoce clases de UI y cambia con cada pantalla nueva, lo que toca un archivo compartido por cada feature |
| C. Un `CompositionLocal` con el contenedor | Cualquier composable lo alcanza | Dependencia oculta. Acerca los composables a los datos, contra la convención de la constitución |
| D. Hilt (lo que sugiere la skill `testing-setup`) | Grafo verificado al compilar. Reglas de prueba listas | Plugin, anotaciones y procesamiento extra. Cambia una decisión ya tomada en la constitución (ver D4) |

**Consecuencias.** Las pruebas construyen cada ViewModel con fakes de las interfaces y un `Clock` fijo, sin Android (costura para la Spec 31). Cada spec F agrega a su ViewModel la fábrica en el `companion`, sin tocar el `AppContainer` salvo para exponer un caso de uso nuevo.

| ID | Requisito |
| --- | --- |
| S30-R13 | El `AppContainer` es el único lugar de `src/main` que construye la base de datos, las implementaciones de repositorio, el reloj y los casos de uso. Ningún ViewModel ni composable los construye. |
| S30-R14 | El `AppContainer` expone solo tipos del dominio (interfaces de repositorio, casos de uso y `Clock`). La base de datos y los DAOs son privados. |
| S30-R15 | Cada ViewModel recibe por constructor solo interfaces del dominio, casos de uso, `Clock` y argumentos de navegación como valores simples. No recibe el `AppContainer`, un `Context`, la `Application` ni una clave de navegación. |
| S30-R16 | Cada ViewModel declara su fábrica en su `companion object` con `viewModelFactory { initializer { ... } }` y lee el contenedor con la extensión `CreationExtras.appContainer` de `WeightrackApplication.kt`. Un ViewModel con argumentos expone `fun factory(...)`. El composable de pantalla lo obtiene con `viewModel(factory = ...)`. |

## ADR-30-05: Repositorios de S20-R42 y casos de uso solo para lógica compartida

**Estado:** Aceptado el 2026-10-11. **Fecha:** 2026-10-11.

**Contexto.** La Spec 20 definió interfaces mínimas y dejó a esta spec mantenerlas o reemplazarlas. El principio 1 permite casos de uso "cuando haya lógica compartida". La guía de Google dice que la capa de casos de uso es opcional y que obligar a pasar siempre por ella "forces you to add use cases even when they are just simple function calls to the data layer".

Las reglas del dominio ya son funciones puras (`validateExercise`, `validateNewSet`, `validateSetCorrection`, `validateSession`, `deleteExercise`). La Spec 20 decidió que la base de datos no repite las invariantes de valor y que el dominio las valida antes de escribir (S20-R28). Las escrituras de sesiones y series las usan F02 y F04 con las mismas reglas.

**Decisión.**

- Se mantienen `ExerciseRepository` y `SessionRepository` de S20-R42 como contrato. Una feature que necesita una consulta o escritura nueva la agrega a la interfaz de la feature dueña de los datos, en su propia spec y como contrato explícito. Por ejemplo, F01 puede agregar una lectura de todos los ejercicios si `validateExercise` la necesita.
- Un ViewModel puede llamar al repositorio y a las funciones puras del dominio directamente cuando la operación la usa una sola pantalla.
- Un caso de uso existe solo cuando la misma lógica (validación del dominio más llamadas a repositorios) la usan dos o más ViewModels o dos features.
- Las escrituras validadas de sesiones y series pasan por casos de uso de `feature/session/domain`, porque F02 y F04 las comparten: crear o editar una sesión (`validateSession`), agregar una serie (`validateNewSet`) y corregir una serie (`validateSetCorrection`). Los nombres exactos los fija la spec F02. Borrar una sesión o una serie no tiene reglas (S10-R23, S10-R24) y va directo al repositorio.
- Una operación que tiene que leer, decidir con el dominio y escribir de forma atómica vive en la implementación del repositorio, dentro de una transacción de Room, como S20-R40. Las transacciones son asunto de los datos, no de un caso de uso ni de un ViewModel.

**Alternativas consideradas.**

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Casos de uso solo para lógica compartida (elegida) | Cumple el principio 1 y la guía de Google. Sin clases que solo reenvían | Dos formas de llegar a los datos; el criterio tiene que estar claro |
| B. Siempre casos de uso | Regla única. La UI nunca toca un repositorio | Muchas clases que solo reenvían una llamada. Contra el principio 6 |
| C. Nunca casos de uso | Menos clases | F02 y F04 duplicarían la validación de sesiones y series |
| D. Interfaces pequeñas por pregunta (por ejemplo `LastPerformanceReader`) | Fakes más pequeños | Más tipos. Reemplaza interfaces recién aprobadas sin un problema concreto |

**Consecuencias.** Un caso de uso es una clase de `feature/<dueña>/domain/` llamada `<Verbo><Sustantivo>UseCase`, como en la guía de Google, con una sola función pública `suspend operator fun invoke(...)` o una que devuelve `Flow`. Recibe por constructor interfaces y `Clock`, y no importa Android. Por ejemplo, agregar una serie lee el ejercicio con `ExerciseRepository` y las series de la sesión con `SessionRepository`, valida con `validateNewSet` y escribe con `addSet`.

| ID | Requisito |
| --- | --- |
| S30-R17 | `ExerciseRepository` y `SessionRepository` de S20-R42 son el contrato de datos. Toda consulta o escritura nueva se agrega a la interfaz de la feature dueña de los datos, en la spec que la necesita, y se anota como contrato explícito. No hay repositorio genérico ni repositorio por pantalla. |
| S30-R18 | Un ViewModel puede llamar directamente al repositorio y a las funciones puras del dominio cuando la operación la usa una sola pantalla. |
| S30-R19 | Un caso de uso existe solo si su lógica la usan dos o más ViewModels o dos features. Vive en `feature/<dueña>/domain/`, se llama `<Verbo><Sustantivo>UseCase`, tiene una sola función pública (`suspend operator fun invoke` o una que devuelve `Flow`), recibe interfaces y `Clock` por constructor y no importa Android. |
| S30-R20 | Crear o editar una sesión, agregar una serie y corregir una serie pasan por casos de uso de `feature/session/domain` que validan con el dominio antes de escribir. Ningún ViewModel llama directamente a `createSession`, `updateSession`, `addSet` ni `updateSet`. |
| S30-R21 | Una operación que lee, decide con el dominio y escribe de forma atómica se implementa en el repositorio dentro de una transacción de Room (patrón S20-R40), no en un caso de uso ni en un ViewModel. |

## ADR-30-06: Rechazos esperados como resultados sellados, fallos como excepciones

**Estado:** Aceptado el 2026-10-11. **Fecha:** 2026-10-11.

**Contexto.** El dominio ya devuelve sus rechazos como tipos sellados con enums de error (`ExerciseValidation.Invalid(ExerciseError)`, `ExerciseSetValidation`, `WorkoutSessionValidation`, `ExerciseWriteResult.Rejected`). Los enums no tienen textos (Spec 10, "Sin textos de interfaz"). Los fallos que nadie espera, como un id inexistente o un error de SQLite no mapeado, hoy son excepciones (Spec 20).

**Decisión.** Los rechazos de negocio se devuelven como resultados sellados y nunca se lanzan. Los fallos inesperados son excepciones que el ViewModel atrapa en el borde de cada escritura y convierte en un mensaje genérico del estado de UI. La UI traduce cada valor de un enum de error a un recurso de strings.

**Alternativas consideradas.**

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Resultados sellados para rechazos, excepciones para fallos (elegida) | Es lo que ya hace el código. El compilador obliga a tratar cada rechazo con `when` | Dos mecanismos que distinguir |
| B. Excepciones para todo | Una sola vía | Los rechazos no se ven en las firmas. Contradice el código existente |
| C. `kotlin.Result` | Viene en la biblioteca estándar | El error es un `Throwable` sin tipo. `runCatching` también atrapa `CancellationException` |
| D. `Either` de una librería (por ejemplo Arrow) | Errores con tipo y composición | Librería nueva sin necesidad |

**Consecuencias.** Los casos de uso nuevos siguen la misma forma (`Valid`/`Invalid` o `Saved`/`Rejected`). Cada feature tiene en su `ui/` el mapeo de sus enums a strings, por ejemplo `ExerciseError.messageRes()`; los textos los escribe la spec F con la Spec 41.

| ID | Requisito |
| --- | --- |
| S30-R22 | Los rechazos de negocio (invariantes rotas, nombre ocupado) se devuelven como tipos sellados con un enum de error del dominio. Nunca se lanzan como excepción. |
| S30-R23 | Los fallos inesperados son excepciones. El ViewModel las atrapa en el borde de cada escritura y las convierte en un mensaje genérico del `UiState`; las lecturas con `Flow` usan `catch` para emitir un estado de error. Nunca se atrapa ni se descarta `CancellationException`. No se usa `kotlin.Result` ni `runCatching` en el dominio ni en los ViewModels. |
| S30-R24 | Los enums de error no tienen textos. La UI de cada feature traduce cada valor a un recurso de strings en su paquete `ui/`. |

## ADR-30-07: Estado de UI único con `StateFlow` y flujo unidireccional

**Estado:** Aceptado el 2026-10-11. **Fecha:** 2026-10-11.

**Contexto.** El principio 2 pide que la UI emita eventos, que el ViewModel produzca un estado único e inmutable y que la UI solo lo dibuje. La guía "UI events" dice que los eventos que nacen en el ViewModel "should always result in a UI state update" y advierte que los `Channel` y otros flujos reactivos no garantizan la entrega cuando el ViewModel vive más que la UI. La guía de producción de estado usa `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ...)` y pide no lanzar coroutines en `init`. La guía de estado de Compose recomienda `collectAsStateWithLifecycle()` de `lifecycle-runtime-compose`. El modo de saltos fuerte (strong skipping) de Compose está activo por defecto desde Kotlin 2.0.20.

**Decisión.**

- Un `<Nombre>UiState` inmutable por pantalla: `data class` o `sealed interface`, solo con `val` y listas de solo lectura.
- El ViewModel expone un único `val uiState: StateFlow<<Nombre>UiState>`. Los datos de la base de datos llegan por los `Flow` del repositorio y se convierten con `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), <estado inicial>)`. El estado que escribe el usuario (campos de un formulario) vive en un `MutableStateFlow` privado que se combina con esos `Flow` y se cambia con `update { }`.
- Los eventos de la UI llegan al ViewModel como funciones públicas `on<Evento>()`, por ejemplo `onNameChange(name)` u `onSaveClick()` (D5).
- Los efectos de una sola vez (navegar después de guardar, mostrar un mensaje) son campos del `UiState`. La pantalla actúa en un `LaunchedEffect` y avisa al ViewModel con `on<Efecto>Handled()`, que limpia el campo.
- La navegación que inicia el usuario y no necesita lógica de negocio va del composable al callback de navegación sin pasar por el ViewModel.
- El composable de pantalla recolecta con `collectAsStateWithLifecycle()`. `<Nombre>Content` no tiene estado: recibe el `UiState`, lambdas y un `Modifier`.
- No se agregan `kotlinx-collections-immutable` ni anotaciones `@Immutable` o `@Stable` salvo que una medición lo justifique con un ADR.

Ejemplo (los nombres reales los fijan las specs F):

```kotlin
data class ExerciseEditorUiState(
    val name: String = "",
    val error: ExerciseError? = null,
    val savedExerciseId: Long? = null,   // efecto: volver al guardar
)

@Composable
fun ExerciseEditorScreen(onSaved: (Long) -> Unit, viewModel: ExerciseEditorViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.savedExerciseId) {
        uiState.savedExerciseId?.let { onSaved(it); viewModel.onSavedHandled() }
    }
    ExerciseEditorContent(uiState, onNameChange = viewModel::onNameChange, onSaveClick = viewModel::onSaveClick)
}
```

**Alternativas consideradas.**

| Opción | A favor | En contra |
| --- | --- | --- |
| A. `StateFlow` con `stateIn` y `MutableStateFlow` privado (elegida) | Encaja con los `Flow` de los repositorios. Se prueba sin Compose. Es la forma de las guías oficiales | Hay que elegir bien el estado inicial |
| B. `mutableStateOf` de Compose en el ViewModel | Menos código en pantallas simples | Ata el ViewModel al runtime de Compose. Mezcla dos modelos de estado con los `Flow` de Room |
| C. `LiveData` | Conocido | API heredada. No encaja con coroutines ni con Compose |
| D. Efectos con `Channel` o `SharedFlow` | Modelo de "evento" directo | La guía oficial advierte que se pueden perder. Contradice el estado único del principio 2 |

**Consecuencias.** Toda pantalla se puede dibujar y probar desde un `UiState` sin ViewModel (previews y pruebas de UI de la Spec 31). `WhileSubscribed(5_000)` mantiene vivo el flujo de datos durante una rotación y lo detiene cuando la pantalla deja de verse.

| ID | Requisito |
| --- | --- |
| S30-R25 | Cada pantalla tiene un único `<Nombre>UiState` inmutable (`data class` o `sealed interface`), solo con `val`, sin colecciones mutables ni `State` de Compose dentro. |
| S30-R26 | El ViewModel expone solo `val uiState: StateFlow<<Nombre>UiState>` como estado observable. Los datos de la base de datos se convierten con `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ...)`. Todo `MutableStateFlow` es privado y se cambia con `update { }`. No se lanzan coroutines en `init`. |
| S30-R27 | Los eventos de la UI llegan al ViewModel como funciones públicas `on<Evento>()` que devuelven `Unit`, como decidió el dueño en D5. El ViewModel no expone funciones `suspend` públicas. |
| S30-R28 | Los efectos de una sola vez son campos del `UiState`. La pantalla los ejecuta en un `LaunchedEffect` y llama a `on<Efecto>Handled()`. No se usan `Channel`, `SharedFlow` ni `LiveData` para eventos. |
| S30-R29 | La navegación que inicia el usuario sin lógica de negocio va del composable al callback de navegación sin pasar por el ViewModel. La que depende de un resultado (por ejemplo, guardar y volver) sigue S30-R28. |
| S30-R30 | El composable de pantalla recolecta el estado con `collectAsStateWithLifecycle()`. No usa `collectAsState()` ni lee `uiState.value` durante la composición. |
| S30-R31 | `<Nombre>Content` recibe el `UiState`, lambdas y un `Modifier`. No recibe ni crea ViewModels, repositorios ni el `AppContainer`. Las previews y las pruebas de UI lo usan directamente. |

## ADR-30-08: Coroutines sin dispatchers escritos a mano

**Estado:** Aceptado el 2026-10-11. **Fecha:** 2026-10-11.

**Contexto.** Room 3 exige DAOs `suspend` o reactivos y ejecuta sus consultas en `Dispatchers.IO` por defecto (hechos de la Spec 20), así que los repositorios ya son seguros para llamarse desde el hilo principal. La guía de buenas prácticas de coroutines pide no escribir `Dispatchers` a mano, inyectarlos cuando se usan, que el ViewModel cree las coroutines en `viewModelScope`, que las capas de datos expongan `suspend` y `Flow`, y no usar `GlobalScope`. Las funciones del dominio son puras y trabajan con listas pequeñas de un solo usuario.

**Decisión.**

- Los repositorios exponen `suspend` para llamadas únicas y `Flow` para lo que se observa (ya lo hace la Spec 20). Nadie envuelve una llamada a un repositorio en `withContext(Dispatchers.IO)`.
- Los ViewModels lanzan coroutines solo en `viewModelScope`. No hay `GlobalScope`, `runBlocking` ni un `CoroutineScope` de la app en la primera versión.
- Una clase que necesite cambiar de hilo con `withContext` recibe su `CoroutineDispatcher` por constructor desde el `AppContainer`. Hoy ninguna lo necesita, así que no se crea un proveedor de dispatchers.
- Una escritura que precede a una navegación termina antes de navegar: el ViewModel marca el éxito en el `UiState` (S30-R28) y la pantalla navega después. Si el usuario sale antes, la coroutine se cancela y la transacción de Room no deja nada a medias (S20-R38, S20-R39).

**Alternativas consideradas.**

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Sin dispatchers a mano; inyectar solo cuando haga falta (elegida) | Cumple la guía sin abstracciones vacías. Room ya resuelve el hilo | Hay que recordar inyectar si aparece trabajo pesado |
| B. Inyectar un proveedor de dispatchers en todas las clases desde ya | Patrón uniforme | Abstracción que hoy nadie usa. Contra el principio 6 |
| C. `withContext(Dispatchers.IO)` en los repositorios | Explícito | Redundante con Room. Dispatcher escrito a mano, contra la guía |

**Consecuencias.** Las pruebas de ViewModel reemplazan el dispatcher principal con `Dispatchers.setMain(...)` de `kotlinx-coroutines-test` (costura para la Spec 31). Si una feature futura calcula algo pesado, por ejemplo las gráficas de progreso, inyecta un dispatcher en esa clase.

| ID | Requisito |
| --- | --- |
| S30-R32 | Los repositorios exponen `suspend` para llamadas únicas y `Flow` para datos observados. Ninguna clase envuelve llamadas a repositorios en `withContext(Dispatchers.IO)`. |
| S30-R33 | Los ViewModels lanzan coroutines solo en `viewModelScope`. `src/main` no usa `GlobalScope` ni `runBlocking` y no define un `CoroutineScope` de la app. |
| S30-R34 | `src/main` no escribe `Dispatchers.*`. Una clase que necesita `withContext` recibe su `CoroutineDispatcher` por constructor desde el `AppContainer`, con un ADR nuevo si es la primera. |
| S30-R35 | Cuando una escritura precede a una navegación, el ViewModel indica el éxito en el `UiState` y la pantalla navega después de recibirlo, nunca antes de que la escritura termine. |

## ADR-30-09: Reloj inyectado desde el `AppContainer`

**Estado:** Aceptado el 2026-10-11. **Fecha:** 2026-10-11.

**Contexto.** S10-R32 prohíbe sesiones con fecha futura y pide que el dominio obtenga "hoy" de un reloj inyectado. `validateSession` ya recibe un `java.time.Clock`. F02 necesitará "hoy" también como fecha por defecto de una sesión nueva. El `AppContainer` todavía no expone ningún reloj.

**Decisión.** El `AppContainer` expone `val clock: Clock = Clock.systemDefaultZone()`. Todo "hoy" de la app se calcula con `LocalDate.now(clock)` desde ese reloj, que llega a los casos de uso y ViewModels por constructor. Las pruebas usan `Clock.fixed(...)`. Lo agrega la primera spec que lo usa (F02).

**Alternativas consideradas.**

| Opción | A favor | En contra |
| --- | --- | --- |
| A. `java.time.Clock` desde el `AppContainer` (elegida) | Ya es el tipo que usa el dominio. Sin abstracción nueva. Fácil de fijar en pruebas | La zona horaria queda fijada al crear el contenedor |
| B. `LocalDate.now()` directo | Sin código extra | No se puede probar. Contradice S10-R32 |
| C. Una interfaz propia `TimeProvider` | Se puede ampliar | Duplica `java.time.Clock` |
| D. Un `Clock` propio que lee `ZoneId.systemDefault()` en cada llamada | Sigue un cambio de zona horaria con la app abierta | Clase nueva para un caso raro |

**Consecuencias.** Si el usuario cambia de zona horaria con la app abierta, "hoy" usa la zona anterior hasta que el proceso se reinicie. Con un solo usuario y fechas solo de día, se acepta; si llega a importar, la opción D lo resuelve con un ADR nuevo.

| ID | Requisito |
| --- | --- |
| S30-R36 | El `AppContainer` expone un único `Clock` (`Clock.systemDefaultZone()`), que llega por constructor a quien necesite la fecha de hoy. `src/main` no llama a `LocalDate.now()` sin reloj, a `Instant.now()` ni a `System.currentTimeMillis()`. Lo agrega la primera spec que lo necesita (F02). |

## ADR-30-10: Navegación con Navigation 3

**Estado:** Aceptado el 2026-10-11. **Fecha:** 2026-10-11.

**Contexto.** La constitución listaba "Navegación: Navigation para Compose" como Propuesto. Las skills instaladas empujan a Jetpack Navigation 3: `navigation-3` es su guía de instalación y migración, `adaptive` la pone como requisito previo y `navigation-event` pide usar el manejo del gesto atrás de Navigation 3 cuando está presente. Por la regla 9, ese choque se registró como propuesta de cambio a la constitución (ver "Cambios que esta spec pide a otras specs") y el dueño eligió Navigation 3 en D1.

Hechos verificados el 2026-10-11 en las páginas oficiales de versiones y en Google Maven:

- Navigation 3 (`androidx.navigation3`): última estable 1.2.0, del 23 de septiembre de 2026. Hay una alfa más nueva, 1.3.0-alpha02 (7 de octubre de 2026).
- `androidx.lifecycle:lifecycle-viewmodel-navigation3`: última estable 2.11.0, del 17 de junio de 2026. La página "Get started" de Navigation 3 muestra 2.12.0-alpha04, que es alfa; esta spec usa la estable.
- Navigation 3 pide `compileSdk` 36 o más; el proyecto usa 37. `navigation3-ui` 1.2.0 exige como mínimo `activity-compose` 1.12.0, `lifecycle-runtime-compose` 2.10.0, Compose UI 1.10.0 y `kotlinx-serialization-core` 1.7.3; el proyecto tiene 1.13.0, 2.11.0, la BOM 2026.09.00 (UI 1.12.1) y 1.11.0.
- Navigation 2 (`androidx.navigation:navigation-compose`): última estable 2.10.2, del 23 de septiembre de 2026. Su página de versiones dice: "This library is in maintenance mode and will only receive critical fixes; new features are not planned" y recomienda Compose. No nombra a Navigation 3.
- `rememberNavBackStack` exige claves que implementen `NavKey` y tengan `@Serializable`. Las rutas con tipo de Navigation 2 también usan `@Serializable`, así que el plugin de serialización hace falta con cualquiera de las dos.

**Decisión.**

- El shell de F00 posee la navegación: crea la pila con `rememberNavBackStack(...)` y la muestra con `NavDisplay`, con los decoradores `rememberSaveableStateHolderNavEntryDecorator()` y `rememberViewModelStoreNavEntryDecorator()`. Así cada ViewModel queda ligado a su entrada y se limpia al sacarla de la pila.
- Cada feature declara sus claves en su `ui/` como `@Serializable data object` o `data class` que implementan `NavKey`, y solo llevan ids o valores simples. Nunca llevan modelos del dominio: la base de datos es la fuente de verdad y las claves se guardan al morir el proceso.
- Las pantallas reciben la navegación como lambdas (`onOpenExercise: (Long) -> Unit`, `onBack: () -> Unit`); no reciben la pila. Los ViewModels no conocen la navegación.
- Cada feature expone sus entradas como una función de extensión sobre `EntryProviderScope<NavKey>` en su `ui/`, como en la guía de migración de Navigation 3. El shell las registra y traduce cada lambda a una operación sobre la pila.
- Una pila o varias pilas de nivel superior (pestañas), la pantalla inicial y las rutas concretas las decide la Spec 42.

Esquema (los nombres reales los fijan las Specs 42 y F):

```kotlin
// feature/catalog/ui/CatalogKeys.kt
@Serializable data object CatalogKey : NavKey
@Serializable data class ExerciseEditorKey(val exerciseId: Long) : NavKey   // 0 = nuevo

// feature/catalog/ui/CatalogEntries.kt
fun EntryProviderScope<NavKey>.catalogEntries(onOpenExercise: (Long) -> Unit, onBack: () -> Unit) {
    entry<CatalogKey> { CatalogScreen(onOpenExercise = onOpenExercise) }
    entry<ExerciseEditorKey> { key ->
        ExerciseEditorScreen(
            onSaved = { onBack() },
            viewModel = viewModel(factory = ExerciseEditorViewModel.factory(key.exerciseId)),
        )
    }
}

// ui/WeightrackApp.kt (shell de F00)
val backStack = rememberNavBackStack(CatalogKey)
NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = entryProvider {
        catalogEntries(
            onOpenExercise = { id -> backStack.add(ExerciseEditorKey(id)) },
            onBack = { backStack.removeLastOrNull() },
        )
    },
)
```

**Alternativas consideradas.**

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Navigation 3, 1.2.0 (elegida) | La pila es una lista que posee la app: encaja con el flujo unidireccional y se prueba fácil. ViewModel por entrada con `lifecycle-viewmodel-navigation3`. Es lo que piden las skills `navigation-3`, `adaptive` y `navigation-event`. Las Scenes permitirían list-detail en el futuro sin rehacer claves ni ViewModels | Biblioteca más joven y en plena evolución (1.3.0 en alfa). Pilas múltiples y resultados entre pantallas se arman con recetas, con código propio |
| B. Navigation Compose 2.10.2 | Madura y muy documentada. `NavController` resuelve pilas múltiples y deep links | La página oficial la marca en modo mantenimiento, sin funciones nuevas. Contradice las skills (regla 9). La pila vive en `NavController`, fuera del estado de la app. Migrar después a Navigation 3 es un cambio atómico (guía de migración) |

**Consecuencias.** Entran con F00 `navigation3-runtime`, `navigation3-ui`, `lifecycle-viewmodel-navigation3`, el plugin `org.jetbrains.kotlin.plugin.serialization`, y `kotlinx-serialization-core` pasa de `runtimeOnly` a `implementation` (ver "Dependencias"). Las tablets siguen fuera de alcance (Spec 01): no se agregan Scenes ni `material3-adaptive-navigation3` en la primera versión.

| ID | Requisito |
| --- | --- |
| S30-R37 | La navegación usa Navigation 3 (D1). El shell posee una pila creada con `rememberNavBackStack` y la muestra con `NavDisplay` y los decoradores `rememberSaveableStateHolderNavEntryDecorator()` y `rememberViewModelStoreNavEntryDecorator()`. |
| S30-R38 | Cada feature declara sus claves de navegación en su paquete `ui/`, como `@Serializable` que implementan `NavKey`, y solo llevan ids o valores simples. Ninguna clave lleva modelos del dominio ni listas. |
| S30-R39 | Las pantallas reciben la navegación como lambdas y nunca la pila. Los ViewModels no importan `androidx.navigation3` ni conocen claves de navegación. |
| S30-R40 | Cada feature expone sus entradas como una función de extensión sobre `EntryProviderScope<NavKey>` en su `ui/`. El shell es el único que registra entradas y modifica la pila. |

## ADR-30-11: Gradle con Kotlin DSL y version catalog

**Estado:** Aceptado el 2026-10-11. **Fecha:** 2026-10-11.

**Contexto.** La constitución listaba "Build: Gradle con Kotlin DSL y version catalog" como Propuesto, pero el proyecto ya lo usa desde la Spec 03: `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml` y plugins con `alias(...)` y `apply false` en la raíz (S03-R05, S03-R06). La única versión fuera del catálogo es la del plugin `foojay-resolver-convention` en `settings.gradle.kts`, porque Gradle no expone el catálogo en ese bloque `plugins`.

**Decisión.** Se confirma Gradle con Kotlin DSL y version catalog, y la fila de la constitución pasa a Decidido (aplicado el 2026-10-11). Las dependencias nuevas entran solo cuando el dueño las aprueba y las agrega la primera spec que las usa.

**Alternativas consideradas.**

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Kotlin DSL y version catalog (elegida) | Ya está en uso. Tipado y autocompletado. Versiones en un solo archivo, como pide la constitución | Ninguno relevante |
| B. Groovy DSL | Sintaxis histórica | Rehacer lo existente sin beneficio |
| C. Convention plugins (`build-logic`) | Útil con varios módulos | Sin sentido con un solo módulo (ADR-30-02) |

**Consecuencias.** Ninguna tarea nueva: solo cambia el estado de la decisión en la constitución.

| ID | Requisito |
| --- | --- |
| S30-R41 | El build usa Gradle con Kotlin DSL y `gradle/libs.versions.toml`. Los plugins se declaran en la raíz con `apply false` y se aplican en `:app` con `alias(libs.plugins...)`. Ningún `build.gradle.kts` escribe una versión literal (S03-R05). La versión del plugin `foojay-resolver-convention` en `settings.gradle.kts` es la única excepción. |
| S30-R42 | Una dependencia de la tabla "Dependencias" se agrega solo después de que el dueño la apruebe, por la primera spec que la usa, y se declara explícitamente en el catálogo aunque llegue de forma transitiva. |

## Costuras para otras specs

Lo que esta arquitectura deja listo para que otras specs decidan, sin decidirlo aquí.

**Spec 31 (pruebas).**

- Cada ViewModel se construye en una prueba de `src/test` con fakes de `ExerciseRepository` o `SessionRepository` y un `Clock.fixed(...)`, sin Room ni emulador (S30-R15).
- Los casos de uso se prueban igual, con fakes de las interfaces (S30-R19).
- `<Nombre>Content` permite pruebas de UI y de capturas sin ViewModel (S30-R31).
- `viewModelScope` usa el dispatcher principal, así que las pruebas de ViewModel necesitan `kotlinx-coroutines-test` en `testImplementation`; hoy solo está en `androidTestImplementation` (Spec 20).
- La pila de navegación es una lista que posee el shell, así que las pruebas de navegación que pide la skill `testing-setup` (atrás, salida por la pantalla inicial) se pueden escribir contra ella.
- Dónde viven los fakes, si se usa Robolectric para pruebas de UI (lo sugiere la skill `testing-setup`; la Spec 20 solo lo descartó para la base de datos) y las capturas son decisiones de la Spec 31.

**Specs 40 a 42 (flujos, diseño y pantallas).**

- La Spec 42 decide pantallas, rutas, pantalla inicial y si hay una pila o varias pilas de nivel superior. Esta spec solo fija que el shell las posee (S30-R40).
- Edge-to-edge: `MainActivity` mantiene `enableEdgeToEdge()` y el manifiesto `adjustResize`. La skill `edge-to-edge` pide aplicar los insets con un solo método y no poner padding al contenedor de un scaffold adaptable; la Spec 41 o 42 decide si cada pantalla tiene su `Scaffold`.
- Gesto atrás: con Navigation 3, el atrás normal lo maneja `NavDisplay`. Para pedir confirmación ante cambios sin guardar, la skill `navigation-event` recomienda `NavigationBackHandler` (`androidx.navigationevent`) con un solo manejador por estado; la decisión de si hay cambios sin guardar vive en el `UiState`. Si la Spec 42 lo usa, declara la dependencia con su versión verificada. En API 33 a 35, el atrás predictivo necesita `android:enableOnBackInvokedCallback="true"` en el manifiesto (checklist de `navigation-event`); en API 36 o más ya viene activo.
- Cómo se ve la última vez dentro del registro de una sesión (D2) lo deciden las Specs 40 y 42.

## Skills consultadas

Por la regla 9, cada choque con una spec se registra como propuesta y el dueño decide.

| Skill (actualizada) | Qué dice | Efecto en esta spec | ¿Choque? |
| --- | --- | --- | --- |
| `navigation-3` (2026-09-24) | Instalar Navigation 3; claves `NavKey` con `@Serializable`; ViewModels por entrada con `rememberViewModelStoreNavEntryDecorator()`; entradas por feature con `EntryProviderScope<NavKey>`; receta para pasar argumentos a un ViewModel con una fábrica propia, sin Hilt | ADR-30-10 y ADR-30-04 | Sí, con la fila "Navegación" de la constitución. Resuelto en D1 (A) y aplicado en la Spec 00 el 2026-10-11 |
| `adaptive` (2026-09-22) | Pide Navigation 3 como requisito; layouts para tablets, plegables y escritorio | Refuerza D1. No se aplica en la primera versión | No con la arquitectura. La Spec 01 deja fuera las tablets; la skill no aplica hasta que una spec las incluya |
| `navigation-event` (2026-09-01) | Con Navigation 3, usar su manejo del atrás; `NavigationBackHandler` en lugar de `BackHandler`; un manejador por estado | Costura para la Spec 42 | No |
| `edge-to-edge` (2026-08-24) | `enableEdgeToEdge()` antes de `setContent`, `adjustResize`, insets con un solo método | Ya se cumple (S03-R12); costura para las Specs 41 y 42 | No |
| `testing-setup` (2026-09-23) | Si no hay framework de inyección, instalar Hilt; usar fakes de interfaces; probar ViewModels, repositorios y DAOs | Las interfaces del dominio y la inyección manual permiten fakes sin Hilt | Sí, con la inyección manual decidida en la constitución. Resuelto en D4 (A): se mantiene la inyección manual |

## Dependencias

Aprobadas por el dueño con esta spec el 2026-10-11. Cada una entra con la spec de la columna "La agrega". Versiones verificadas el 2026-10-11 en las páginas oficiales de versiones y en Google Maven. Ninguna cambia la línea de Kotlin ni de Compose.

| Artefacto | Versión | Uso | La agrega | Fuente |
| --- | --- | --- | --- | --- |
| `androidx.navigation3:navigation3-runtime` | 1.2.0 (23 de septiembre de 2026) | `implementation` | F00 | [Navigation 3 releases](https://developer.android.com/jetpack/androidx/releases/navigation3) |
| `androidx.navigation3:navigation3-ui` | 1.2.0 | `implementation` | F00 | [Navigation 3 releases](https://developer.android.com/jetpack/androidx/releases/navigation3) |
| `androidx.lifecycle:lifecycle-viewmodel-navigation3` | 2.11.0 (17 de junio de 2026), la misma versión de Lifecycle que ya usa el proyecto | `implementation` | F00 | [Lifecycle releases](https://developer.android.com/jetpack/androidx/releases/lifecycle) |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.11.0 | `implementation`. `viewModel(factory = ...)` (S30-R16) | Primera spec con un ViewModel | [Lifecycle releases](https://developer.android.com/jetpack/androidx/releases/lifecycle), [ViewModel factories](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories) |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.11.0 | `implementation`. `collectAsStateWithLifecycle()` (S30-R30) | Primera spec con una pantalla | [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state) |
| Plugin `org.jetbrains.kotlin.plugin.serialization` | La de Kotlin, 2.4.21, con `version.ref = "kotlin"` | `apply false` en la raíz, aplicado en `:app`. Genera los serializadores de las claves | F00 | [Gradle Plugin Portal](https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization), [Navigation 3 get started](https://developer.android.com/guide/navigation/navigation-3/get-started) |
| `org.jetbrains.kotlinx:kotlinx-serialization-core` | 1.11.0, ya en el catálogo | Pasa de `runtimeOnly` a `implementation`: las claves usan `@Serializable` al compilar | F00 | Spec 20; [kotlinx.serialization releases](https://github.com/Kotlin/kotlinx.serialization/releases) (1.12.0 está en RC) |

Las tres versiones de Lifecycle comparten la referencia de `lifecycle-runtime-ktx` en el catálogo, para que no se desalineen. Al agregarlas, la clave `lifecycleRuntimeKtx` se puede renombrar a `lifecycle`.

No se agregan:

- Hilt, Koin ni otro framework de inyección (ADR-30-04, D4).
- `androidx.navigation:navigation-compose` (Navigation 2): el dueño eligió Navigation 3 en D1 (ADR-30-10).
- `androidx.compose.material3.adaptive:adaptive-navigation3`: no hay layouts adaptables en la primera versión (Spec 01).
- `androidx.navigationevent:navigationevent-compose` como dependencia directa: llega de forma transitiva con `navigation3-ui` (1.1.1). Se declara solo si la Spec 42 usa `NavigationBackHandler`.
- `kotlinx-collections-immutable` (ADR-30-07).
- Las versiones alfa de Lifecycle 2.12 que muestra la página "Get started" de Navigation 3.

## Fuera de alcance de esta spec

- Pantallas, flujos, rutas concretas, diseño visual y textos: Specs 40 a 42 y specs F.
- Estrategia de pruebas, librerías de prueba y ubicación de los fakes: Spec 31.
- Implementación de F00 a F04 y los nombres finales de sus casos de uso, pantallas y claves.
- Modularización: ADR-30-02 fija cuándo reconsiderarla.
- Layouts adaptables, tablets y otros dispositivos (Spec 01).
- Registro de fallos del agente: sigue abierto en la constitución.
- Features de la visión a futuro (rutinas, mediciones corporales, gráficas).

## Criterios de aceptación

S30-A01 a S30-A11 se comprueban sobre el código actual y en cada spec posterior; el 2026-10-11 pasan todos sobre el commit `4ce5ffb` (ver "Verificación"). S30-A12 a S30-A19 y S30-A22 los comprueba cada spec F al construir su corte vertical (S30-R03). S30-A20 y S30-A21 revisan esta spec y sus dependencias. Las rutas parten de `app/src/main/java/com/juancholopes/weightrack/`, abreviado `<pkg>`.

| ID | Criterio | Comprueba | Evidencia |
| --- | --- | --- | --- |
| S30-A01 | El proyecto tiene un solo módulo. | S30-R04 | `grep -n "include(" settings.gradle.kts` devuelve solo `include(":app")` |
| S30-A02 | El dominio no importa Android, AndroidX ni paquetes `data` o `ui` de la app. | S30-R09, S10-R16 | `grep -rnE "^import (android\|androidx\|com\.juancholopes\.weightrack\.(data\|ui\|feature\.[a-z]+\.(data\|ui)))\." <pkg>/feature/*/domain` sin resultados |
| S30-A03 | Ningún paquete de UI importa Room ni paquetes `data`. | S30-R09 | `grep -rnE "^import (androidx\.room3\|com\.juancholopes\.weightrack\.(data\|feature\.[a-z]+\.data))\." <pkg>/ui $(find <pkg>/feature -type d -name ui)` sin resultados |
| S30-A04 | Las dependencias entre features siguen S30-R10. | S30-R10 | `grep -rnE "^import com\.juancholopes\.weightrack\.feature\.(session\|lastperformance\|history)\." <pkg>/feature/catalog` y `grep -rnE "^import com\.juancholopes\.weightrack\.feature\.(lastperformance\|history)\." <pkg>/feature/session` sin resultados; y para cada feature `f`, `grep -rnE "^import com\.juancholopes\.weightrack\.feature\.[a-z]+\.ui\." <pkg>/feature/$f \| grep -v "feature\.$f\.ui"` sin resultados |
| S30-A05 | Solo el `AppContainer` construye la base de datos y las implementaciones de repositorio. | S30-R13 | `grep -rnE "Room[A-Za-z]+Repository\(\|databaseBuilder" app/src/main \| grep -v "class Room"` devuelve solo líneas de `AppContainer.kt` |
| S30-A06 | El contenedor solo se nombra en la raíz y en las fábricas de ViewModel. | S30-R14, S30-R15, S30-R16 | `grep -rlE "AppContainer\|appContainer" app/src/main` devuelve solo `AppContainer.kt`, `WeightrackApplication.kt` y archivos `*ViewModel.kt` |
| S30-A07 | No hay dispatchers escritos a mano, `GlobalScope` ni `runBlocking`. | S30-R33, S30-R34 | `grep -rnE "Dispatchers\.\|GlobalScope\|runBlocking" app/src/main` sin resultados |
| S30-A08 | La fecha de hoy sale solo del reloj del `AppContainer`. | S30-R36, S10-R32 | `grep -rnE "LocalDate\.now\(\)\|Instant\.now\(\)\|System\.currentTimeMillis\|Clock\.system" app/src/main` devuelve como mucho la línea de `Clock.systemDefaultZone()` en `AppContainer.kt` |
| S30-A09 | Hay una sola Activity y ningún Fragment. | S30-R11 | `grep -c "<activity" app/src/main/AndroidManifest.xml` devuelve 1; `grep -rn "androidx.fragment" app/src/main gradle/libs.versions.toml` sin resultados |
| S30-A10 | Ningún `build.gradle.kts` escribe versiones y los plugins se aplican con `alias`. | S30-R41 | `grep -nE '"[0-9]+\.[0-9]+' build.gradle.kts app/build.gradle.kts` sin versiones de dependencias o plugins, y revisión de los bloques `plugins` |
| S30-A11 | Build y pruebas unitarias pasan desde un clon limpio. | S30-R04, S30-R41 | `./gradlew assembleDebug test` |
| S30-A12 | Cada pantalla tiene `<Nombre>Screen`, `<Nombre>Content`, `<Nombre>ViewModel` y `<Nombre>UiState` en `feature/<feature>/ui/`, y `Content` no recibe ViewModel. | S30-R12, S30-R31 | `find <pkg>/feature -path "*/ui/*"` y revisión de cada pantalla |
| S30-A13 | Ningún ViewModel expone estado mutable ni canales de eventos. | S30-R26, S30-R28 | `grep -rnE "Channel<\|SharedFlow<\|MutableSharedFlow\|LiveData" app/src/main` sin resultados; `grep -rnE "^\s*(val\|var) [A-Za-z_]+ *(:\|=) *MutableStateFlow" <pkg>/feature` sin resultados |
| S30-A14 | Las pantallas recolectan el estado con `collectAsStateWithLifecycle()`. | S30-R30 | `grep -rnE "collectAsState\(" app/src/main` sin resultados y `collectAsStateWithLifecycle` en cada `*Screen.kt` |
| S30-A15 | Cada ViewModel tiene al menos una prueba en `src/test` que lo construye con un fake de una interfaz del dominio, sin Room ni emulador, y comprueba el `UiState` después de un evento. | S30-R15, S30-R25 a S30-R27 | Prueba nombrada en la spec F, que corre con `./gradlew test` |
| S30-A16 | Los ViewModels no conocen la navegación. | S30-R39 | `grep -lE "^import androidx\.navigation" $(find app/src/main -name "*ViewModel.kt")` sin resultados |
| S30-A17 | Las claves de navegación viven en `feature/*/ui/`, son `@Serializable` y solo llevan ids o valores simples. | S30-R38, S30-R40 | `grep -rn ": NavKey" app/src/main` solo en rutas `feature/*/ui/`, y revisión de cada clave |
| S30-A18 | La UI no llama directamente a las escrituras validadas de sesiones y series. | S30-R20 | `grep -rnE "\.(createSession\|updateSession\|addSet\|updateSet)\(" <pkg>/feature/*/ui` sin resultados |
| S30-A19 | El dominio no tiene textos de interfaz y cada enum de error que muestra la UI tiene su recurso de strings. | S30-R24 | `grep -rn "R\.string" <pkg>/feature/*/domain` sin resultados, y revisión del mapeo en cada `ui/` |
| S30-A20 | Cada ADR tiene estado, fecha, contexto, decisión, alternativas y consecuencias, y aparece en el índice. | S30-R01 | Revisión de esta spec |
| S30-A21 | Las dependencias agregadas coinciden con "Dependencias" y con D1 (Navigation 3): no conviven `androidx.navigation3` y `androidx.navigation:navigation-compose`. | S30-R37, S30-R42 | `grep -n "navigation" gradle/libs.versions.toml` y `./gradlew :app:dependencies --configuration debugRuntimeClasspath` |
| S30-A22 | En cada spec F, el plan muestra que los paquetes, los casos de uso, los errores, la navegación sin lógica y las escrituras antes de navegar siguen esta spec. | S30-R02, S30-R05 a S30-R08, S30-R17 a S30-R23, S30-R29, S30-R32, S30-R35 | Revisión del plan de la spec F y `find <pkg> -type d` |

## Verificación

Hecha el 2026-10-11 sobre el árbol de trabajo: commit `4ce5ffb` más los cambios de specs de ese día, sin cambios de código. Cada comando se ejecutó tal como está en "Criterios de aceptación", desde la raíz del proyecto. `<pkg>` es `app/src/main/java/com/juancholopes/weightrack`.

| ID | Resultado | Evidencia |
| --- | --- | --- |
| S30-A01 | Pasa | Devuelve solo `26:include(":app")`. |
| S30-A02 | Pasa | Sin resultados. |
| S30-A03 | Pasa | `find <pkg>/feature -type d -name ui` no devuelve nada, porque ninguna feature tiene `ui/` todavía. La búsqueda corre solo sobre `<pkg>/ui` y no da resultados. |
| S30-A04 | Pasa | Las dos primeras búsquedas, sin resultados. La tercera, sin resultados para `catalog` y `session`; `lastperformance` y `history` no existen todavía y se omiten. |
| S30-A05 | Pasa | Tres líneas, todas de `AppContainer.kt`: `Room.databaseBuilder(...)` (línea 21), `RoomExerciseRepository(...)` (27) y `RoomSessionRepository(...)` (31). |
| S30-A06 | Pasa | Solo `AppContainer.kt` y `WeightrackApplication.kt`. Todavía no hay ViewModels. |
| S30-A07 | Pasa | Sin resultados. |
| S30-A08 | Pasa | Sin resultados. El reloj llega con F02. |
| S30-A09 | Pasa | `grep -c` devuelve 1. La búsqueda de `androidx.fragment` no da resultados. |
| S30-A10 | Pasa | Solo `app/build.gradle.kts:19: versionName = "1.0"`, que es la versión de la app, no de una dependencia ni de un plugin. Los bloques `plugins` de la raíz y de `:app` usan solo `alias(libs.plugins...)`, con `apply false` en la raíz. |
| S30-A11 | Pasa | `./gradlew assembleDebug test` termina con `BUILD SUCCESSFUL`. Las pruebas estaban al día, así que se volvieron a correr con `./gradlew :app:testDebugUnitTest --rerun`: 58 pruebas en 9 clases, sin fallos, errores ni omitidas. Se ejecutó en el árbol de trabajo, no en un clon limpio. |
| S30-A20 | Pasa | Los 11 ADR tienen estado, fecha, contexto, decisión, alternativas y consecuencias, y están en el índice. |
| S30-A21 | Pasa | `grep -n "navigation" gradle/libs.versions.toml` no da resultados: las dependencias de navegación entran con F00. En `debugRuntimeClasspath` solo aparece `androidx.navigationevent` 1.0.0, transitiva; no están `androidx.navigation3` ni `navigation-compose`. |
| S30-A12 a S30-A19 y S30-A22 | No aplica todavía | Los comprueba cada spec F al construir su corte vertical (S30-R03). |

## Decisiones abiertas

Todas cerradas por el dueño el 2026-10-11, con la opción recomendada.

- [x] **D1. Librería de navegación.** Decidido el 2026-10-11: A, Navigation 3. A: Navigation 3 1.2.0 con `lifecycle-viewmodel-navigation3` 2.11.0. B: Navigation Compose 2.10.2. Recomendación: A. La pila como lista que posee la app encaja con el flujo unidireccional y con la inyección manual; es lo que piden tres de las skills instaladas (regla 9); Navigation 2 está en modo mantenimiento según su página oficial. El costo es una biblioteca más joven y algo de código propio si la Spec 42 elige varias pilas. Con A se pide cambiar la fila "Navegación" de la constitución.
- [x] **D2. Paquetes de F03 (última vez) y F04 (historial).** Decidido el 2026-10-11: A, paquetes propios. A: paquetes propios `feature/lastperformance` y `feature/history`, solo con `ui/`, usando el dominio de `session`; la pantalla de F02 declara un hueco para la última vez y el shell lo llena con el composable de F03, que tiene su propio ViewModel. B: F03 dentro de `feature/session` (su sección y su campo en el `UiState` del registro) y F04 en `feature/history`; exige enmendar la convención "cada funcionalidad nueva vive en su propio paquete". C: las dos dentro de `feature/session`; misma enmienda y un paquete que mezcla tres features. Recomendación: A. Cumple la convención y el principio 5 sin enmiendas, y quitar F03 o F04 es quitar un paquete. El costo es un parámetro composable en la pantalla de F02 y un ViewModel pequeño para la sección de F03.
- [x] **D3. Paquete del shell de F00.** Decidido el 2026-10-11: A, el paquete raíz `ui/`. A: el paquete raíz `ui/` (composable raíz y navegación), junto a `ui/theme/`, con una aclaración en la constitución de que las piezas compartidas viven fuera de `feature/`. B: `feature/shell/ui`. Recomendación: A. El shell no es una funcionalidad de negocio sino la pieza que une a todas, como `data/` y `ui/theme/`; dentro de `feature/` rompería la regla de que ninguna feature importa la UI de otra (S30-R10).
- [x] **D4. Propuesta de la skill `testing-setup`: instalar Hilt.** Decidido el 2026-10-11: A, se mantiene la inyección manual. A: mantener la inyección manual decidida el 2026-10-10. B: adoptar Hilt, lo que exige enmendar la constitución. Recomendación: A. Con dos repositorios, un reloj y pocos casos de uso, las fábricas de ADR-30-04 bastan; las pruebas construyen los ViewModels con fakes sin ningún framework; Hilt agregaría plugin, procesamiento de anotaciones y tiempo de build sin un problema concreto que resolver.
- [x] **D5. Forma de los eventos de la UI.** Decidido el 2026-10-11: A, funciones `on<Evento>()`. A: funciones públicas `on<Evento>()` en el ViewModel. B: un `sealed interface <Nombre>Action` y una sola función `onAction(action)`. Recomendación: A. Menos tipos (principio 6), cada evento se descubre con autocompletado y se prueba llamando a la función. El costo es que `<Nombre>Content` recibe una lambda por evento; con B recibe una sola.
- [x] **D6. Dónde viven los ADR.** Decidido el 2026-10-11: A, dentro de esta spec. A: dentro de esta spec, como enmiendas (ADR-30-01). B: un archivo por ADR en `.docs/adr/`. Recomendación: A, por las razones de ADR-30-01; se pasa a B con un ADR nuevo si la spec crece demasiado.

## Cambios que esta spec pide a otras specs

Esta spec no edita ninguna. El dueño aprueba y aplica cada cambio. Las filas de las Specs 00, 03, 10 y 20 se aplicaron el 2026-10-11 con la aprobación del dueño. Las de las Specs 31, 42 y F00 a F04 quedan pendientes hasta que esas specs existan.

| Spec | Cambio propuesto | Motivo | Estado |
| --- | --- | --- | --- |
| 00 Constitución | La fila "Navegación" pasa a "Navigation 3 (`androidx.navigation3`), según ADR-30-10. Decidido el 2026-10-11". Anotar la enmienda. | Regla 9: las skills `navigation-3`, `adaptive` y `navigation-event` chocan con "Navigation para Compose". | Aplicado el 2026-10-11 |
| 00 Constitución | La fila "Build" pasa de Propuesto a "Decidido el 2026-10-11", según ADR-30-11. | El proyecto ya usa Kotlin DSL y version catalog desde la Spec 03. | Aplicado el 2026-10-11 |
| 00 Constitución | Aclarar en las convenciones de código que el shell, el tema y la base de datos compartida viven fuera de `feature/` (`ui/`, `ui/theme/`, `data/`). | Evitar que "cada funcionalidad nueva vive en su propio paquete dentro de `feature/`" se lea como que F00 va en `feature/`. | Aplicado el 2026-10-11 |
| 00 Constitución | En la decisión "Inyección de dependencias", donde dice "La Spec 30 lo registra en su ADR", anotar "ADR-30-04". | Trazabilidad. | Aplicado el 2026-10-11 |
| 03 Configuración | Actualizar el esqueleto de paquetes con el árbol de ADR-30-03: nombres de feature, `ui/` del shell y la composición de cada pantalla (`Screen`, `Content`, `ViewModel`, `UiState`). Reemplazar "El nombre exacto de cada feature y el reparto fino entre capas los decide la Spec 30" por una referencia a S30-R05 y S30-R06. | La Spec 03 dejó esas decisiones a esta spec. | Aplicado el 2026-10-11 |
| 10 Modelo de dominio | En "Propósito" y "Fuera de alcance", anotar que el dominio se queda donde está (ADR-30-03), que las interfaces de S20-R42 se mantienen (ADR-30-05) y que el reloj de S10-R32 viene del `AppContainer` (ADR-30-09). La nota sobre `nameKey` queda resuelta: sigue `internal` porque hay un solo módulo (ADR-30-02). | La Spec 10 dejó esas decisiones a esta spec. | Aplicado el 2026-10-11 |
| 20 Persistencia | Marcar como aplicada su fila "30 Arquitectura" de "Cambios que esta spec pide a otras specs": las interfaces se mantienen (ADR-30-05) y la inyección manual queda registrada (ADR-30-04). | Cerrar el pendiente de la Spec 20. | Aplicado el 2026-10-11 |
| 31 Estrategia de pruebas | Recoger las costuras de "Costuras para otras specs": ViewModels y casos de uso con fakes de las interfaces y `Clock.fixed`, `kotlinx-coroutines-test` en `testImplementation` con `Dispatchers.setMain`, pruebas de UI sobre `<Nombre>Content` y pruebas de navegación sobre la pila del shell. Decidir dónde viven los fakes. | S30-A15 necesita la infraestructura de pruebas de la Spec 31. | Pendiente: se aplica al escribir esa spec |
| 42 Pantallas y navegación | Usar claves por feature con solo ids (S30-R38), lambdas de navegación (S30-R39), entradas por feature (S30-R40); decidir una o varias pilas y la pantalla inicial; decidir el hueco de la última vez en la pantalla de F02 (D2); decidir `NavigationBackHandler` para cambios sin guardar y `android:enableOnBackInvokedCallback` para API 33 a 35. | Costuras de navegación de ADR-30-10. | Pendiente: se aplica al escribir esa spec |
| F00 a F04 | Citar los requisitos de esta spec que aplican y presentar la evidencia de S30-A12 a S30-A19 y S30-A22 (S30-R03). F00 agrega las dependencias aprobadas de navegación y Lifecycle; F02 agrega el reloj al `AppContainer` y los casos de uso de sesiones y series. | Aplicar la arquitectura en cada corte vertical. | Pendiente: se aplica al escribir esa spec |

## Fuentes

Consultadas el 2026-10-11:

- [Navigation 3 releases](https://developer.android.com/jetpack/androidx/releases/navigation3): 1.2.0 estable del 23 de septiembre de 2026; 1.3.0-alpha02 del 7 de octubre de 2026; `compileSdk` 37 desde 1.2.0-alpha03.
- [Navigation 3, get started](https://developer.android.com/guide/navigation/navigation-3/get-started): artefactos `navigation3-runtime` y `navigation3-ui`, add-on `lifecycle-viewmodel-navigation3` (mostrado en 2.12.0-alpha04), plugin `org.jetbrains.kotlin.plugin.serialization`, `compileSdk` 36 o más.
- [Navigation 3, save and manage state](https://developer.android.com/guide/navigation/navigation-3/save-state): `rememberNavBackStack` exige `NavKey` y `@Serializable`; `rememberViewModelStoreNavEntryDecorator()` liga cada ViewModel a su entrada.
- Código fuente de AndroidX, `ViewModelStoreNavEntryDecorator.kt` (rama `androidx-main`): la fábrica y los `CreationExtras` por defecto del dueño del `ViewModelStore` pasan a cada entrada.
- Google Maven, metadatos `.module` de `navigation3-ui-android` 1.2.0, `navigation3-runtime-android` 1.2.0 y `lifecycle-viewmodel-navigation3-android` 2.11.0: versiones mínimas transitivas citadas en ADR-30-10.
- [Lifecycle releases](https://developer.android.com/jetpack/androidx/releases/lifecycle): 2.11.0 estable del 17 de junio de 2026, incluido `lifecycle-viewmodel-navigation3`; 2.12.0-alpha04 del 23 de septiembre de 2026.
- [Navigation releases](https://developer.android.com/jetpack/androidx/releases/navigation): 2.10.2 estable del 23 de septiembre de 2026 y el aviso de modo mantenimiento.
- [Gradle Plugin Portal, `org.jetbrains.kotlin.plugin.serialization`](https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization): 2.4.21. Maven Central: `kotlinx-serialization-core` 1.12.0-RC como última publicada (no estable).
- [Create ViewModels with dependencies](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories): `viewModelFactory`, `initializer`, `APPLICATION_KEY` y `viewModel(factory = ...)`.
- [UI events](https://developer.android.com/topic/architecture/ui-layer/events): los eventos del ViewModel se reducen a estado; advertencia sobre `Channel`.
- [UI state production](https://developer.android.com/topic/architecture/ui-layer/state-production): `stateIn` con `WhileSubscribed(5_000)`, `MutableStateFlow.update`, no lanzar coroutines en `init`.
- [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state): `collectAsStateWithLifecycle()` en `lifecycle-runtime-compose` 2.11.0.
- [Best practices for coroutines in Android](https://developer.android.com/kotlin/coroutines/coroutines-best-practices): inyectar dispatchers, funciones `suspend` seguras para el hilo principal, coroutines en `viewModelScope`, no usar `GlobalScope`.
- [Domain layer](https://developer.android.com/topic/architecture/domain-layer): capa opcional, casos de uso para lógica compleja o reutilizada, nombre `<verbo><sustantivo>UseCase`, costo de obligar a pasar siempre por casos de uso.
- [Strong skipping mode](https://developer.android.com/develop/ui/compose/performance/stability/strongskipping): activo por defecto desde Kotlin 2.0.20.
- Skills de `.claude/skills/`: `navigation-3`, `navigation-event`, `adaptive`, `edge-to-edge` y `testing-setup`.
