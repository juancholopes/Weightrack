# Spec F02: Registrar sesión y series

Oct 11, 2026 · @Juan

| ID | Estado | Fase | Depende de |
| --- | --- | --- | --- |
| F02 | Aprobada | 4 Features | F01 |

## Propósito

F02 deja al usuario registrar su entrenamiento: elegir un ejercicio desde el inicio, anotar sus series de peso y repeticiones, y cambiar la fecha y la nota de la sesión (S01-R02, S01-R10).

Construye tres pantallas: el inicio con las pestañas Hoy y Ejercicios (S42-P01), el registro de series (S42-P02) y los datos de la sesión (S42-P05). El inicio reemplaza la pantalla de arranque temporal de F00. La pestaña Ejercicios es la vista del catálogo (Spec 42, D2).

Las decisiones de esta spec quedan tomadas el 2026-10-11: el dueño aceptó de antemano las recomendaciones y pidió un MVP funcional (0.0.1 = F00 a F03) hoy.

## Alcance

| Fuente | Qué implementa F02 |
| --- | --- |
| Flujos (Spec 40) | S40-F01, S40-F03, S40-F08 a S40-F15, S40-F23, su parte de S40-F21 y el destino de S40-F19 (el registro con `sessionId`, sin entrada hasta F04). Pasos 1, 4 y 5 de S40-F22 |
| Pantallas (Spec 42) | S42-P01 (`HomeKey`), S42-P02 (`SetLoggerKey`) y S42-P05 (`SessionEditorKey`) |
| Requisitos S40 | S40-R01 a S40-R14, S40-R21 a S40-R32 y S40-R40 a S40-R50 en sus pantallas |
| Requisitos S42 | S42-R02 a S42-R04 y S42-R06 en el inicio; S42-R08 a S42-R21, S42-R24 a S42-R26 y S42-R28 |
| Dominio (Spec 10) | Usa S10-R01 a S10-R03, S10-R09, S10-R11 a S10-R15, S10-R18, S10-R25, S10-R26, S10-R28 a S10-R30, S10-R32 y S10-R33. F02 usa el concepto de S10-R34 para el número de la tarjeta |
| Datos (Spec 20) | Usa S20-R29, S20-R31 a S20-R33 y S20-R39. Implementa S20-R49 y una lectura más de músculos (SF02-R18) |
| Arquitectura (Spec 30) | S30-R12 a S30-R40, en especial S30-R20 (casos de uso), S30-R35 y S30-R36 (`Clock`) |
| Diseño (Spec 41) | Construye `PillTabs`, `ExerciseCard` y `WeightNumber` en `ui/theme/component/` y `Formats.kt` en `ui/theme/` |

## Requisitos

