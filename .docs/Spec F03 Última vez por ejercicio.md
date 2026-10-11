# Spec F03: Última vez por ejercicio

Oct 11, 2026 · @Juan

| ID | Estado | Fase | Depende de |
| --- | --- | --- | --- |
| F03 | Aprobada | 4 Features | F02 |

## Propósito

F03 muestra, mientras el usuario registra un ejercicio, todas las series de ese ejercicio en su sesión anterior más reciente: la "última vez" (S01-R03). Es el motivo para abrir la app en el gimnasio.

Es una sección pequeña con su propio ViewModel en `feature/lastperformance/ui/`. El shell la pone en el hueco del registro de series de F02 (D2 de la Spec 30, S42-R16). No agrega consultas: usa la última vez que ya existe (S10-R25, S10-R26, S20-R29).

Con F03 se cierra la 0.0.1 (MVP): esta spec lleva el criterio de extremo a extremo de la Spec 01 para etiquetarla.

Las decisiones de esta spec quedan tomadas el 2026-10-11: el dueño aceptó de antemano las recomendaciones y pidió un MVP funcional hoy.

## Alcance

| Fuente | Qué implementa F03 |
| --- | --- |
| Flujos (Spec 40) | S40-F16. Paso 7 de S40-F22, sin el paso 6 (historial, F04) |
| Pantallas (Spec 42) | "Hueco de la última vez (F03)" de S42-P02 |
| Requisitos S40 | S40-R33, S40-R34, S40-R35, y S40-R47 a S40-R49 en la sección |
| Requisitos S42 | S42-R15 (posición en el foco), S42-R16, S42-R24 (sin "Ver estadísticas" antes de F05), S42-R26 |
| Dominio y datos | Usa S10-R18, S10-R25, S10-R26 y S20-R29 con `observeLastPerformance` y `observeSession`. No agrega nada |
| Arquitectura (Spec 30) | S30-R06, S30-R07 (sin `data/` ni `domain/`), S30-R10, S30-R12, S30-R15, S30-R16, S30-R23, S30-R25 a S30-R31 |
| Versiones (Spec 01) | Criterio de etiqueta de la 0.0.1 y S01-R11 |

## Requisitos

| ID | Requisito |
| --- | --- |
| SF03-R01 | **Paquete.** F03 vive en `feature/lastperformance/ui/` con `LastPerformanceSection.kt` (hace de `Screen`), `LastPerformanceContent.kt`, `LastPerformanceViewModel.kt` y `LastPerformanceUiState.kt` (S30-R06, S30-R12). No tiene `domain/` ni `data/` (S30-R07). Solo importa `session.domain`, `catalog.domain` y `ui/theme` (S30-R10). |
| SF03-R02 | **Hueco.** El shell reemplaza el composable vacío de F02 por `LastPerformanceSection(exerciseId, sessionId, onOpenProgress = null, modifier)`, como muestra la Spec 42. `session` no importa `lastperformance` (S42-R16). |
| SF03-R03 | **Lectura.** Con `sessionId` nulo, el ViewModel observa `observeLastPerformance(exerciseId, before = null)`. Con un `sessionId`, observa `observeSession(sessionId)` y, con esa sesión, `observeLastPerformance(exerciseId, before = sesión)`, para excluirla a ella y a las posteriores (S10-R26, S40-R34). Incluye las series de un ejercicio archivado (S10-R18). |
| SF03-R04 | **Un ViewModel por ejercicio y sesión.** La sección obtiene su ViewModel con `viewModel(key = "lastPerformance:$exerciseId:$sessionId", factory = LastPerformanceViewModel.factory(exerciseId, sessionId))`, en el `ViewModelStore` de la entrada del registro (S30-R16, skill `navigation-3`). Cuando la primera serie crea la sesión y `sessionId` pasa de `null` a un `id`, la sección cambia de ViewModel y no muestra las series que se acaban de anotar. |
| SF03-R05 | **Contenido.** Con datos, muestra "Última vez · {fecha}" y las series en orden con el formato de serie de `Formats.kt`: "60 kg × 10 · 60 × 10 · 60 × 8" (S40-F16, S42-R26). Sin datos, muestra "Primera vez con este ejercicio." (S40-R35). Mientras carga, reserva su alto para que el panel no salte. |
| SF03-R06 | **Accesibilidad.** La sección es un encabezado y un solo nodo para TalkBack: "Última vez, jueves 8 de octubre: 60 kilogramos, 10 repeticiones; ..." (S42-P02, S40-R49). |
| SF03-R07 | **Estadísticas.** "Ver estadísticas" se muestra solo si `onOpenProgress` no es `null`. Hasta F05 es `null` (S42-R24). |
| SF03-R08 | **Error.** Si la lectura falla, el `Flow` emite un estado de error con `catch` (S30-R23) y la sección muestra "No se pudo cargar la última vez." en su lugar, sin bloquear el registro (D3). Para probarlo, F03 agrega a `FakeSessionRepository` un fallo de lectura provocado, con su prueba (S31-R08). |
| SF03-R09 | **Valores propuestos.** F03 no alimenta a F02: S30-R10 no deja a `session` importar `lastperformance`. F02 lee la misma consulta para proponer valores (SF02-R14, D5 de la Spec 40). Con el mismo `exerciseId` y la misma sesión, la primera serie que muestra F03 es la que propone F02 (D1). |
| SF03-R10 | **Textos.** "Última vez · {fecha}", "Primera vez con este ejercicio.", "Ver estadísticas" y el texto de error van en `strings.xml` (S40-R50). |
| SF03-R11 | **Cierre de la 0.0.1.** Antes de etiquetar, `versionName` pasa de "1.0" a "0.0.1" en `app/build.gradle.kts`. `versionCode` queda en 1, porque es la primera etiqueta; cada etiqueta siguiente lo sube (S01-R11; F00, "Fuera de alcance"). La etiqueta `v0.0.1` y su commit se hacen solo cuando el dueño lo pide (constitución, regla 8). |

