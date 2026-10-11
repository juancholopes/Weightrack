# Spec F01: Catálogo de ejercicios

Oct 11, 2026 · @Juan

| ID | Estado | Fase | Depende de |
| --- | --- | --- | --- |
| F01 | Aprobada | 4 Features | F00 |

## Propósito

F01 deja al usuario mantener su catálogo de ejercicios: crearlos, editarlos con su nota fija, archivarlos, eliminarlos y desarchivarlos (S01-R01, S01-R10).

Construye dos pantallas: el editor de ejercicio (S42-P03) y la lista de archivados (S42-P04). Agrega al dominio la búsqueda por nombre (S10-R33) y a los datos la lectura "¿tiene series?" (S20-R48).

La vista del catálogo no es de F01: es la pestaña Ejercicios del inicio, que construye F02 (Spec 42, D2). Hasta que F02 exista, F01 se abre desde la pantalla de arranque temporal de F00.

Las decisiones de esta spec quedan tomadas el 2026-10-11: el dueño aceptó de antemano las recomendaciones y pidió un MVP funcional (0.0.1 = F00 a F03) hoy.

## Alcance

| Fuente | Qué implementa F01 |
| --- | --- |
| Flujos (Spec 40) | S40-F04, S40-F05, S40-F06, S40-F07, el editor de S40-F11 y su parte de S40-F21. Pasos 2 y 3 de S40-F22 |
| Pantallas (Spec 42) | S42-P03 (editor) y S42-P04 (archivados), con sus claves `ExerciseEditorKey` y `ArchivedExercisesKey` |
| Requisitos S40 | S40-R04 a S40-R08 en el editor, S40-R15 a S40-R20, S40-R40 a S40-R50 en sus pantallas |
| Requisitos S42 | S42-R06 y S42-R21 en P03; S42-R07; la parte del editor de S42-R08 y S42-R09; S42-R10 y S42-R25 en sus pantallas |
| Dominio (Spec 10) | Usa S10-R04 a S10-R08, S10-R12, S10-R15, S10-R17 a S10-R22 y S10-R31. Implementa S10-R33 |
| Datos (Spec 20) | Usa S20-R38, S20-R40 y S20-R41. Implementa S20-R48 |
| Arquitectura (Spec 30) | S30-R12, S30-R15 a S30-R18, S30-R22 a S30-R31, S30-R35, S30-R38 a S30-R40 |
| Diseño (Spec 41) | Construye `EmptyState` y `ConfirmDialog` en `ui/theme/component/` (tabla "Componentes compartidos"). S41-R11 a S41-R16 |

## Requisitos

Cada requisito remite a los de otras specs en lugar de repetirlos.