| ID | Requisito |
| --- | --- |
| SF02-R01 | **Pantallas y claves.** El inicio vive en `feature/session/ui/home/`, el registro en `feature/session/ui/logger/` y los datos de la sesión en `feature/session/ui/sessioneditor/`, cada uno con `Screen`, `Content`, `ViewModel` y `UiState` (S30-R12). Las claves `HomeKey`, `SetLoggerKey` y `SessionEditorKey` son las de la Spec 42 y viven en `feature/session/ui/` (S42-R07). F02 expone `EntryProviderScope<NavKey>.sessionEntries(...)` (S30-R40). |
| SF02-R02 | **Shell.** Sigue "Cómo crece el shell" de F00: agrega el puente `ui/SessionDestinations.kt` y su línea en `WeightrackApp`. La pila nace con `rememberNavBackStack(HomeKey)` (S42-R02) y F02 borra `ui/StartScreen.kt`, con `StartKey` y `startDestination`. Al guardar en el editor con `logAfterSave = true`, el shell agrega `SetLoggerKey(exerciseId)` (S42-R08). Archivar o eliminar desde el editor saca el editor y el registro de ese ejercicio (S42-R09). El shell pasa el `PendingNotice` al inicio. Hasta F03, el hueco de la última vez se llena con un composable vacío; hasta F05 y F04, las lambdas de estadísticas e historial son `null` (S42-R24). |
| SF02-R03 | **Reloj.** El `AppContainer` expone `val clock: Clock = Clock.systemDefaultZone()` (ADR-30-09, S30-R36). Todo "hoy" sale de `LocalDate.now(clock)` en ViewModels y casos de uso. |
| SF02-R04 | **Casos de uso.** En `feature/session/domain/` viven `CreateSessionUseCase`, `UpdateSessionUseCase`, `AddSetUseCase` y `CorrectSetUseCase` (S30-R19, S30-R20). Validan con `validateSession`, `validateNewSet` o `validateSetCorrection` antes de escribir y devuelven un resultado sellado con el enum de error del dominio (S30-R22). El `AppContainer` los construye (S30-R13). Borrar una serie va directo a `deleteSet` (ADR-30-05). |
| SF02-R05 | **Primera serie crea la sesión.** `AddSetUseCase` recibe el `sessionId` resuelto o `null`. Con `null`, valida una sesión nueva con la fecha de apertura y la serie, y escribe las dos con `createSession(session, listOf(set))` (S20-R39, D3 de la Spec 40). Devuelve el `id` de la sesión. |
| SF02-R06 | **Hoy.** Sigue "Pestaña Hoy" de la Spec 42 y S42-R13. La sesión de hoy es la primera de `observeHistory()` si su fecha es hoy (S10-R28). Sus tarjetas van en el orden de la primera serie de cada ejercicio (D16 de la Spec 40). "De tu última sesión" son los ejercicios activos de la sesión más reciente anterior a hoy sin series hoy. Sin sesión de hoy, muestra el texto vacío de la Spec 42. "Editar sesión" abre `SessionEditorKey`. |
| SF02-R07 | **Menú del inicio.** "Archivados" abre `ArchivedExercisesKey`. Con sesión de hoy, "Empezar otra sesión" crea al momento una sesión de hoy con `CreateSessionUseCase` y Hoy pasa a mostrarla (S40-F09, S40-R22). Sin sesión de hoy, no se ofrece. |
| SF02-R08 | **Ejercicios.** Sigue "Pestaña Ejercicios" de la Spec 42. Lista `observeActiveCatalog(muscle, equipment)` en una `LazyVerticalGrid` adaptable, filtrada por `matchesExerciseSearch` de F01 (S40-R13, S10-R33). Los filtros son dos `FilterChip` con menú: "Equipo" y "Músculo". Sin resultados: "Crear «{texto}»" y "Limpiar búsqueda" (S40-R14). Catálogo vacío: `EmptyState` con "Crear ejercicio" (S40-R12). |
| SF02-R09 | **Tarjeta.** `ExerciseCard` muestra lo que pide S42-R12. El número es el `heaviestWeightGrams` de SF02-R17 con `NumberStyles.card`; el pie es la fecha de esa sesión o, en Hoy, "{n} series hoy". El subtítulo usa el primer músculo principal en el orden del enum `Muscle` (D2). Tocarla abre `SetLoggerKey(exerciseId)` sin escribir nada (S42-R11). El menú tiene "Editar ejercicio" y, solo con una lambda no nula, "Ver estadísticas". Es un solo nodo de TalkBack con acciones de accesibilidad. |
| SF02-R10 | **Pestañas y atrás.** `PillTabs` cambia `selectedTab` en el `UiState`, con Hoy por defecto (S42-R03). Un solo `NavigationBackHandler`, activo solo en Ejercicios, vuelve a Hoy (S42-R06, D10 de la Spec 42). |
| SF02-R11 | **Crear y avisos.** El botón flotante abre el editor según la tabla "Crear ejercicio" de la Spec 42 (S42-R14). El inicio recibe el `PendingNotice` con `onNotice`, lo pone en su `UiState` y lo muestra en su `SnackbarHost`. "Deshacer" de un archivado llama a `unarchive` (S42-R09, S40-R42). |
| SF02-R12 | **Sesión del registro.** Sigue "Sesión" de la Spec 42 y S42-R11. La fecha de apertura sale del reloj al crear el ViewModel y no cambia aunque pase la medianoche. Con `sessionId = 0L`, la sesión es la más reciente con esa fecha; si no hay, `resolvedSessionId` es `null` hasta la primera serie. Con otro `sessionId`, usa ese y nunca crea otra. |
| SF02-R13 | **Registro.** Muestra, en el orden de foco de S42-R15: la barra con el nombre y la fecha, la nota fija observada en la base de datos (D13 de la Spec 40), el hueco `lastPerformance(exerciseId, resolvedSessionId, modifier)` (S42-R16), la lista de series de este ejercicio en esta sesión numeradas por posición (S40-R23) y el panel. "Editar ejercicio" abre el editor con `logAfterSave = false`. Una serie guardada se anuncia con una región en vivo (S40-R27). |
| SF02-R14 | **Valores propuestos.** Siguen D5 de la Spec 40: la primera serie del ejercicio en la sesión propone la primera serie de `observeLastPerformance(exerciseId, before = sesión resuelta)`; las siguientes, la última serie de hoy. En peso corporal sin propuesta, el peso adicional es 0 y está fijado. Tras guardar, el panel conserva los valores (S40-R27). Los valores propuestos cuentan como iniciales (S40-R40). |
| SF02-R15 | **Control.** `ValueAdjuster` cumple "Control del peso y las repeticiones" de la Spec 42 y S42-R18 a S42-R20. La lógica de la ventana y la lectura del valor escrito son funciones puras de `feature/session/ui/logger/`. Nada se redondea en silencio y no se guarda un valor sin fijar (S42-R19, S40-R08). |
| SF02-R16 | **Corregir, eliminar y archivado.** Tocar una serie pone el panel en corrección (S42-R17): "Guardar cambios" usa `CorrectSetUseCase`, "Eliminar serie" pide confirmar con `ConfirmDialog` y "Cancelar corrección" vuelve a una serie nueva. Con el ejercicio archivado, el panel no ofrece serie nueva y muestra el texto de la Spec 42 (S40-R29). |
| SF02-R17 | **Peso más alto de la última sesión.** F02 agrega `observeLatestSessionPeaks(): Flow<List<LatestSessionPeak>>` a `SessionRepository` como contrato explícito (S20-R49, S30-R17), con `LatestSessionPeak(exerciseId, sessionId, date, heaviestWeightGrams)` y una función pura `latestSessionPeaks(sessions, sets)` en `feature/session/domain/` para el fake (S31-R06). Incluye su consulta en `SessionDao`, su implementación Room y su caso en `SessionRepositoryContract` (S31-R08). |
| SF02-R18 | **Músculos de todos los ejercicios.** F02 agrega `observeAllMuscles(): Flow<List<ExerciseMuscle>>` a `ExerciseRepository` para el subtítulo de las tarjetas, con `SELECT * FROM exercise_muscle` en `ExerciseDao`, su fake y su caso de contrato (D2, S30-R17). |
| SF02-R19 | **Datos de la sesión.** P05 sigue la Spec 42: fecha con `DatePickerDialog` y `selectableDates` sin días futuros, nota y "Guardar" con `UpdateSessionUseCase` (S42-R21, S40-R30). Un solo `NavigationBackHandler`, activo solo con cambios (S42-R06). |
| SF02-R20 | **Formatos.** `ui/theme/Formats.kt` da peso, repeticiones, serie y fecha corta, visibles y para TalkBack, según "Formatos" de la Spec 41. Los nombres de días y meses llegan de `strings.xml` (S41-R14, S42-R26). |
| SF02-R21 | **Componentes.** `PillTabs`, `ExerciseCard` y `WeightNumber` viven en `ui/theme/component/`, sin estado, sin importar `feature` y con preview (S41-R12, S41-R16). La tarjeta usa el tono por equipo de la dirección A. Pulsar la tarjeta escala a 0,96, los dígitos de `WeightNumber` ruedan y una serie nueva entra con `animateItem`; con movimiento reducido, solo hay cambios de tono o de golpe (S42-R27, S41-R10). |
| SF02-R22 | **Errores y textos.** Cada `ExerciseSetError` y `WorkoutSessionError` tiene su texto en `strings.xml`, mapeado en `session/ui` (S30-R24). Una excepción inesperada muestra el mensaje genérico, conserva lo escrito y deja reintentar (S30-R23). `FakeSessionRepository` agrega un fallo provocado con su prueba (S31-R08). Las etiquetas de equipo y músculo reutilizan los recursos de F01. |
| SF02-R23 | **Pantallas y fuente.** Cada pantalla tiene su `Scaffold` y su `SnackbarHost`; las listas usan `innerPadding` como `contentPadding` (S42-R10). Con la fuente al 200 %, la acción principal de Hoy y "Guardar serie" se ven y se tocan (S42-R28). |