## Plan

**Capas.** Solo UI: una sección con su ViewModel. El dominio y los datos no cambian. El shell cambia una lambda.

**Archivos.** Rutas relativas a `com/juancholopes/weightrack/`.

| Paquete | Archivo | Qué cambia |
| --- | --- | --- |
| `feature/lastperformance/ui/` | `LastPerformanceSection.kt`, `LastPerformanceContent.kt`, `LastPerformanceViewModel.kt`, `LastPerformanceUiState.kt` | Nuevos (SF03-R01 a SF03-R08) |
| `ui/` | `SessionDestinations.kt` | El hueco recibe `LastPerformanceSection` en lugar del composable vacío (SF03-R02) |
| `res/values/` | `strings.xml` | Textos de SF03-R10 |
| `testFixtures/.../testing/` | `FakeSessionRepository.kt` | Fallo de lectura provocado (SF03-R08) |
| raíz de `app/` | `build.gradle.kts` | `versionName = "0.0.1"` al cerrar la 0.0.1 (SF03-R11) |

**Estado.** Guía; los nombres finales salen de la implementación.

```kotlin
sealed interface LastPerformanceUiState {
    data object Loading : LastPerformanceUiState
    data object FirstTime : LastPerformanceUiState
    data class Shown(val date: LocalDate, val today: LocalDate, val sets: List<SetValues>) : LastPerformanceUiState
    data object Error : LastPerformanceUiState
}
data class SetValues(val weightGrams: Int, val reps: Int)
```

**ViewModel.** Recibe `exerciseId`, `sessionId: Long?`, `SessionRepository` y `Clock` (para "Hoy" y "Ayer" en la fecha, S30-R15). Arma la lectura de SF03-R03 con `flatMapLatest`, la convierte con `map` y `catch`, y la expone con `stateIn(viewModelScope, WhileSubscribed(5_000), Loading)` (S30-R26). No tiene eventos: es solo lectura. Su fábrica vive en el `companion` (S30-R16).

**Section y Content.** `LastPerformanceSection` obtiene el ViewModel con la clave de SF03-R04, recolecta con `collectAsStateWithLifecycle()` (S30-R30) y llama a `LastPerformanceContent(uiState, onOpenProgress, modifier)`. El `Content` formatea con `Formats.kt`; en `Loading` dibuja un espacio del alto de una línea de encabezado y una de series.

**Cómo llegan los valores propuestos a F02.** No llegan desde F03. F02 llama a `observeLastPerformance(exerciseId, before)` con la misma sesión resuelta y toma la primera serie (SF02-R14). Las dos lecturas salen de la misma consulta de S20-R29, así que coinciden. SF03-A05 lo comprueba.

**Riesgos.**

- Al pasar `sessionId` de `null` a un `id`, el ViewModel nuevo empieza en `Loading`. Reservar el alto evita que el panel salte.
- La sección y la propuesta de F02 hacen dos lecturas iguales. Es el costo de no cruzar `session` con `lastperformance` (S30-R10); la consulta usa índices y es pequeña.
- El recorrido de la 0.0.1 exige cambiar la fecha del teléfono o esperar al día siguiente. Lo hace el dueño a mano.

## Tareas

Cada tarea sigue S31-R22: prueba primero, verla fallar, código mínimo, refactorizar.