| ID | Requisito |
| --- | --- |
| SF01-R01 | El editor vive en `feature/catalog/ui/editor/` y la lista de archivados en `feature/catalog/ui/archived/`, cada uno con `Screen`, `Content`, `ViewModel` y `UiState` (S30-R12). Las claves son las del bloque "Claves" de la Spec 42 y viven en `feature/catalog/ui/` (S42-R07). F01 expone sus entradas con `EntryProviderScope<NavKey>.catalogEntries(...)` (S30-R40). |
| SF01-R02 | **Crear.** El editor pide nombre, equipo (un valor), músculos (varios, sin repetir) con su marca "Principal" y nota opcional (S40-F04, S40-R15). El primer músculo elegido empieza como principal (S40-R16). Con `prefillName`, el nombre empieza con ese texto. Al tocar "Guardar ejercicio" valida con `validateExercise` y guarda con `create` (S40-R05). |
| SF01-R03 | **Editar.** Con `exerciseId` distinto de `0L`, el editor parte de `observeExercise` y `observeMuscles`. Guardar usa `update` y conserva el `id`, las series y el estado de archivado (S10-R22, S40-R18). |
| SF01-R04 | **Errores.** Se muestra un solo error, el que devuelve el dominio, junto a su campo o debajo de su grupo (D18 de la Spec 40). No se escribe nada y el formulario conserva lo escrito. Tras un intento fallido, el error se actualiza mientras el usuario corrige y el foco va al campo con error (S40-R05, S40-R47). Cada `ExerciseError` tiene su texto en `strings.xml` (S30-R24). |
| SF01-R05 | **Nombre ocupado.** Por un activo: error junto al nombre. Por un archivado al crear: diálogo «{nombre}» está archivado con "Desarchivar" y "Usar otro nombre" (D9 de la Spec 40). "Desarchivar" llama a `unarchive(id)` y termina como un guardado con ese `id`. Al editar: solo el error, sin oferta (S40-R17). |
| SF01-R06 | **Archivar o eliminar.** El editor de un ejercicio existente muestra una sola acción según `observeHasSets` (SF01-R11): "Archivar ejercicio", que actúa al momento, o "Eliminar ejercicio", que pide confirmar con `ConfirmDialog` (D8 de la Spec 40). Las dos llaman a `delete(exerciseId)`, y el `ExerciseDeletion` que devuelve decide el resultado (S20-R40). Mientras `hasSets` es `null`, no se muestra ninguna de las dos. |
| SF01-R07 | **Resultados.** `savedExerciseId`, `archivedResult` y `deletedResult` son efectos del `UiState` (S30-R28). La pantalla llama a `onSaved(id)`, `onArchived(...)` u `onDeleted(...)` cuando la escritura terminó (S30-R35) y luego a `on…Handled()`. El shell aplica S42-R08 y S42-R09: saca el editor y guarda un `PendingNotice`. |
| SF01-R08 | **Cambios sin guardar.** `hasUnsavedChanges` es `true` cuando los valores difieren de los iniciales; el nombre de `prefillName` cuenta como inicial (S40-R40). El editor tiene un solo `NavigationBackHandler`, activo solo con cambios, que abre "¿Descartar los cambios?" (S42-R06). El botón cerrar hace lo mismo. |
| SF01-R09 | **Escrituras seguras.** Mientras guarda, `isSaving` impide otro guardado: un doble toque crea un solo ejercicio (S40-R07). Una excepción inesperada pone `genericError`, conserva lo escrito y deja reintentar (S30-R23, S40-R06). |
| SF01-R10 | **Archivados.** La lista sale de `observeArchived` (S10-R31), con "Desarchivar" en cada fila. Desarchivar actúa al momento y muestra "«{nombre}» volvió al catálogo" en el `SnackbarHost` de esta pantalla (S40-R20). Vacía, muestra el texto de la Spec 42 con `EmptyState`, y nunca antes del primer dato (S42-R25). |
| SF01-R11 | **¿Tiene series?** F01 agrega `observeHasSets(exerciseId: Long): Flow<Boolean>` a `ExerciseRepository` como contrato explícito (S30-R17), con su consulta en `ExerciseDao`, su implementación en `RoomExerciseRepository` y en `FakeExerciseRepository`, y su prueba en `ExerciseRepositoryContract` (S20-R48, S31-R08). |
| SF01-R12 | **Búsqueda por nombre.** F01 agrega en `feature/catalog/domain/ExerciseSearch.kt` dos funciones puras: `exerciseSearchKey(text: String): String` y `matchesExerciseSearch(name: String, query: String): Boolean`, con la regla exacta de S10-R33. F01 no tiene pantalla que la use; la usa la pestaña Ejercicios de F02 (D1). |
| SF01-R13 | **Textos.** Los textos visibles van en `res/values/strings.xml`, en español (constitución). F01 agrega las etiquetas de los 5 `Equipment` y los 14 `Muscle` (Spec 10, "Sin textos de interfaz"), los textos de S40 y S42 para sus pantallas y los de `ExerciseError`. El mapeo de enum a recurso vive en `feature/catalog/ui/CatalogLabels.kt` (D3). |
| SF01-R14 | **Componentes y patrones.** F01 construye `EmptyState` y `ConfirmDialog` en `ui/theme/component/`, sin estado, sin importar `feature` y con preview `@ThemePreviews` (S41-R12, S41-R16). Diálogos, avisos y errores siguen "Patrones de la Spec 40" de la Spec 41 (S41-R13). |
| SF01-R15 | **Accesibilidad e insets.** La marca "Principal" se lee como texto y los errores tienen semántica de error y región en vivo (S40-R49). Las áreas táctiles miden 48 dp o más (S40-R46). El editor aplica el teclado con un solo método, `Modifier.fitInside(WindowInsetsRulers.Ime.current)` (skill `edge-to-edge`), y "Guardar ejercicio" se ve con el teclado abierto. |
| SF01-R16 | **Fallo provocado.** `FakeExerciseRepository` agrega una forma explícita de lanzar una excepción en la siguiente escritura, con su prueba, para probar SF01-R09 (S31-R08). |