## Plan

**Capas.** Dominio: cuatro casos de uso, un tipo y una función pura. Datos: dos lecturas nuevas. UI: tres pantallas, el control, tres componentes y los formatos. Shell: la raíz pasa a `HomeKey`. Orden de trabajo de S31-R22: dominio y casos de uso, ViewModel, `Content` y navegación.

**Archivos.** Rutas relativas a `com/juancholopes/weightrack/`.

| Paquete | Archivo | Qué cambia |
| --- | --- | --- |
| raíz | `AppContainer.kt` | `clock` y los cuatro casos de uso |
| `feature/catalog/domain/`, `feature/catalog/data/` | `ExerciseRepository.kt`, `ExerciseDao.kt`, `RoomExerciseRepository.kt` | `observeAllMuscles()` (SF02-R18) |
| `feature/session/domain/` | `CreateSessionUseCase.kt`, `UpdateSessionUseCase.kt`, `AddSetUseCase.kt`, `CorrectSetUseCase.kt`, `SessionWriteResults.kt` | Escrituras validadas (SF02-R04, SF02-R05) |
| `feature/session/domain/` | `SessionQueries.kt`, `SessionRepository.kt` | `LatestSessionPeak`, `latestSessionPeaks` y `observeLatestSessionPeaks()` (SF02-R17) |
| `feature/session/data/` | `SessionDao.kt`, `SessionRows.kt`, `RoomSessionRepository.kt` | Consulta guía de S20-R49 y su fila |
| `feature/session/ui/` | `SessionKeys.kt`, `SessionEntries.kt`, `SessionLabels.kt` | Claves, entradas y mapeo de errores y enums |
| `feature/session/ui/home/` | `HomeScreen.kt`, `HomeContent.kt`, `HomeViewModel.kt`, `HomeUiState.kt` | S42-P01 |
| `feature/session/ui/logger/` | `SetLoggerScreen.kt`, `SetLoggerContent.kt`, `SetLoggerViewModel.kt`, `SetLoggerUiState.kt`, `ValueAdjuster.kt`, `ValueWindow.kt`, `WeightInput.kt` | S42-P02 y el control |
| `feature/session/ui/sessioneditor/` | `SessionEditorScreen.kt`, `SessionEditorContent.kt`, `SessionEditorViewModel.kt`, `SessionEditorUiState.kt` | S42-P05 |
| `ui/` | `SessionDestinations.kt`, `EditorResults.kt`, `CatalogDestinations.kt`, `WeightrackApp.kt`; se borra `StartScreen.kt` | Puente `sessionDestinations(backStack, ...)` de SF00-R08, `logAfterSave`, raíz `HomeKey` (SF02-R02) |
| `ui/theme/` | `Formats.kt`; `component/PillTabs.kt`, `ExerciseCard.kt`, `WeightNumber.kt` | SF02-R20, SF02-R21 |
| `testFixtures/.../testing/` | Fakes y contratos | Las dos lecturas nuevas y el fallo provocado de sesiones |