| # | Tarea | Criterio |
| --- | --- | --- |
| T01 | `LastPerformanceViewModelTest`: sin sesión, con sesión, mismo día, archivado, sin datos y error; implementar `LastPerformanceUiState` y `LastPerformanceViewModel` con su fábrica. | SF03-A01 a SF03-A04 |
| T02 | Prueba de coherencia con `SetLoggerViewModel`; ajustar si no coinciden. | SF03-A05 |
| T03 | Textos en `strings.xml`. `LastPerformanceContentTest`; implementar `LastPerformanceContent` con su preview y `LastPerformanceSection`. | SF03-A06, SF03-A07 |
| T04 | Conectar la sección en `SessionDestinations.kt`; comprobar que `session` no importa `lastperformance`. | SF03-A08 |
| T05 | Correr las búsquedas de arquitectura y el comando completo. | SF03-A09, SF03-A10 |
| T06 | Cambiar `versionName` a "0.0.1", instalar la release firmada y hacer el recorrido en modo avión. | SF03-A11, SF03-A12 |

## Criterios de aceptación

Las pruebas JVM corren con `./gradlew test`, con fakes y `Clock.fixed` (S31-R11), y las instrumentadas con `./gradlew connectedDebugAndroidTest`. Cada prueba lleva el comentario del criterio (S31-R21). Los nombres son una guía.

| ID | Criterio | Comprueba | Evidencia |
| --- | --- | --- | --- |
| SF03-A01 | Sin `sessionId`, la sección muestra la sesión más reciente con el ejercicio y todas sus series en orden de `order`. | SF03-R03, S10-R25 | `LastPerformanceViewModelTest` (JVM) |
| SF03-A02 | Desde una sesión antigua, muestra la sesión anterior a ella. Con dos sesiones del mismo día, la de mayor `id`. Incluye las series de un ejercicio archivado. | SF03-R03 (S40-A32) | `LastPerformanceViewModelTest` |
| SF03-A03 | Con una serie de hoy ya guardada, el ViewModel con ese `sessionId` muestra la sesión anterior y no la de hoy. Tras renombrar el ejercicio, la última vez sigue igual. | SF03-R04 (parte de S01-A06) | `LastPerformanceViewModelTest` |
| SF03-A04 | Sin series anteriores, el estado es `FirstTime`. Si el fake falla al leer, el estado es `Error`. | SF03-R05, SF03-R08 | `LastPerformanceViewModelTest` y la prueba del fallo en `FakeSessionRepositoryTest` |
| SF03-A05 | Con los mismos fakes, sin sesión de hoy, la primera serie que muestra la sección es la que propone `SetLoggerViewModel`. | SF03-R09 | `LastPerformanceViewModelTest.firstSet_matchesLoggerProposal` |
| SF03-A06 | `LastPerformanceContent` muestra "Última vez · jue 8 oct" y "60 kg × 10 · 60 × 10 · 60 × 8"; sin datos, "Primera vez con este ejercicio."; en `Loading` no muestra ese texto y ocupa alto. Es un encabezado y un solo nodo que se lee con unidades. | SF03-R05, SF03-R06 (S40-A31, parte de S40-A40) | `LastPerformanceContentTest` (instrumentada) con aserciones de semántica |
| SF03-A07 | Con `onOpenProgress = null`, no aparece "Ver estadísticas". `LastPerformanceContent` tiene preview `@ThemePreviews`. | SF03-R07 (parte de S42-A20), S41-R16 | `LastPerformanceContentTest`; la búsqueda de S41-A10 |
| SF03-A08 | El shell llena el hueco con `LastPerformanceSection` y `session` no importa `lastperformance`. | SF03-R02 (parte de S42-A12) | Revisión de `SessionDestinations.kt`; `grep -rn "lastperformance" app/src/main/java/com/juancholopes/weightrack/feature/session` sin resultados |
| SF03-A09 | Las reglas de arquitectura se cumplen en `lastperformance`: S30-A03, S30-A04, S30-A12 a S30-A17 y S41-A01. | SF03-R01, S30-R03 | Las búsquedas de esos criterios, anotadas en "Verificación" |
| SF03-A10 | El build y todas las pruebas pasan desde cero. | Definición de hecho | `./gradlew assembleDebug test connectedDebugAndroidTest` (S31-R24) |
| SF03-A11 | La release de la 0.0.1 tiene `versionName` "0.0.1" y `versionCode` 1, está firmada con la clave de la Spec 01 y su manifiesto no declara `INTERNET`. | SF03-R11 (S01-A03; S01-A09 al etiquetar) | `aapt2 dump badging app/build/outputs/apk/release/app-release.apk`; `apksigner verify --print-certs` con la huella de la Spec 01; `grep INTERNET` sobre el manifiesto fusionado sin resultados |
| SF03-A12 | **Recorrido de la 0.0.1.** En modo avión, con la app recién instalada: crear "Press de banca" desde Hoy (barra, pecho principal, tríceps secundario); el registro dice "Primera vez con este ejercicio."; guardar 60 kg × 10 y otra serie con los valores propuestos; en un día posterior, tocar la tarjeta y ver "Última vez · {fecha}" con las dos series y 60 kg × 10 propuesto. | Criterio de la 0.0.1 de la Spec 01; S40-F22 sin el paso 6 (parte de S40-A06) | Capturas o video en el teléfono, anotados en "Verificación" con el modelo, la API y el procedimiento de D2 |