## Plan

**Capas.** Dominio: una función pura nueva y un método nuevo en la interfaz. Datos: una consulta y su implementación. UI: dos pantallas, sus claves y dos componentes compartidos. Shell: registrar las entradas y aplicar los resultados del editor. No hay casos de uso: cada operación la usa una sola pantalla (S30-R18, S30-R19).

**Archivos.** Rutas relativas a `com/juancholopes/weightrack/`.

| Paquete | Archivo | Qué cambia |
| --- | --- | --- |
| `feature/catalog/domain/` | `ExerciseSearch.kt` | Nuevo: `exerciseSearchKey` y `matchesExerciseSearch` (SF01-R12) |
| `feature/catalog/domain/` | `ExerciseRepository.kt` | `fun observeHasSets(exerciseId: Long): Flow<Boolean>` (SF01-R11) |
| `feature/catalog/data/` | `ExerciseDao.kt`, `RoomExerciseRepository.kt` | Consulta guía de S20-R48 y su implementación |
| `feature/catalog/ui/` | `CatalogKeys.kt`, `CatalogEntries.kt`, `CatalogLabels.kt` | Claves, `catalogEntries(...)` y mapeo de enums a strings |
| `feature/catalog/ui/editor/` | `ExerciseEditorScreen.kt`, `ExerciseEditorContent.kt`, `ExerciseEditorViewModel.kt`, `ExerciseEditorUiState.kt` | S42-P03 |
| `feature/catalog/ui/archived/` | `ArchivedExercisesScreen.kt`, `ArchivedExercisesContent.kt`, `ArchivedExercisesViewModel.kt`, `ArchivedExercisesUiState.kt` | S42-P04 |
| `ui/` | `PendingNotice.kt`, `EditorResults.kt`, `CatalogDestinations.kt`, `WeightrackApp.kt`, `StartScreen.kt` | Aviso del shell (D6 de la Spec 42); funciones puras que aplican un resultado del editor a la pila; el puente `catalogDestinations(backStack, ...)` de SF00-R08; una línea en `WeightrackApp`; las lambdas de `startDestination` |
| `ui/theme/component/` | `EmptyState.kt`, `ConfirmDialog.kt` | Componentes de la Spec 41 |
| `res/values/` | `strings.xml` | Textos de SF01-R13 |
| `testFixtures/.../testing/` | `FakeExerciseRepository.kt`, `ExerciseRepositoryContract.kt` | `observeHasSets`, fallo provocado y sus pruebas |

**Estado del editor.** Guía; los nombres finales salen de la implementación.

```kotlin
data class ExerciseEditorUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val equipment: Equipment? = null,
    val muscles: List<MuscleChoice> = emptyList(), // en el orden en que se eligieron
    val notes: String = "",
    val hasSets: Boolean? = null,                  // null: nuevo o cargando
    val fieldError: ExerciseError? = null,
    val archivedConflict: ArchivedConflict? = null, // id y nombre del archivado
    val confirmDialog: EditorDialog? = null,        // DELETE o DISCARD
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    val savedExerciseId: Long? = null,              // efecto
    val archivedResult: ArchivedResult? = null,     // efecto: id, nombre, wasDeleteRequest
    val deletedResult: String? = null,              // efecto: nombre
    val genericError: Boolean = false,              // efecto
)
data class MuscleChoice(val muscle: Muscle, val isPrimary: Boolean)
```

**ViewModel del editor.** Recibe `exerciseId`, `prefillName` y `ExerciseRepository` (S30-R15). En modo edición combina `observeExercise`, `observeMuscles` y `observeHasSets` con un `MutableStateFlow` privado del formulario; en modo nuevo no lee nada hasta guardar. Expone el resultado con `stateIn(..., WhileSubscribed(5_000), ...)`. No lanza coroutines en `init` (S30-R26). El formulario toma los valores de la base de datos hasta la primera edición. Al guardar, lee una vez `observeActiveCatalog()` y `observeArchived()` para armar `existing` (D4); el índice único de la Spec 20 sigue como segunda barrera (S20-R41).