**Estados.** Guía con los campos de la Spec 42; los nombres finales salen de la implementación.

```kotlin
data class HomeUiState(
    val isLoading: Boolean, val today: LocalDate, val selectedTab: HomeTab,
    val todaySession: TodaySessionUi?, val todayCards: List<ExerciseCardUi>,
    val lastSessionDate: LocalDate?, val lastSessionCards: List<ExerciseCardUi>,
    val exerciseCards: List<ExerciseCardUi>, val query: String,
    val muscleFilter: Muscle?, val equipmentFilter: Equipment?, val catalogIsEmpty: Boolean,
    val canStartAnotherSession: Boolean, val notice: HomeNotice?, val genericError: Boolean,
)
data class ExerciseCardUi(
    val exerciseId: Long, val name: String, val primaryMuscle: Muscle?, val equipment: Equipment,
    val archived: Boolean, val peakWeightGrams: Int?, val peakDate: LocalDate?, val setsToday: Int?,
)
data class SetLoggerUiState(
    val isLoading: Boolean, val exercise: LoggerExerciseUi?, val sessionDate: LocalDate,
    val resolvedSessionId: Long?, val todaySets: List<SetRowUi>,
    val weight: ValueInput, val reps: ValueInput, val editingSetId: Long?,
    val hasUnsavedChanges: Boolean, val isSaving: Boolean, val fieldError: LoggerFieldError?,
    val typingDialog: TypingDialog?, val confirmDialog: LoggerDialog?,
    val savedAnnouncement: SetRowUi?, val genericError: Boolean,
)
data class ValueInput(val value: Int?, val isFixed: Boolean, val windowCenter: Int) // gramos o repeticiones
```

`notice`, `savedAnnouncement` y `genericError` son efectos (S30-R28). `SessionEditorUiState` lleva `date`, `today`, `notes`, `fieldError`, `showDatePicker`, `hasUnsavedChanges`, `isSaving`, `saved` y `genericError` (Spec 42).

**Lecturas del inicio.** `observeHistory()` da la sesión de hoy y la anterior a hoy; `observeSessionSets` de cada una da las tarjetas de Hoy y de la última sesión; `observeActiveCatalog`, `observeAllMuscles` y `observeLatestSessionPeaks` dan las tarjetas de Ejercicios. Todo se combina con `flatMapLatest` y `combine`, y se expone con `stateIn` (S30-R26).

**Lecturas del registro.** `observeExercise`, `observeHistory` (para resolver la sesión con la fecha de apertura), `observeSessionSets(resolvedSessionId)` filtrada por el ejercicio y `observeLastPerformance(exerciseId, before)` para proponer valores. `session` no importa `lastperformance` (S30-R10, S42-A12).

**Ventana del control.** Funciones puras, probadas en la JVM: `weightWindow(referenceGrams)` centra 20 kg en la parada de 1250 g más cercana y recorta a 0..1 000 000 g; `repsWindow(reference)` centra 20 repeticiones y recorta a 1..999; `parseWeightInput(text)` acepta coma o punto y devuelve gramos o un error, sin redondear.

**Riesgos.**