## Decisiones

- [x] **D1. Valores propuestos.** Decidido el 2026-10-11 (el dueño aceptó de antemano las recomendaciones): A, F02 lee la misma consulta por su cuenta. A: cada feature llama a `observeLastPerformance` con la misma sesión. B: el shell pasa el estado de F03 al registro de F02. C: una función de dominio nueva que las dos usen. Por qué A: es lo que fija la Spec 42 y respeta S30-R10 y S30-R39. B mete estado de una feature en otra a través del shell. C repite lo que ya hace la consulta de S20-R29. Costo: dos lecturas iguales al abrir el registro.
- [x] **D2. Cómo se prueba "otro día".** Decidido el 2026-10-11 (el dueño aceptó de antemano las recomendaciones): A, la build debug con la fecha del teléfono adelantada. A: hacer el recorrido en `com.juancholopes.weightrack.debug`, adelantar un día la fecha del sistema (fecha automática apagada), comprobar la última vez y luego volver a la fecha automática y borrar los datos de la debug. B: hacerlo en la release en dos días reales. Por qué A: se etiqueta hoy, y los datos de prueba no ensucian la app de uso diario, que vive aparte (SF00-R12). B es más fiel, pero retrasa la etiqueta un día; además ocurre solo en el primer uso real. Costo: la release se comprueba sin cambio de fecha (instala, abre y registra en modo avión).
- [x] **D3. Error de lectura.** Decidido el 2026-10-11 (el dueño aceptó de antemano las recomendaciones): A, un texto corto en la sección. A: "No se pudo cargar la última vez." B: esconder la sección. Por qué A: el usuario sabe que falta un dato y no lo confunde con "Primera vez". Costo: un texto nuevo, que no estaba en las Specs 40 ni 42.

## Fuera de alcance

- El enlace "Ver estadísticas" y su pantalla: F05. F03 deja el parámetro `onOpenProgress`.
- La última vez dentro del historial o de una sesión pasada: F04, que reutiliza el registro de F02 con su `sessionId` y, con él, esta sección.
- Comparar hoy con la última vez, récords o tendencias: F05.
- La etiqueta `v0.0.1` y el commit: solo cuando el dueño lo pide.

**Entrega.** F03 cierra la 0.0.1. A F05 le deja el parámetro `onOpenProgress` de la sección, y a F04 una sección que ya funciona con un `sessionId` explícito.

## Verificación

Se llena al implementar, según S31-R25: comandos ejecutados, resultado y conteo de pruebas por tarea de Gradle, y las capturas del recorrido de la 0.0.1.

## Fuentes

Consultadas el 2026-10-11:

- Specs 00 (definición de hecho y regla 8), 01 ("Versiones", S01-R11, S01-A03, S01-A06, S01-A09, "Firma de los APK"), 10 (S10-R18, S10-R25, S10-R26), 20 (S20-R29), 30 (ADR-30-03, D2, S30-R10, S30-R16, S30-R23 a S30-R31), 31 (S31-R08, S31-R11, S31-R21 a S31-R25), 40 (S40-F16, S40-F22, D5, D6), 41 ("Formatos") y 42 ("Hueco de la última vez (F03)", S42-R16, S42-R24).
- Specs F00 (SF00-R08, SF00-R12, SF00-R13, "Fuera de alcance") y F02 (SF02-R13, SF02-R14, `SessionDestinations.kt`).
- Código: `feature/session/domain/SessionRepository.kt` (`observeLastPerformance`, `observeSession`), `SessionQueries.kt` (`LastPerformance`, `lastPerformance`) y `app/build.gradle.kts` (`versionCode = 1`, `versionName = "1.0"`).
- Skill `navigation-3`: el decorador de `ViewModelStore` por entrada y `viewModel(key = ...)` para varios ViewModels en una misma entrada.