**Archivar o eliminar en carrera.** Si `hasSets` decía `false` pero el ejercicio ganó series antes de confirmar, `delete` devuelve `Archive`. El editor pone `archivedResult` con `wasDeleteRequest = true` y el aviso dice "«{nombre}» tiene series, así que se archivó" (S40-F06).

**Shell.** F01 crea `ui/PendingNotice.kt` con el código de la Spec 42 y su estado `rememberSaveable` en el shell. Las reglas de pila de S42-R08 y S42-R09 son funciones puras de `ui/EditorResults.kt` sobre una `MutableList<NavKey>`, para probarlas en la JVM sin escribir datos (S31-R18). Al recibir un resultado del editor, el shell saca el editor de la pila. Con `onArchived` u `onDeleted`, saca también lo que haya sobre la raíz y guarda el aviso. Hasta F02, la raíz es `StartKey` y el aviso queda guardado sin mostrarse (D2). `logAfterSave` se respeta desde F02, cuando exista `SetLoggerKey`. F01 sigue "Cómo crece el shell" de F00: agrega el puente `ui/CatalogDestinations.kt`, que llama a `catalogEntries(...)` y traduce cada lambda a una operación sobre la pila, y una línea en `WeightrackApp`. Además pasa a `startDestination` las lambdas `onCreateExercise` y `onOpenArchived` de `StartScreen` (SF00-R07).

**Riesgos.**

- La búsqueda no tiene pantalla en F01. Se prueba solo en la JVM hasta F02.
- Hasta F02 no hay forma de llegar al editor en modo edición. Se prueba con el ViewModel y con `Content`; la navegación llega con el menú de la tarjeta (F02).
- La Spec 41 todavía dice que F00 construye `PillTabs` y F01 `ExerciseCard`. La Spec 42 los pasa a F02. F01 no construye ninguno de los dos.
- S31-R18 prohíbe que una prueba abra `weightrack.db`. Por eso la prueba instrumentada de navegación solo recorre caminos que no leen datos: el editor nuevo no consulta nada hasta guardar. Lo demás de la pila se prueba con `EditorResults.kt` en la JVM y, si hace falta, con `WeightrackNavDisplay` y claves de prueba (SF00-R09).

## Tareas

Cada tarea sigue S31-R22: prueba primero, verla fallar, código mínimo, refactorizar. El orden va del dominio a la navegación.

| # | Tarea | Criterio |
| --- | --- | --- |
| T01 | Escribir `ExerciseSearchTest` con los casos de SF01-A01; implementar `ExerciseSearch.kt`. | SF01-A01 |
| T02 | Agregar el caso de `observeHasSets` a `ExerciseRepositoryContract` y verlo fallar en el fake; implementar en `ExerciseRepository` y `FakeExerciseRepository`. | SF01-A02 |
| T03 | Escribir la prueba de DAO; implementar la consulta en `ExerciseDao` y `RoomExerciseRepository`; correr el contrato contra Room. | SF01-A02 |
| T04 | Agregar al fake el fallo provocado, con su prueba en `FakeExerciseRepositoryTest`. | SF01-A09 |
| T05 | `ExerciseEditorViewModelTest` para crear, validar y nombre ocupado; implementar `ExerciseEditorUiState` y `ExerciseEditorViewModel` con su fábrica (S30-R16). | SF01-A03 a SF01-A06 |
| T06 | Pruebas de editar, cambios sin guardar, archivar o eliminar y fallos; completar el ViewModel. | SF01-A07 a SF01-A09 |
| T07 | `ArchivedExercisesViewModelTest`; implementar su `UiState` y su ViewModel. | SF01-A10 |
| T08 | Agregar a `strings.xml` las etiquetas y los textos; escribir `CatalogLabels.kt`. Construir `EmptyState` y `ConfirmDialog` con sus previews. | SF01-A16, SF01-A15 |
| T09 | `ExerciseEditorContentTest`; implementar `ExerciseEditorContent` y `ExerciseEditorScreen`, con el `NavigationBackHandler` y el manejo del teclado. | SF01-A11, SF01-A12 |
| T10 | `ArchivedExercisesContentTest`; implementar su `Content` y su `Screen`. | SF01-A13 |
| T11 | `EditorResultsTest`; implementar `EditorResults.kt` y `PendingNotice.kt`. Luego `CatalogNavigationTest`; implementar `CatalogKeys.kt`, `catalogEntries(...)`, el puente `CatalogDestinations.kt`, su línea en `WeightrackApp` y las lambdas de `startDestination`. | SF01-A14 |
| T12 | Correr las búsquedas de arquitectura y el comando completo; llenar "Verificación". | SF01-A15, SF01-A17 |