- Es la spec más grande del MVP. Si el tiempo no alcanza, primero el registro y Hoy; Ejercicios con búsqueda y filtros al final, dentro de esta misma spec.
- La consulta de S20-R49 debe desempatar por `id` el mismo día. Lo cubre su caso de contrato contra Room.
- `observeAllMuscles` es un contrato que no pidió la Spec 42 (D2).
- "Empezar otra sesión" puede dejar sesiones vacías que solo F04 permite borrar.
- La Spec 41 todavía asigna `PillTabs` a F00 y `ExerciseCard` a F01. F00 y F01 no los construyen (F00, "Fuera de alcance"), así que los construye F02.

## Tareas

Cada tarea sigue S31-R22: prueba primero, verla fallar, código mínimo, refactorizar.

| # | Tarea | Criterio |
| --- | --- | --- |
| T01 | Casos de contrato de `observeLatestSessionPeaks` y `observeAllMuscles`; implementar `latestSessionPeaks`, los fakes, las consultas de DAO con sus pruebas y los repositorios Room. | SF02-A02, SF02-A03 |
| T02 | Fallo provocado en `FakeSessionRepository`, con su prueba. | SF02-A14 |
| T03 | Pruebas de los cuatro casos de uso; implementarlos y exponerlos con `clock` en el `AppContainer`. | SF02-A01 |
| T04 | `FormatsTest`; implementar `Formats.kt` y sus textos de días y meses. | SF02-A20 |
| T05 | `ValueWindowTest` y `WeightInputTest`; implementar `ValueWindow.kt` y `WeightInput.kt`. | SF02-A09, SF02-A10 |
| T06 | `SetLoggerViewModelTest`: sesión, valores propuestos, reglas, corrección, archivado, nota y fallos; implementar `SetLoggerUiState` y `SetLoggerViewModel`. | SF02-A07 a SF02-A12, SF02-A14, SF02-A19 |
| T07 | `HomeViewModelTest`: Hoy, menú, Ejercicios, tarjetas y aviso; implementar `HomeUiState` y `HomeViewModel`. | SF02-A04 a SF02-A06 |
| T08 | `SessionEditorViewModelTest`; implementar su `UiState` y su ViewModel. | SF02-A13, SF02-A14 |
| T09 | Construir `WeightNumber`, `ExerciseCard` y `PillTabs` con previews. `SetLoggerContentTest`; implementar `ValueAdjuster`, `SetLoggerContent` y `SetLoggerScreen` con el hueco. | SF02-A16, SF02-A17, SF02-A21 |
| T10 | `HomeContentTest`; implementar `HomeContent` y `HomeScreen`. | SF02-A15, SF02-A21 |
| T11 | `SessionEditorContentTest`; implementar su `Content` y su `Screen`. | SF02-A13, SF02-A17 |
| T12 | Ampliar `EditorResultsTest`; cambiar la raíz a `HomeKey`, agregar `SessionDestinations.kt` con `sessionEntries(...)`, conectar el aviso, borrar `StartScreen.kt` y adaptar `MainActivitySmokeTest` y `ShellNavigationTest` de F00 a la nueva raíz. | SF02-A18 |
| T13 | Correr las búsquedas de arquitectura y el comando completo; llenar "Verificación". | SF02-A22, SF02-A23 |

## Criterios de aceptación

Las pruebas JVM corren con `./gradlew test`, construyen los ViewModels y casos de uso con fakes y `Clock.fixed` (S31-R11 a S31-R13), y las instrumentadas con `./gradlew connectedDebugAndroidTest`. Cada prueba lleva el comentario del criterio (S31-R21). Los nombres son una guía.