## Criterios de aceptación

Las pruebas JVM corren con `./gradlew test` y las instrumentadas con `./gradlew connectedDebugAndroidTest` (S31-R23, S31-R24). Cada prueba lleva el comentario del criterio (S31-R21). Los nombres de prueba son una guía.

| ID | Criterio | Comprueba | Evidencia |
| --- | --- | --- | --- |
| SF01-A01 | "PRESS", "press  de" y "prés" encuentran "Press de banca"; "pena" y "peña" encuentran "Remo de peña"; "elevacion" encuentra "Elevación lateral"; un texto vacío o solo con espacios encuentra todos. | SF01-R12, S10-R33 (S10-A23, parte de S40-A11) | `ExerciseSearchTest` (JVM) |
| SF01-A02 | `observeHasSets` emite `false` sin series, `true` tras agregar una y `false` tras borrarla, en el fake y en Room. | SF01-R11, S20-R48 (S20-A41) | `ExerciseRepositoryContract.observeHasSets_followsTheSetsOfTheExercise`, corrida por `FakeExerciseRepositoryTest` y `RoomExerciseRepositoryContractTest`; prueba en `ExerciseDaoTest` |
| SF01-A03 | Crear con "  Press   de  banca ", barra y pecho guarda "Press de banca" y pone `savedExerciseId`. Sin equipo, sin músculos o con nombre vacío muestra un solo error en su campo y no escribe nada. | SF01-R02, SF01-R04 | `ExerciseEditorViewModelTest` (JVM, con fake) |
| SF01-A04 | El primer músculo elegido queda como principal y un músculo no se puede elegir dos veces. | SF01-R02, S40-R16 (S40-A13) | `ExerciseEditorViewModelTest` |
| SF01-A05 | Guardar con el nombre de un activo muestra el error junto al nombre y no escribe nada. | SF01-R05 (S40-A14) | `ExerciseEditorViewModelTest` |
| SF01-A06 | Al crear con el nombre de un archivado aparece el diálogo. "Desarchivar" deja el archivado activo y sin cambios, y pone `savedExerciseId` con su `id`. "Usar otro nombre" cierra el diálogo y conserva lo escrito. Al editar, solo aparece el error. | SF01-R05 (S40-A15) | `ExerciseEditorViewModelTest` |
| SF01-A07 | Con series, "Archivar ejercicio" archiva sin diálogo y pone `archivedResult`. Sin series, "Eliminar ejercicio" abre el diálogo, no escribe hasta confirmar y luego pone `deletedResult`. Si el ejercicio ganó series antes de confirmar, queda archivado con `wasDeleteRequest = true`. | SF01-R06, SF01-R07 (parte de S40-A16) | `ExerciseEditorViewModelTest` |
| SF01-A08 | En modo edición, el formulario carga los valores guardados sin cambios pendientes; guardar conserva el `id` y el estado de archivado. Con `prefillName` y sin tocar nada, no hay cambios pendientes. | SF01-R03, SF01-R08 | `ExerciseEditorViewModelTest` |
| SF01-A09 | Si el fake lanza una excepción al guardar, aparece `genericError`, el formulario conserva lo escrito y un segundo intento guarda. Dos `onSaveClick()` seguidos crean un solo ejercicio. | SF01-R09, SF01-R16 (S40-A38) | `ExerciseEditorViewModelTest` y la prueba del fallo en `FakeExerciseRepositoryTest` |
| SF01-A10 | Archivados lista por nombre; "Desarchivar" saca la fila, el ejercicio vuelve a `observeActiveCatalog` con sus series y aparece el aviso. | SF01-R10 (S40-A17) | `ArchivedExercisesViewModelTest` (JVM, con fakes que comparten `InMemoryStore`) |
| SF01-A11 | En `ExerciseEditorContent`, la marca "Principal" se lee como texto, el error del nombre se expone como error de semántica, la acción dice "Archivar ejercicio" o "Eliminar ejercicio" según `hasSets`, y "Guardar ejercicio" se ve con la fuente al 200 %. | SF01-R04, SF01-R06, SF01-R15 (parte de S40-A40) | `ExerciseEditorContentTest` (instrumentada) |
| SF01-A12 | Con cambios, atrás muestra "¿Descartar los cambios?"; "Seguir editando" conserva lo escrito y "Descartar" llama a cerrar sin escribir. Sin cambios, atrás cierra sin diálogo. El archivo tiene un solo `NavigationBackHandler(`. | SF01-R08 (S40-A36, S40-A37, parte de S42-A05) | `ExerciseEditorContentTest` y `grep -c "NavigationBackHandler(" ExerciseEditorContent.kt` devuelve 1 |
| SF01-A13 | Archivados no muestra su estado vacío mientras carga; vacía, muestra su texto; tras desarchivar, muestra el aviso. | SF01-R10 (parte de S42-A21) | `ArchivedExercisesContentTest` (instrumentada) |
| SF01-A14 | Las funciones de resultado del shell dejan la pila así: guardar desde `[StartKey, ExerciseEditorKey()]` da `[StartKey]`; archivar o eliminar desde ahí da `[StartKey]` y un `PendingNotice` con el nombre. En la app, "Crear ejercicio" de la pantalla de arranque abre el editor, tras `ActivityScenario.recreate()` la pila sigue igual y atrás vuelve a la pantalla de arranque. | SF01-R07 (parte de S42-A06 y S42-A07) | `EditorResultsTest` (JVM) y `CatalogNavigationTest` (instrumentada, sin escribir datos, S31-R18) |
| SF01-A15 | Las reglas de arquitectura se cumplen en `catalog`: S30-A03, S30-A04, S30-A12 a S30-A17 y S30-A19, y S41-A01 y S41-A10 en los archivos nuevos. | S30-R03, S41-R01, S41-R16 | Las búsquedas de esos criterios, anotadas en "Verificación" |
| SF01-A16 | Ningún texto de la interfaz está escrito en el código y los textos usan el glosario. | SF01-R13 (S40-A41) | `grep -rnE 'Text\("' app/src/main/java/.../feature/catalog/ui` sin resultados y la búsqueda de S02-A04 en `strings.xml` |
| SF01-A17 | El build y todas las pruebas pasan desde cero. | Definición de hecho | `./gradlew assembleDebug test connectedDebugAndroidTest` (S31-R24) |

El "Deshacer" del aviso de archivar lo prueba F02 en el inicio (S42-A07). La búsqueda en pantalla y sus filtros los prueba F02 (S40-A11, S40-A12). S40-A10 todavía nombra a F01, pero el catálogo es la pestaña Ejercicios: lo aporta F02 (SF02-A05, "Cambios" de F02).

## Decisiones

- [x] **D1. Dónde se implementa la búsqueda.** Decidido el 2026-10-11 (el dueño aceptó de antemano las recomendaciones): A, en F01. A: F01 escribe y prueba la función en `catalog.domain` y F02 solo la usa. B: F02 la escribe junto con la pestaña Ejercicios, como dice hoy S10-R33. Por qué A: la función es del catálogo, la Spec 40 la pidió "porque la usan F01 y F02", y F02 ya es la spec más grande. Costo: una función sin pantalla que la use hasta F02, que llega el mismo día.
- [x] **D2. Aviso antes de que exista el inicio.** Decidido el 2026-10-11 (el dueño aceptó de antemano las recomendaciones): A, el shell guarda el aviso y lo muestra F02. A: el `PendingNotice` queda guardado y el inicio de F02 lo consume. B: la pantalla de arranque lo muestra sin "Deshacer". Por qué A: no se escribe código que se borra el mismo día, y el MVP se entrega con F02. Costo: entre F01 y F02 el aviso no se ve; Archivados sigue siendo la otra vía (S40-R44).
- [x] **D3. Etiquetas de los enums.** Decidido el 2026-10-11 (el dueño aceptó de antemano las recomendaciones): A, un mapeo por feature sobre los mismos recursos. A: los textos viven una sola vez en `strings.xml`; `catalog/ui` y luego `session/ui` tienen cada uno su `when` de enum a recurso. B: un mapeo compartido en `ui/theme/`. Por qué A: lo pide S30-R24, y S30-R10 no deja a `session` importar `catalog.ui`; `ui/theme/` no conoce el dominio. Costo: dos `when` cortos con los mismos recursos.
- [x] **D4. Lista `existing` para validar.** Decidido el 2026-10-11 (el dueño aceptó de antemano las recomendaciones): A, unir activos y archivados al guardar. A: leer una vez `observeActiveCatalog()` y `observeArchived()`. B: agregar una lectura de todos los ejercicios, que ADR-30-05 permite. Por qué A: no agrega contrato y el índice único cubre una lista desactualizada (S20-R41). Costo: dos lecturas por guardado, en un catálogo de decenas de filas.