| ID | Criterio | Comprueba | Evidencia |
| --- | --- | --- | --- |
| SF02-A01 | `AddSetUseCase` sin sesión crea una sola sesión con la fecha dada y su serie; con sesión, agrega la serie con el siguiente `order`. 62 300 g, 1 000 250 g, 0 y 1000 repeticiones, o un ejercicio archivado, se rechazan sin escribir nada. `CorrectSetUseCase` cambia la serie en su lugar. `UpdateSessionUseCase` rechaza mañana y una nota de 501 caracteres, y guarda "   " como `null`. `CreateSessionUseCase` crea una sesión de hoy. | SF02-R04, SF02-R05 (partes de S40-A24 y S40-A28) | `AddSetUseCaseTest`, `CorrectSetUseCaseTest`, `UpdateSessionUseCaseTest`, `CreateSessionUseCaseTest` (JVM) |
| SF02-A02 | Con series del ejercicio en dos sesiones del mismo día (60 kg; 62,5 kg en la de mayor `id`) y otro ejercicio sin series, `observeLatestSessionPeaks` da una fila con la sesión de mayor `id` y 62 500 g, y ninguna para el segundo. | SF02-R17 (S20-A42) | Caso en `SessionRepositoryContract` corrido contra el fake y Room; prueba en `SessionDaoTest` |
| SF02-A03 | `observeAllMuscles` devuelve los músculos de todos los ejercicios y emite de nuevo tras crear uno. | SF02-R18 | Caso en `ExerciseRepositoryContract`, fake y Room |
| SF02-A04 | El inicio abre en Hoy. Sin sesión de hoy, no ofrece "Empezar otra sesión"; con una, sí, y al tocarla crea una sola sesión que Hoy pasa a mostrar. Hoy lista los ejercicios de la sesión de hoy en el orden de su primera serie, una vez cada uno, y "De tu última sesión" los de la sesión anterior sin series hoy. | SF02-R06, SF02-R07 (S40-A08, S40-A19, S40-A20, S42-A10) | `HomeViewModelTest` |
| SF02-A05 | Ejercicios excluye archivados y ordena por nombre; "elevacion" encuentra "Elevación lateral" y los filtros se combinan con la búsqueda. Cada tarjeta trae el peso más alto y la fecha de la última sesión, o ninguno sin series, y el primer músculo principal en el orden del enum. | SF02-R08, SF02-R09 (S40-A10, parte de S40-A11 y de S42-A09) | `HomeViewModelTest` |
| SF02-A06 | Con un `PendingNotice.Archived`, el inicio muestra el aviso; "Deshacer" devuelve el ejercicio al catálogo activo. | SF02-R11 (parte de S42-A07 y de S40-A16) | `HomeViewModelTest` |
| SF02-A07 | Abrir el registro sin sesión de hoy no crea nada. Dos `onSaveClick()` seguidos crean una sola sesión con una serie. Con dos sesiones de hoy, la serie va a la de mayor `id`. Con el reloj del caso de uso en el día siguiente, la serie va a una sesión con la fecha de apertura. Un ViewModel nuevo sobre los mismos fakes muestra las series guardadas. | SF02-R05, SF02-R12 (S42-A08, S40-A18, S40-A30) | `SetLoggerViewModelTest` |
| SF02-A08 | La primera serie propone la primera de la última vez; la siguiente, la recién guardada. En peso corporal sin propuesta, el peso adicional es 0. Sin propuesta en otro equipo, guardar muestra "Fija el peso para guardar la serie." y no escribe. Con los valores propuestos sin tocar, no hay cambios pendientes. | SF02-R14 (S40-A23, S40-A04) | `SetLoggerViewModelTest` |
| SF02-A09 | Escribir "62,3" o "1000,25" muestra el error y no cambia el valor; "62,5" y "62.5" fijan 62 500 g. En 0 kg, bajar está desactivado. | SF02-R15 (S42-A15, parte de S40-A24) | `WeightInputTest` y `SetLoggerViewModelTest` |
| SF02-A10 | Con 60 kg, la ventana va de 50 a 70 kg; + da 60,25 kg; el slider solo da múltiplos de 1,25 kg; al soltar en 70 kg, la ventana pasa a 60 a 80 kg; con 62,25 kg, se centra en 62,5 kg. Cerca de 0 y de 1000 kg, se recorta. | SF02-R15 (S42-A14) | `ValueWindowTest` (JVM) |
| SF02-A11 | Tocar una serie pone el panel en corrección con sus valores; guardar la cambia en su lugar; eliminar pide confirmar y la sesión sigue aunque quede sin series. Series con `order` 1, 3 y 4 se numeran 1, 2 y 3. | SF02-R16, SF02-R13 (S40-A26, S42-A13, parte de S40-A20) | `SetLoggerViewModelTest` |
| SF02-A12 | Con el ejercicio archivado, el panel no ofrece serie nueva y sus series se pueden corregir y eliminar. | SF02-R16 (S40-A27) | `SetLoggerViewModelTest` |
| SF02-A13 | En P05, mañana no se puede elegir en el selector y además se rechaza al guardar. | SF02-R19 (S42-A17, S40-A28) | `SessionEditorViewModelTest` y `SessionEditorContentTest` (instrumentada) |
| SF02-A14 | Si el fake lanza una excepción al guardar una serie o la sesión, aparece el mensaje genérico, se conserva lo escrito y un segundo intento guarda. | SF02-R22 (S40-A38) | `SetLoggerViewModelTest`, `SessionEditorViewModelTest` y la prueba del fallo en `FakeSessionRepositoryTest` |
| SF02-A15 | En `HomeContent`: el botón flotante registra `ExerciseEditorKey(logAfterSave = true)` en Hoy y `ExerciseEditorKey()` en Ejercicios; una búsqueda sin resultados ofrece "Crear «{texto}»" con el texto como `prefillName`. Tocar una tarjeta llama a abrir el registro con su `exerciseId`. La tarjeta es un nodo que lee nombre, músculo, equipo, fecha y peso con unidad. Con lambdas `null` no hay "Ver estadísticas" ni "Historial". Atrás en Ejercicios vuelve a Hoy. Ningún vacío aparece antes del primer dato. | SF02-R08 a SF02-R11 (S42-A11, S40-A12, S42-A09, S42-A20, S42-A21, parte de S42-A03) | `HomeContentTest` (instrumentada) |
| SF02-A16 | En `SetLoggerContent`: el orden de foco es el de S42-R15; el hueco recibe `resolvedSessionId`; con valores propuestos, un toque en "Guardar serie" guarda sin enfocar ningún campo de texto; la serie nueva aparece con su número y se anuncia; el número, −, + y el slider exponen etiqueta y valor con unidad, y el número tiene acción de clic. | SF02-R13, SF02-R15 (S40-A03, S40-A25, S42-A12, S42-A16, parte de S40-A40) | `SetLoggerContentTest` (instrumentada) |
| SF02-A17 | Con cambios, atrás en el registro y en P05 muestra "¿Descartar los cambios?"; sin cambios, sale sin diálogo. Cada archivo tiene un solo `NavigationBackHandler(`. | SF02-R10, SF02-R19 (partes de S42-A05, S40-A36 y S40-A37) | `SetLoggerContentTest`, `SessionEditorContentTest` y `grep -c "NavigationBackHandler(" <archivo>` |
| SF02-A18 | Las funciones de resultado del shell dejan la pila así: guardar con `logAfterSave = true` desde `[HomeKey, ExerciseEditorKey]` da `[HomeKey, SetLoggerKey(id)]`; sin él, `[HomeKey]`; archivar desde `[HomeKey, SetLoggerKey(id), ExerciseEditorKey(id)]` da `[HomeKey]`. La app abre en el inicio, en Hoy. | SF02-R02 (S42-A02, S42-A06, S40-A22, parte de S42-A07) | `EditorResultsTest` (JVM) y `MainActivitySmokeTest` adaptada (S31-R17) |
| SF02-A19 | Editar la nota del ejercicio con el registro abierto cambia la nota del `UiState` del registro. | SF02-R13 (S40-A29) | `SetLoggerViewModelTest` |
| SF02-A20 | `Formats.kt` da "62,5 kg", "60 kg", "62,25 kg", "1000 kg", "60 kg × 10", "jue 8 oct", "Hoy", "Ayer" y el año solo si no es el actual, con sus lecturas para TalkBack. No hay formatos escritos a mano fuera de `ui/theme/`. | SF02-R20 (S42-A25, parte de S41-A12) | `FormatsTest` (JVM) y `grep -rn '" kg"' app/src/main/java \| grep -v /ui/theme/` sin resultados |
| SF02-A21 | Con la fuente al 200 %, "Crear ejercicio" o una tarjeta en Hoy y "Guardar serie" se ven y se pueden tocar. | SF02-R23 (S42-A23, S41-A11) | `HomeContentTest` y `SetLoggerContentTest` con `DeviceConfigurationOverride.FontScale(2f)` |
| SF02-A22 | Las reglas de arquitectura se cumplen: S30-A03 a S30-A08 (el reloj solo en `AppContainer.kt`), S30-A12 a S30-A19 (ninguna escritura validada desde `ui`), S41-A01, S41-A10 y S42-A01. | S30-R03, SF02-R03, SF02-R04 | Las búsquedas de esos criterios, anotadas en "Verificación" |
| SF02-A23 | El build y todas las pruebas pasan desde cero. | Definición de hecho | `./gradlew assembleDebug test connectedDebugAndroidTest` (S31-R24) |