## Fuera de alcance

- La vista del catálogo, sus tarjetas, la búsqueda en pantalla y los filtros: pestaña Ejercicios de F02.
- `PillTabs`, `ExerciseCard`, `WeightNumber` y `Formats.kt`: F02.
- "Deshacer" en el inicio y abrir el registro tras guardar con `logAfterSave = true`: F02.
- "Ver estadísticas": F05.
- Rutinas, mediciones corporales y todo lo de "Fuera de alcance" de la Spec 40 (S40-R51).

**Entrega a F02.** F01 le deja el editor y Archivados listos para conectar desde el inicio, `observeHasSets`, `matchesExerciseSearch`, `EmptyState`, `ConfirmDialog`, `PendingNotice` en el shell y las etiquetas de equipo y músculo en `strings.xml`.

## Cambios que esta spec pide a otras specs

Esta spec no edita ninguna. El dueño aprueba y aplica cada cambio.

| Spec | Cambio | Motivo |
| --- | --- | --- |
| 10 Modelo de dominio | En S10-R33 y S10-A23, decir que la búsqueda se implementa en F01 y la usa F02. | D1. |
| 41 Sistema de diseño | En "Componentes compartidos", confirmar que F01 construye `EmptyState` y `ConfirmDialog`, y pasar `PillTabs` y `ExerciseCard` a F02 (fila "41" de la Spec 42). | Coherencia con la Spec 42, D2. |

## Verificación

Se llena al implementar, según S31-R25: comandos ejecutados, resultado y conteo de pruebas por tarea de Gradle.

## Fuentes

Consultadas el 2026-10-11:

- Specs 00 (ciclo de trabajo y definición de hecho), 01, 02, 10 (S10-R04 a S10-R08, S10-R12, S10-R15, S10-R17 a S10-R22, S10-R31, S10-R33), 20 (S20-R38, S20-R40, S20-R41, S20-R48), 30 (ADR-30-03 a ADR-30-07, ADR-30-10), 31 (S31-R04 a S31-R25), 40 (S40-F04 a S40-F07, S40-F11, S40-F21, D8, D9, D12, D18), 41 ("Componentes compartidos", "Patrones de la Spec 40") y 42 (S42-P03, S42-P04, "Claves", D5, D6, D9).
- Código: `feature/catalog/domain/` (`ExerciseRules.kt`, `ExerciseCatalog.kt`, `ExerciseRepository.kt`), `feature/catalog/data/ExerciseDao.kt`, `testFixtures/.../FakeExerciseRepository.kt` e `InMemoryStore.kt`.
- Skills en `.claude/skills/`: `navigation-3` (ViewModel por entrada), `navigation-event` (`NavigationBackHandler`, un manejador por estado, diálogos con su propio despachador) y `edge-to-edge` (`Scaffold` por pantalla, teclado con `fitInside`).