Las pruebas instrumentadas no escriben datos (S31-R18): prueban `Content` con estados armados y la pila con funciones puras. Lo que necesita datos se prueba en la JVM con fakes.

## Decisiones

- [x] **D1. Casos de uso.** Decidido el 2026-10-11 (el dueño aceptó de antemano las recomendaciones): A, uno por escritura validada. A: `CreateSessionUseCase`, `UpdateSessionUseCase`, `AddSetUseCase` y `CorrectSetUseCase`, cada uno con una sola función pública. B: un `SaveSetUseCase` que agrega o corrige según el `id`. Por qué A: cumple S30-R19 (una sola función pública) y cada uno valida con su función del dominio. Costo: cuatro clases cortas.
- [x] **D2. Músculo de la tarjeta.** Decidido el 2026-10-11 (el dueño aceptó de antemano las recomendaciones): A, una lectura de todos los músculos. A: `observeAllMuscles()` en `ExerciseRepository` y el primer principal en el orden del enum `Muscle`. B: combinar `observeMuscles(id)` de cada ejercicio, un `Flow` por tarjeta. C: mostrar solo el equipo. Por qué A: una consulta para todas las tarjetas, como D11 de la Spec 42, y un orden estable, porque la tabla no guarda el orden en que se eligieron. B abre decenas de consultas observadas. C incumple S42-R12. Costo: un contrato más en `ExerciseRepository`, que la Spec 20 debe documentar.
- [x] **D3. Pruebas de navegación sin datos.** Decidido el 2026-10-11 (el dueño aceptó de antemano las recomendaciones): A, reglas de pila como funciones puras y `Content` con estados. A: `EditorResults.kt` en la JVM, y pruebas instrumentadas de `Content` que no tocan la base de datos. B: un `AppContainer` de prueba con Room en memoria. Por qué A: cumple S31-R18 y ADR-30-04 sin cambiar el contenedor. B exige un ADR. Costo: el recorrido completo con datos solo se ve en el teléfono (S40-A06, criterio de F03).
- [x] **D4. Archivados en "De tu última sesión".** Decidido el 2026-10-11 (el dueño aceptó de antemano las recomendaciones): A, no se muestran. A: solo los ejercicios activos. B: mostrarlos con la marca "Archivado". Por qué A: no reciben series nuevas (S10-R11, S10-R18), y sus series siguen en el historial de F04. Costo: ninguno.

## Fuera de alcance

- La sección de la última vez: F03. F02 deja el hueco y lo llena con un composable vacío.
- Historial, sesión pasada y elegir ejercicio para una sesión pasada (S42-P07 a S42-P10): F04. F02 ya acepta `SetLoggerKey` con `sessionId`.
- Estadísticas (S42-P06): F05.
- Cambiar el ejercicio de una serie (D15 de la Spec 40), temporizador de descanso, rutinas y lo demás de "Fuera de alcance" de la Spec 40 (S40-R51).

**Entrega a F03.** F02 le deja el hueco `lastPerformance(exerciseId, sessionId, modifier)` en `SetLoggerScreen` con el `resolvedSessionId`, el composable vacío del shell para reemplazar, `Formats.kt` para series y fechas, y el `clock` del `AppContainer`.

## Cambios que esta spec pide a otras specs

Esta spec no edita ninguna. El dueño aprueba y aplica cada cambio.

| Spec | Cambio | Motivo |
| --- | --- | --- |
| 20 Persistencia | Agregar a "Lecturas nuevas para las specs F" `ExerciseRepository.observeAllMuscles()` con `SELECT * FROM exercise_muscle`, que implementa F02. Anotar que el tipo de S20-R49 se llama `LatestSessionPeak`. | D2 y SF02-R17. |
| 31 Estrategia de pruebas | En S31-R18, aclarar que `MainActivitySmokeTest` puede abrir la base de datos de la build debug para leer, porque el inicio lee el historial al arrancar. | D3. Hoy S31-R18 prohíbe abrir `weightrack.db`. |
| 40 Flujos de usuario | En S40-A10, la columna "La aporta" pasa de F01 a F02: el catálogo es la pestaña Ejercicios. | Spec 42, D2. |
| 41 Sistema de diseño | En "Componentes compartidos", `PillTabs` y `ExerciseCard` los construye F02 (fila "41" de la Spec 42). | Spec 42, D2. |

## Verificación

Se llena al implementar, según S31-R25: comandos ejecutados, resultado y conteo de pruebas por tarea de Gradle.

## Fuentes

Consultadas el 2026-10-11:

- Specs 00, 01, 10 (S10-R01 a S10-R03, S10-R09, S10-R11 a S10-R15, S10-R25, S10-R26, S10-R28 a S10-R30, S10-R32 a S10-R34), 20 (S20-R29, S20-R31 a S20-R33, S20-R39, S20-R49), 30 (ADR-30-03 a ADR-30-10), 31 (S31-R04 a S31-R25), 40 (S40-F01, S40-F03, S40-F08 a S40-F15, S40-F23, D3, D5, D13, D16, D17), 41 (tokens, `NumberStyles`, "Componentes compartidos", "Formatos", movimiento) y 42 (S42-P01, S42-P02, S42-P05, "Control del peso y las repeticiones", D4, D7, D8, D10, D11).
- Spec F01: editor, `observeHasSets`, `matchesExerciseSearch`, `PendingNotice` y `EditorResults.kt`.
- Código: `feature/session/domain/` (`SessionRepository.kt`, `SessionQueries.kt`, `ExerciseSetRules.kt`, `WorkoutSessionRules.kt`), `feature/session/data/SessionDao.kt`, `AppContainer.kt` y los fakes de `testFixtures`.
- Skills en `.claude/skills/`: `navigation-3` (claves, entradas y ViewModel por entrada), `navigation-event` (un `NavigationBackHandler` por estado; pestañas sin despachador hijo) y `edge-to-edge` (`Scaffold` por pantalla, `contentPadding`, teclado con `fitInside`).
