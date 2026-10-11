# Spec 31: Estrategia de pruebas

Oct 11, 2026 · @Juan

| ID | Estado | Fase | Depende de |
| --- | --- | --- | --- |
| 31 | Implementada | 2 Arquitectura | 30 |

## Propósito

Esta spec fija cómo se prueba Weightrack: qué tipos de prueba tiene la primera versión, dónde corre cada uno, con qué librerías, dónde viven los fakes, cómo se nombran las pruebas, cómo se trabaja con la prueba primero y qué comandos demuestran que el build y las pruebas pasan.

Recoge las costuras que dejó la Spec 30 ("Costuras para otras specs") y la decisión de la Spec 20 de probar la base de datos en el dispositivo. No cambia código de producción.

Cómo se aplica:

- Esta spec construye ahora solo la infraestructura que ya tiene un consumidor: los fakes de los repositorios con su prueba de contrato, `kotlinx-coroutines-test` en las pruebas JVM, el `MainDispatcherRule` y una prueba de humo del arranque. El resto queda descrito y entra con la primera spec que lo usa (columna "Entra con" de la pirámide).
- Cada spec F escribe sus pruebas con estas reglas y presenta su evidencia como pide S31-R25.

El dueño aceptó de antemano, el 2026-10-11, las recomendaciones de esta spec y pidió escribirla, aprobarla e implementarla en una sola pasada. Cada decisión queda marcada con esa fecha y conserva sus alternativas para poder revisarla. Aprobada e implementada el 2026-10-11: todos los criterios S31-A01 a S31-A14 tienen evidencia (ver "Verificación").

## Decisiones del dueño que esta spec toma como dadas

| Tema | Decisión | Fecha |
| --- | --- | --- |
| Inyección de dependencias | Manual con un `AppContainer`. Hilt descartado aunque lo sugiera la skill `testing-setup` (D4 de la Spec 30) | 2026-10-10 y 2026-10-11 (ADR-30-04) |
| Pruebas de base de datos | Instrumentadas en `src/androidTest`, con base de datos en memoria y el driver de producción. Sin Robolectric para Room | 2026-10-10 (Spec 20, opción A) |
| Pruebas con fakes | ViewModels y casos de uso se prueban con fakes de `ExerciseRepository` y `SessionRepository` y un `Clock.fixed`, sin Room ni emulador | 2026-10-11 (ADR-30-04, ADR-30-05, ADR-30-09) |
| Coroutines | `viewModelScope` sin dispatchers inyectados; las pruebas reemplazan `Dispatchers.Main` | 2026-10-11 (ADR-30-08) |
| UI | `<Nombre>Content` sin estado, que se prueba sin ViewModel | 2026-10-11 (ADR-30-07, S30-R31) |
| Navegación | Navigation 3; la pila es una lista que posee el shell | 2026-10-11 (ADR-30-10) |
| Módulos | Un solo módulo, `:app` | 2026-10-11 (ADR-30-02) |
| Prueba primero | El código de producción nuevo se escribe con la prueba primero (modo TDD estricto del dueño) | 2026-10-11 |
| Versiones | Se pueden ajustar por compatibilidad, anotando el motivo en la spec | 2026-10-11 (Spec 20) |

## Estado actual

Revisado el 2026-10-11 sobre el commit `f229bb9`.

| Elemento | En el código |
| --- | --- |
| Pruebas JVM (`src/test`) | 58 pruebas en 9 clases: dominio (Spec 10), mappers y conversores (Spec 20) y la plantilla `ExampleUnitTest` |
| Pruebas instrumentadas (`src/androidTest`) | 51 pruebas en 7 clases: esquema, migración, DAOs y repositorios Room (Spec 20) y la plantilla `ExampleInstrumentedTest` |
| Librerías de prueba | JUnit 4.13.2 en `testImplementation`. En `androidTestImplementation`: `androidx.test.ext:junit` 1.3.0, `espresso-core` 3.7.0, `room3-testing` 3.0.3, `kotlinx-coroutines-test` 1.11.0 y `ui-test-junit4` (BOM). `ui-test-manifest` en `debugImplementation` |
| Sin uso | `ui-test-junit4` está declarado, pero ninguna prueba lo usa |
| Fakes | No hay |
| `kotlinx-coroutines-test` en `src/test` | No está (costura de la Spec 30) |
| Comandos | `./gradlew assembleDebug test` (S03-R14); las instrumentadas aparte, con `./gradlew connectedAndroidTest` |
| AGP 9 | `android.onlyEnableUnitTestForTheTestedBuildType` es `true` por defecto, así que `./gradlew test` corre solo `testDebugUnitTest` |

## Pirámide de pruebas

| Tipo | Qué prueba | Dónde corre | Con qué | Entra con |
| --- | --- | --- | --- | --- |
| Dominio | Reglas, validaciones y preguntas puras de la Spec 10 | `src/test`, JVM | JUnit 4 | Ya existe (Spec 10) |
| Mappers y conversores | Entidades a modelos y vuelta | `src/test`, JVM | JUnit 4 | Ya existe (Spec 20) |
| Contrato en fakes | Que los fakes cumplen S20-R42 | `src/test`, JVM | JUnit 4, `kotlinx-coroutines-test` | Esta spec |
| Casos de uso | Validación y escritura, con fakes y `Clock.fixed` | `src/test`, JVM | JUnit 4, `kotlinx-coroutines-test` | F02, con los primeros casos de uso (S30-R20) |
| ViewModels | El `UiState` después de cada evento, con fakes, `Clock.fixed` y `MainDispatcherRule` | `src/test`, JVM | JUnit 4, `kotlinx-coroutines-test` | La primera spec con un ViewModel |
| Base de datos | Esquema, DAOs, restricciones, transacciones, migraciones y repositorios Room | `src/androidTest`, dispositivo | JUnit 4, `room3-testing`, `kotlinx-coroutines-test` | Ya existe (Spec 20) |
| Contrato en Room | Que Room cumple el mismo contrato que los fakes | `src/androidTest`, dispositivo | Lo mismo que la fila anterior | Esta spec |
| UI de Compose | `<Nombre>Content` con estados y eventos | `src/androidTest`, dispositivo | `ui-test-junit4` (reglas `v2`), `ui-test-manifest` | La primera spec con una pantalla |
| Navegación | Atrás y salida por la pantalla inicial en la pila del shell | `src/androidTest`, dispositivo | `ui-test-junit4`, Navigation 3 | F00 |
| Humo del arranque | La app arranca y muestra su contenido | `src/androidTest`, dispositivo | `ui-test-junit4` | Esta spec |
| Capturas | — | — | — | No en la primera versión (D2) |
| Cobertura | — | — | — | No en la primera versión (D3) |
| Extremo a extremo con UI Automator | — | — | — | No en la primera versión: ninguna función toca la UI del sistema |

La base es ancha y corre en la JVM: es rápida, no necesita dispositivo y cubre la lógica, que es lo que la constitución pide probar. Lo que necesita Android real (el SQLite del teléfono, la composición y la navegación) corre en el dispositivo, donde ya corren las pruebas de base de datos. La prueba de contrato une los dos lados: el comportamiento que los fakes dan en la JVM se comprueba también contra Room en el dispositivo.

## Requisitos

### Dónde corre cada prueba

| ID | Requisito |
| --- | --- |
| S31-R01 | Las pruebas que no necesitan el framework de Android viven en `src/test` y corren en la JVM con `./gradlew test`: dominio, mappers, conversores, casos de uso, ViewModels y fakes. |
| S31-R02 | Las pruebas que necesitan Android real viven en `src/androidTest` y corren en un teléfono o emulador con `./gradlew connectedDebugAndroidTest`: base de datos (Spec 20), contrato de los repositorios Room, UI de Compose, navegación y humo del arranque. No se usa Robolectric (D1). |
| S31-R03 | Todas las pruebas usan JUnit 4 y las aserciones de `org.junit.Assert`, como las que ya existen. No se agregan JUnit 5, librerías de aserciones ni frameworks de mocks (D5). Una dependencia que la prueba no puede construir se reemplaza con un fake de su interfaz. |

### Fakes y contrato de los repositorios

| ID | Requisito |
| --- | --- |
| S31-R04 | Los fakes y los contratos viven en `src/testFixtures/java/com/juancholopes/weightrack/testing/`, con `android { testFixtures { enable = true } }` en `app/build.gradle.kts` (D4). `src/test` y `src/androidTest` los ven sin configuración extra. Los fixtures declaran sus propias dependencias (ver "Dependencias"). |
| S31-R05 | `FakeExerciseRepository` implementa `ExerciseRepository` y `FakeSessionRepository` implementa `SessionRepository`. Los dos reciben por constructor un `InMemoryStore`, que hace de base de datos y por defecto es uno nuevo. Compartir el mismo `InMemoryStore` entre los dos fakes reproduce las tablas comunes de Room. |
| S31-R06 | Los fakes cumplen el contrato de S20-R42 tal como lo cumplen `RoomExerciseRepository` y `RoomSessionRepository`, según la tabla "Contrato que cumplen los fakes". Las lecturas se responden con las funciones del dominio de la Spec 10, no con copias de su lógica. |
| S31-R07 | El contrato se escribe una sola vez, como clases de prueba abstractas en los fixtures (`ExerciseRepositoryContract` y `SessionRepositoryContract`), y corre dos veces: contra los fakes en `src/test` (`FakeExerciseRepositoryTest`, `FakeSessionRepositoryTest`) y contra Room en `src/androidTest` (`RoomExerciseRepositoryContractTest`, `RoomSessionRepositoryContractTest`), con una base en memoria nueva por prueba (S20-R46). Las pruebas de la Spec 20 no cambian. |
| S31-R08 | Una spec F que agrega una consulta o una escritura a una interfaz (S30-R17) agrega, en la misma tarea, su prueba al contrato y su implementación al fake. Un fake no tiene comportamiento que el contrato no pruebe. Si un ViewModel necesita simular un fallo inesperado (S30-R23), la spec F agrega al fake una forma explícita de provocarlo, con su prueba. |

### Contrato que cumplen los fakes

| Tema | Comportamiento, igual en Room y en los fakes |
| --- | --- |
| Ids | Crear ignora el `id` recibido y asigna uno nuevo, mayor que cualquiera que haya existido en esa tabla, aunque se haya borrado (S20-R17, S10-R14). Los hijos (músculos y series) reciben el id del padre. |
| Nombre único | Crear o editar un ejercicio con el nombre normalizado de otro devuelve `Rejected(NAME_TAKEN)` o, si el otro está archivado, `Rejected(NAME_TAKEN_BY_ARCHIVED)`, y no escribe nada (S20-R41). La clave sale de `normalizeExerciseName` y `nameKey` (S20-R12). Cambiar solo las mayúsculas del propio nombre se guarda. |
| Restricciones | Una clave foránea rota, un músculo repetido en un ejercicio o un orden repetido en una sesión hacen fallar la escritura con una excepción, sin escribir nada (S20-R20 a S20-R26, S20-R38, S20-R39). |
| Borrar o archivar | `delete` archiva un ejercicio con series y borra uno sin series junto con sus músculos (S20-R40). Un id inexistente lanza `IllegalStateException`. |
| Cascada | Borrar una sesión borra sus series y no cambia los ejercicios (S20-R20). |
| Sin fila | `updateSession`, `deleteSession`, `updateSet`, `deleteSet` y `unarchive` con un id inexistente no hacen nada. `update` de un ejercicio inexistente falla con una excepción. |
| Orden de las lecturas | El de las preguntas de la Spec 10: catálogo y archivados por nombre sin distinguir mayúsculas, historial por fecha e id descendentes, series por orden, última vez y evolución como en S10-R25 a S10-R27 (S20-R29 a S20-R34). |
| Flows | Cada lectura emite el valor actual al suscribirse y vuelve a emitir después de cada escritura que lo cambia (S20-A33). |

### Coroutines, reloj, ViewModels y casos de uso

| ID | Requisito |
| --- | --- |
| S31-R09 | `kotlinx-coroutines-test` se declara en `testImplementation` y en `testFixturesImplementation` con la referencia `kotlinxCoroutines` del catálogo (1.11.0), la misma de `kotlinx-coroutines-core`. Ninguna configuración resuelve otra versión de `kotlinx-coroutines-*`. |
| S31-R10 | `MainDispatcherRule` vive en `src/test/java/com/juancholopes/weightrack/testing/`. Es la regla de la guía oficial: reemplaza `Dispatchers.Main` con un `TestDispatcher` (`UnconfinedTestDispatcher` por defecto) antes de cada prueba y lo restablece después. Toda prueba de ViewModel la declara con `@get:Rule`. Las pruebas instrumentadas no la usan, porque allí existe el hilo principal real. |
| S31-R11 | Una prueba de ViewModel construye el ViewModel con fakes, con casos de uso reales sobre fakes y con un `Clock.fixed(...)`. Nunca usa Room, el `AppContainer` ni la fábrica del ViewModel. Corre en `runTest`, llama a las funciones `on<Evento>()` y comprueba `uiState.value`. Si el estado sale de `stateIn(..., WhileSubscribed(...))`, la prueba arranca antes un colector vacío en `backgroundScope` con `UnconfinedTestDispatcher(testScheduler)` (D8). Cada ViewModel tiene al menos una prueba así (S30-A15). |
| S31-R12 | Una prueba de caso de uso lo construye con fakes y `Clock.fixed(...)` y corre en `runTest`, sin `MainDispatcherRule`. Comprueba el resultado sellado y lo que quedó guardado en el fake. |
| S31-R13 | Ninguna prueba depende del reloj ni de la zona horaria del equipo. "Hoy" sale de `Clock.fixed(Instant, ZoneOffset)` y las fechas se escriben con `LocalDate.of(...)`. |

### UI, navegación y humo

| ID | Requisito |
| --- | --- |
| S31-R14 | Las pruebas de UI de Compose prueban `<Nombre>Content` con un `UiState` armado en la prueba y lambdas que registran las llamadas. No crean ViewModels, repositorios ni el `AppContainer`. Usan las reglas del paquete `androidx.compose.ui.test.junit4.v2` (`createComposeRule()` o `createAndroidComposeRule<ComponentActivity>()`), porque las de v1 están deprecadas. |
| S31-R15 | Las pruebas de UI buscan los nodos por semántica: texto de `strings.xml` leído desde la actividad, descripción de contenido o rol. Un `testTag` se usa solo si encontrar el nodo exige más de tres matchers (skill `testing-setup`). Si la pantalla guarda estado con `rememberSaveable`, una prueba lo comprueba con `StateRestorationTester`. |
| S31-R16 | F00 prueba la navegación del shell en `src/androidTest`: atrás saca la entrada superior de la pila, y atrás en la pantalla inicial sale de la app (o vuelve a la pestaña inicial si la Spec 42 elige varias pilas). No hay deep links en la primera versión. |
| S31-R17 | `MainActivitySmokeTest` arranca `MainActivity` y comprueba que muestra el nombre de la app. Es la única prueba de extremo a extremo de la primera versión. F00 la adapta para comprobar su pantalla inicial. |
| S31-R18 | Ninguna prueba abre ni escribe el archivo `weightrack.db` de la app. Las de base de datos usan `Room.inMemoryDatabaseBuilder` (S20-R46), salvo la de migración, que usa su propio archivo `migration-test.db` (S20-R45); las de UI usan `Content` con estados; las de navegación arman el shell sin escribir datos. |

### Convenciones y flujo de trabajo

| ID | Requisito |
| --- | --- |
| S31-R19 | Una clase de prueba se llama `<ClaseProbada>Test` y vive en el mismo paquete que la clase que prueba (S03-R08). La que corre un contrato contra una implementación que ya tiene `<Implementación>Test` se llama `<Implementación>ContractTest`. Las clases abstractas de contrato se llaman `<Interfaz>Contract`. |
| S31-R20 | Los métodos de prueba nuevos se nombran `sujeto_condición_resultado`, en camelCase y separados por `_`, por ejemplo `create_withNameOfArchived_returnsNameTakenByArchived` (D7). Sin espacios ni comillas invertidas. Las pruebas existentes conservan sus nombres. |
| S31-R21 | Una prueba que es evidencia de un criterio lleva encima un comentario con el criterio y el requisito, por ejemplo `// S20-A20 / S20-R41`. El criterio nombra la prueba como `Clase.método`. |
| S31-R22 | Todo código de producción nuevo se escribe con la prueba primero, tarea por tarea: (1) escribir la prueba del criterio, (2) correrla y verla fallar por el motivo esperado, (3) escribir el código mínimo para que pase y (4) refactorizar con las pruebas en verde. Dentro de un corte vertical el orden es dominio y casos de uso, ViewModel, `Content` y navegación. Una prueba escrita después del código solo se acepta para caracterizar código que ya existe, como el contrato contra Room. |

### Comandos y evidencia

| ID | Requisito |
| --- | --- |
| S31-R23 | Comando sin dispositivo: `./gradlew assembleDebug test`. Compila la app y corre todas las pruebas JVM. Es el ciclo diario y el que puede correr cualquier agente. |
| S31-R24 | Comando completo: `./gradlew assembleDebug test connectedDebugAndroidTest`, con un teléfono o emulador conectado. Es el "un comando" de la definición de hecho de la constitución (D6): una spec está hecha solo si pasa desde cero, en una copia limpia del repositorio (como S03-A09). |
| S31-R25 | Cada spec F presenta su evidencia así: cada criterio nombra su prueba (`Clase.método`) o su comando, y la sección "Verificación" lista los comandos ejecutados, su resultado y, por tarea de Gradle, las pruebas, fallos, errores y omitidas leídos de `app/build/test-results/` (JVM) y de `app/build/outputs/androidTest-results/connected/` (instrumentadas). Las instrumentadas anotan el modelo y la API del dispositivo. Sin dispositivo, los criterios instrumentados quedan "Pendiente de dispositivo" y la spec no pasa a Implementada. |
| S31-R26 | Se eliminan las pruebas de plantilla `ExampleUnitTest` y `ExampleInstrumentedTest`, que no prueban nada de la app. `MainActivitySmokeTest` (S31-R17) reemplaza a la instrumentada. |
| S31-R27 | Las librerías de prueba son solo las de "Dependencias" y sus versiones viven en `gradle/libs.versions.toml` (S30-R41, S30-R42). Agregar otra exige enmendar esta spec. |

## Decisiones

Todas tomadas el 2026-10-11 con la opción recomendada, porque el dueño aceptó de antemano las recomendaciones. Las alternativas quedan para poder revisarlas.

### D1. Robolectric para las pruebas de UI

La skill `testing-setup` pide Robolectric para correr las pruebas de UI en `src/test`. La Spec 20 lo descartó solo para la base de datos. El 2026-10-11 se probó en este proyecto Robolectric 4.17 (10 de septiembre de 2026, la primera versión con SDK 37) con una prueba de Compose sobre `MainActivity`:

- Con el JDK 25 del toolchain falla al iniciar, con `IllegalAccessException` sobre `jdk.internal.access.SharedSecrets`. Hay que pasar `--add-exports` y `--add-opens` a la JVM de pruebas.
- Con SDK 37 falla al sincronizar la UI: Espresso 3.7.0, la última estable, llama por reflexión a `InputManager.getInstance()`, que ya no existe en SDK 37. Hay que fijar `sdk=36`, distinto del `targetSdk` 37.
- Con esos dos ajustes pasa. La primera corrida descargó unos 430 MB de imágenes de Android en `~/.m2` y esa prueba tardó 35 s.

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Sin Robolectric; UI, navegación y humo en `src/androidTest` | Ninguna dependencia nueva: `ui-test-junit4` ya está declarado. Corre en el Android real de destino, con la misma composición que la app. Las pruebas de base de datos ya exigen un dispositivo | Las pruebas de UI quedan fuera de `./gradlew test` y necesitan un teléfono o emulador. Cada corrida instala dos APK; en algunos teléfonos Xiaomi la instalación por USB pide aprobación en pantalla. Ciclo más lento |
| B. Robolectric 4.17 en `src/test` | Las pruebas de UI corren sin dispositivo, en el comando diario. Es lo que pide la skill | Tres ajustes frágiles: flags de la JVM, `sdk=36` fijo e `isIncludeAndroidResources`. Prueba un SDK distinto del de destino. Descarga cientos de MB fuera de la caché de Gradle. Se rompe con cada salto de JDK o de SDK hasta que Robolectric y Espresso se pongan al día |

**Decidido el 2026-10-11: A, sin Robolectric en la primera versión (el dueño aceptó de antemano las recomendaciones).** Se reconsidera con una enmienda de esta spec si una versión de Robolectric y Espresso corre SDK 37 sin fijar el SDK ni pasar flags a la JVM, o si el dueño mide que el ciclo en el dispositivo frena el trabajo con la prueba primero.

### D2. Pruebas de capturas

La skill pide Compose Preview Screenshot Testing, y Dropshots para capturas en el dispositivo. Con AGP 9.5.0-alpha03 o más se configura con test suites de AGP; con un AGP menor, con el plugin `com.android.compose.screenshot`, que la propia skill marca como deprecado, y pide no actualizar AGP solo para eso. El proyecto usa AGP 9.4.1 y AGP 9.5 no tiene versión estable. La herramienta sigue en alfa (0.0.1-alpha16). Hoy la única UI es un `Scaffold` con el nombre de la app, que F00 reemplaza, y todavía no hay sistema de diseño (Spec 41).

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Sin capturas en la primera versión; se reconsidera con la Spec 41 | Nada que mantener. Sin imágenes de referencia que F00 invalidaría enseguida. Sin herramienta alfa ni deprecada | Un cambio visual no deseado solo se detecta a mano |
| B. Compose Preview Screenshot Testing con el plugin independiente | Herramienta de Google. Usa las previews. Corre en la JVM | Plugin deprecado en cuanto el proyecto use AGP 9.5. Alfa. Exige una bandera experimental. Habría que migrar a test suites |
| C. Roborazzi | Corre en la JVM | Exige Robolectric (D1) y un plugin de terceros |
| D. Dropshots | Capturas reales en el dispositivo | Plugin de terceros. Imágenes que dependen del dispositivo. Sin necesidad hoy |

**Decidido el 2026-10-11: A, sin capturas en la primera versión (el dueño aceptó de antemano las recomendaciones).** La Spec 41 decide si entran, de preferencia con test suites de AGP cuando el proyecto use AGP 9.5 o más.

### D3. Cobertura

La skill pide Jacoco para la cobertura de las pruebas locales.

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Sin informe de cobertura | Nada que configurar. La calidad se mide con criterios de aceptación que nombran sus pruebas (constitución, regla 6) y con la prueba primero | No hay un número para ver zonas sin probar |
| B. Cobertura integrada de AGP (`enableUnitTestCoverage = true` en `debug` y la tarea `createDebugUnitTestCoverageReport`) | Sin plugin nuevo: usa el Jacoco que trae AGP | Nadie consume el informe hoy. Un porcentaje no dice si se prueba lo importante |
| C. Plugin `jacoco` con una tarea propia que une pruebas JVM e instrumentadas | Informe completo | Configuración de Gradle propia que mantener. Sin consumidor |

**Decidido el 2026-10-11: A, sin informe de cobertura (el dueño aceptó de antemano las recomendaciones).** Si el dueño quiere medirla, B es el primer paso y no agrega dependencias.

### D4. Dónde viven los fakes

Los fakes los necesitan hoy sus propias pruebas, y después las pruebas de ViewModels y casos de uso. La única forma de demostrar que un fake se comporta como Room es correr las mismas pruebas contra los dos, y Room solo corre en el dispositivo (Spec 20). AGP 9.0 trae soporte completo de Android Studio para los test fixtures. Probado el 2026-10-11 en este proyecto: `testFixtures { enable = true }` funciona en el módulo de aplicación con AGP 9.4.1 y Kotlin integrado; el código de los fixtures ve lo `internal` de `src/main` (por ejemplo `nameKey`), y `src/test` y `src/androidTest` ven los fixtures sin configuración extra.

| Opción | A favor | En contra |
| --- | --- | --- |
| A. `src/testFixtures`, con pruebas de contrato compartidas | Una sola copia de los fakes y del contrato, visible desde `src/test` y `src/androidTest`. El mismo contrato corre contra Room en el dispositivo: si un fake se aparta de Room, una prueba falla. Android Studio lo entiende desde AGP 9 | Un bloque más en `app/build.gradle.kts` y dependencias propias de los fixtures, incluido el runtime de Compose solo porque lo exige el plugin del compilador. La DSL está marcada como incubadora |
| B. `src/test`, en un paquete `testing` | Cero configuración | Las pruebas instrumentadas no ven los fakes. No hay forma de comprobarlos contra Room: el contrato quedaría solo en lo que el fake cree |
| C. Carpeta `src/sharedTest` agregada a `test` y a `androidTest` | Comparte código sin fixtures | Android Studio no indexa bien una misma carpeta en dos source sets del mismo módulo (aviso de raíces de contenido duplicadas) |
| D. Un módulo `:testing` | Patrón habitual en proyectos grandes | Un segundo módulo exige reemplazar ADR-30-02 |

**Decidido el 2026-10-11: A, fakes y contratos en `src/testFixtures` (el dueño aceptó de antemano las recomendaciones).**

### D5. Mocks

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Ningún framework de mocks; solo fakes | Lo piden la Spec 30 y la skill ("MockK solo si es claramente necesario"). Las interfaces del dominio son pocas y pequeñas. Los fakes comprueban estado, no llamadas, y el contrato los mantiene fieles a Room | Hay que escribir y mantener los fakes |
| B. MockK | Dobles rápidos de escribir | Prueba llamadas en lugar de comportamiento, así que las pruebas se rompen al refactorizar. Dependencia sin necesidad |

**Decidido el 2026-10-11: A, solo fakes (el dueño aceptó de antemano las recomendaciones).** Se reconsidera solo si una dependencia no se puede envolver en una interfaz del proyecto.

### D6. El comando de la definición de hecho

La constitución pide que el build y las pruebas pasen desde cero con un comando. S03-R14 usa `./gradlew assembleDebug test` y deja las instrumentadas aparte. Las pruebas de base de datos ya son instrumentadas.

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Dos niveles: `./gradlew assembleDebug test` sin dispositivo para el día a día, y `./gradlew assembleDebug test connectedDebugAndroidTest` con dispositivo como comando de la definición de hecho | Un solo comando cubre build, pruebas JVM e instrumentadas. El nivel sin dispositivo sigue siendo el ciclo rápido que cualquier agente puede correr | Cerrar una spec exige un dispositivo |
| B. Solo `./gradlew assembleDebug test` | No necesita dispositivo | Deja fuera la base de datos y la UI, así que no demuestra que las pruebas pasan |
| C. `./gradlew build connectedCheck` | Tareas estándar de Gradle; incluye lint | Compila también `release` con R8 y corre lint en cada ciclo: más lento. El lint ya tiene su propio criterio (S03-R20) |

**Decidido el 2026-10-11: A, dos niveles (el dueño aceptó de antemano las recomendaciones).**

### D7. Nombres de prueba

| Opción | A favor | En contra |
| --- | --- | --- |
| A. `sujeto_condición_resultado` en camelCase | Es el estilo de las pruebas y criterios de la Spec 20. Se lee bien en el informe. Vale en la JVM y en el dispositivo | Nombres largos |
| B. Frases con comillas invertidas | Muy legibles | No valen en `src/androidTest` con `minSdk` 26: D8 rechaza espacios en nombres de método antes de DEX 040 (API 30). Habría dos estilos según el source set |
| C. `given_when_then` | Estructura explícita | Más largo. No lo usa ninguna prueba existente |

**Decidido el 2026-10-11: A, `sujeto_condición_resultado` (el dueño aceptó de antemano las recomendaciones).** Las pruebas existentes no se renombran.

### D8. Cómo se observan los flows en las pruebas

| Opción | A favor | En contra |
| --- | --- | --- |
| A. Leer `uiState.value` y, con `stateIn(WhileSubscribed)`, arrancar un colector vacío en `backgroundScope` | Es la forma de la guía oficial "Test Kotlin flows on Android", que recomienda tratar `StateFlow` como contenedor de datos. Sin dependencias | Para comprobar emisiones intermedias hay que recolectar a mano |
| B. Turbine | API cómoda para recorrer emisiones | Librería de terceros sin necesidad: el estado único de ADR-30-07 se comprueba con `.value` |

**Decidido el 2026-10-11: A, `.value` y colector en `backgroundScope` (el dueño aceptó de antemano las recomendaciones).**

## Skills consultadas

Por la regla 9, cada choque con una spec queda como decisión, con su motivo.

| Skill (actualizada) | Qué dice | Efecto en esta spec | ¿Choque? |
| --- | --- | --- | --- |
| `testing-setup` (2026-09-23) | Si no hay framework de inyección, instalar Hilt | No se instala | Sí. Resuelto en D4 de la Spec 30: inyección manual |
| `testing-setup` | JUnit 4 para pruebas locales e instrumentadas | Se mantiene JUnit 4 (S31-R03) | No |
| `testing-setup` | Robolectric para las pruebas de UI, que viven en `src/test` | Las pruebas de UI viven en `src/androidTest` | Sí. Resuelto en D1 |
| `testing-setup` | Compose Preview Screenshot Testing (plugin deprecado con AGP menor que 9.5) y Dropshots | Sin capturas en la primera versión | Sí. Resuelto en D2 |
| `testing-setup` | Jacoco para la cobertura | Sin cobertura | Sí. Resuelto en D3 |
| `testing-setup` | Fakes primero; MockK solo si es claramente necesario | Solo fakes, en `src/testFixtures`, que es un source set de pruebas | No (D4, D5) |
| `testing-setup` | Pruebas unitarias de ViewModels, repositorios y DAOs; no de Activities, layouts ni configuración de inyección | S31-R01, S31-R11; los DAOs, en el dispositivo por la Spec 20 | No |
| `testing-setup` | Base de datos con pruebas instrumentadas y base en memoria | Ya lo hace la Spec 20 | No |
| `testing-setup` | Pruebas de UI con `ComposeTestRule` y `ComponentActivity`, matchers semánticos primero, `testTag` con más de tres matchers, restauración de estado | S31-R14, S31-R15 | No |
| `testing-setup` | Pruebas de navegación: atrás, deep links y salida por la pantalla inicial | S31-R16, sin deep links en la primera versión | No |
| `testing-setup` | `DeviceConfigurationOverride` para tamaños de ventana y escala de fuente | No en la primera versión: solo teléfono (Spec 01). La Spec 41 decide si prueba la escala de fuente 1,5 | No; se difiere |
| `testing-setup` | Pocas pruebas de extremo a extremo; UI Automator solo para la UI del sistema | Una prueba de humo (S31-R17) | No |
| `testing-setup` | Documentar la estrategia en `AGENTS.md` o `docs/testing.md` | Esta spec es el documento; no se crean esos archivos | Sí, leve. Las specs son la documentación del proyecto (constitución) |

## Dependencias

Versiones verificadas el 2026-10-11 en Maven Central, Google Maven y las páginas oficiales de versiones. Ninguna cambia la línea de Kotlin, de Compose ni de Coroutines. Solo se agrega una entrada al catálogo, `androidx-compose-runtime`, sin versión propia porque la fija la BOM.

| Artefacto | Versión | Uso | Motivo | Fuente |
| --- | --- | --- | --- | --- |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.11.0 (8 de mayo de 2026), ya en el catálogo; es la última estable | Se agrega a `testImplementation` y `testFixturesImplementation`. Sigue en `androidTestImplementation` | `runTest` en los contratos y `MainDispatcherRule`. Misma versión que `kotlinx-coroutines-core`, para no repetir el desfase de versiones de la Spec 20 | [kotlinx.coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases), Maven Central |
| `junit:junit` | 4.13.2, ya en el catálogo; es la última de JUnit 4 | Se agrega a `testFixturesImplementation` | Los contratos usan `@Test`, `@Before` y `Assert` | Maven Central |
| `androidx.compose.runtime:runtime` | 1.12.1, por la BOM 2026.09.00 (S03-R02) | `testFixturesImplementation`, con `platform(libs.androidx.compose.bom)` | El plugin del compilador de Compose se aplica a todas las compilaciones de Kotlin de `:app` y falla si no encuentra el runtime, aunque los fixtures no tengan composables (comprobado el 2026-10-11). Ya llega a la app de forma transitiva | [Compose BOM](https://developer.android.com/develop/ui/compose/bom/bom-mapping), Google Maven |
| `androidx.compose.ui:ui-test-junit4` | 1.12.1, por la BOM; ya declarado | `androidTestImplementation`. Lo usa ahora `MainActivitySmokeTest` | Reglas `v2` (S31-R14) | [Migrate to v2 testing APIs](https://developer.android.com/develop/ui/compose/testing/migrate-v2) |
| `androidx.compose.ui:ui-test-manifest` | Por la BOM; ya declarado | `debugImplementation` | `createAndroidComposeRule<ComponentActivity>()` en las pruebas de `Content` | [Compose testing common patterns](https://developer.android.com/develop/ui/compose/testing/common-patterns) |
| `androidx.test.ext:junit` y `androidx.test.espresso:espresso-core` | 1.3.0 y 3.7.0, ya declarados; son las últimas estables | `androidTestImplementation` | Sin cambio | Google Maven |
| `androidx.room3:room3-testing` | 3.0.3, ya declarado | `androidTestImplementation` | Sin cambio (Spec 20) | Spec 20 |

El APK de prueba hereda las versiones de ejecución de la app (hechos de la Spec 20). Las dependencias de los fixtures no crean desfases: `kotlinx-coroutines-test` 1.11.0 coincide con el `kotlinx-coroutines-core` 1.11.0 de la app y el runtime de Compose sale de la misma BOM.

No se agregan:

- Robolectric 4.17 (D1).
- Compose Preview Screenshot Testing 0.0.1-alpha16, Roborazzi ni Dropshots (D2).
- El plugin `jacoco` ni `enableUnitTestCoverage` (D3).
- MockK ni Mockito (D5).
- Turbine (D8).
- `hilt-android-testing` (ADR-30-04).
- UI Automator: no hay pruebas de la UI del sistema.
- JUnit 5, Truth ni AssertJ (S31-R03).

## Fuera de alcance de esta spec

- Las pruebas de cada feature: las escribe su spec F con estas reglas.
- Pruebas de capturas y de escala de fuente: Spec 41 (D2).
- Layouts adaptables y tamaños de ventana: fuera de la primera versión (Spec 01).
- Integración continua: no hay servidor de CI.
- Dispositivos administrados por Gradle (Gradle Managed Devices): la Spec 20 los dejó como opción que habría que aprobar.
- Cobertura (D3).
- Registro de fallos del agente: sigue abierto en la constitución.

## Criterios de aceptación

S31-A01 a S31-A14 se comprueban sobre esta implementación. S31-A15 lo comprueba cada spec F. Las rutas de prueba parten de `app/src/`.

| ID | Criterio | Comprueba | Evidencia |
| --- | --- | --- | --- |
| S31-A01 | Los fixtures están activados y contienen los fakes y los contratos. | S31-R04, S31-R05, S31-R07 | `grep -n -A1 "testFixtures {" app/build.gradle.kts` muestra `enable = true`; `find app/src/testFixtures -name "*.kt"` lista `InMemoryStore.kt`, `FakeExerciseRepository.kt`, `FakeSessionRepository.kt`, `RepositoryContract.kt`, `ExerciseRepositoryContract.kt` y `SessionRepositoryContract.kt` |
| S31-A02 | Los fakes cumplen el contrato en la JVM. | S31-R05, S31-R06, S31-R07 | `FakeExerciseRepositoryTest` y `FakeSessionRepositoryTest`, con `./gradlew test`, sin fallos |
| S31-A03 | Room cumple el mismo contrato en el dispositivo. | S31-R06, S31-R07 | `RoomExerciseRepositoryContractTest` y `RoomSessionRepositoryContractTest`, con `./gradlew connectedDebugAndroidTest`, sin fallos |
| S31-A04 | `kotlinx-coroutines-test` está en las pruebas JVM y en los fixtures, y todas las configuraciones de prueba resuelven `kotlinx-coroutines-*` 1.11.0. | S31-R09 | `grep -n "coroutines.test" app/build.gradle.kts`; `./gradlew :app:dependencies --configuration debugUnitTestRuntimeClasspath` y `--configuration debugAndroidTestRuntimeClasspath`, sin otra versión de `kotlinx-coroutines` |
| S31-A05 | `MainDispatcherRule` reemplaza `Dispatchers.Main` y comparte el reloj virtual de `runTest`. | S31-R10 | `MainDispatcherRuleTest.coroutineLaunchedOnMain_runsOnTheTestScheduler` |
| S31-A06 | La app arranca en el dispositivo y muestra su nombre. | S31-R17 | `MainActivitySmokeTest.launch_showsAppName` |
| S31-A07 | El build no tiene Robolectric, mocks, Turbine, capturas, Jacoco ni JUnit 5. | S31-R02, S31-R03, S31-R27 | `grep -rniE "robolectric\|mockk\|mockito\|turbine\|roborazzi\|dropshots\|screenshot\|jacoco\|jupiter\|enableUnitTestCoverage" gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts gradle.properties` sin resultados |
| S31-A08 | Las pruebas de UI usan las reglas `v2` de Compose. | S31-R14 | `grep -rnE "import androidx\.compose\.ui\.test\.junit4\.create" app/src` sin resultados |
| S31-A09 | Ninguna prueba usa el reloj del sistema ni el archivo de base de datos de la app. | S31-R13, S31-R18 | `grep -rnE "LocalDate\.now\(\)\|Instant\.now\(\)\|System\.currentTimeMillis\|Clock\.system" app/src/test app/src/androidTest app/src/testFixtures` y `grep -rnE "WeightrackDatabase\.NAME\|weightrack\.db" app/src/test app/src/androidTest app/src/testFixtures`, los dos sin resultados |
| S31-A10 | Las pruebas nuevas siguen las convenciones de nombres y no quedan pruebas de plantilla. | S31-R19, S31-R20, S31-R26 | `find app/src -name "Example*Test.kt"` sin resultados; `grep -rn 'fun `' app/src/test app/src/androidTest app/src/testFixtures` sin resultados; `grep -rhA1 "@Test" app/src/testFixtures app/src/test/java/com/juancholopes/weightrack/testing app/src/androidTest/java/com/juancholopes/weightrack/MainActivitySmokeTest.kt \| grep -oE "fun [A-Za-z0-9_]+" \| grep -vE "fun [a-z][A-Za-z0-9]*(_[a-z][A-Za-z0-9]*)+$"` sin resultados; revisión de los nombres de las clases nuevas |
| S31-A11 | El comando sin dispositivo pasa desde cero. | S31-R23 | `./gradlew assembleDebug test` en una copia limpia de los archivos no ignorados del repositorio |
| S31-A12 | El comando completo pasa con un dispositivo. | S31-R24 | `./gradlew assembleDebug test connectedDebugAndroidTest` con un teléfono conectado |
| S31-A13 | Los criterios S30-A01 a S30-A11 siguen pasando. | S30-R04 a S30-R41 | Los comandos de la Spec 30, tal como están escritos |
| S31-A14 | Cada choque con una skill está registrado como decisión, y cada decisión conserva sus alternativas. | Constitución, regla 9 | Revisión de "Decisiones" y "Skills consultadas" |
| S31-A15 | Cada spec F escribe sus pruebas según S31-R11 a S31-R16 y S31-R22 y presenta su evidencia según S31-R25. | S31-R11 a S31-R16, S31-R22, S31-R25 | Revisión de cada spec F |

## Verificación

Hecha el 2026-10-11 sobre el árbol de trabajo: commit `f229bb9` más los cambios de esta spec. Cada comando se ejecutó tal como está en "Criterios de aceptación", desde la raíz del proyecto. El dispositivo fue un Xiaomi 24117RN76L con Android 16 (API 36), conectado por USB.

| ID | Resultado | Evidencia |
| --- | --- | --- |
| S31-A01 | Pasa | `grep` muestra `40: testFixtures {` y `41: enable = true`. `find` lista los seis archivos. |
| S31-A02 | Pasa | `FakeExerciseRepositoryTest`: 18 pruebas. `FakeSessionRepositoryTest`: 20. Sin fallos, errores ni omitidas. Antes de escribir los fakes, las 38 fallaron: primero por compilación y después, con fakes vacíos, con `NotImplementedError`. |
| S31-A03 | Pasa | En el Xiaomi: `RoomExerciseRepositoryContractTest`, 18 pruebas, y `RoomSessionRepositoryContractTest`, 20, sin fallos. |
| S31-A04 | Pasa | `grep` devuelve las líneas 65 (`testImplementation`), 68 (`testFixturesImplementation`) y 77 (`androidTestImplementation`). En `debugUnitTestRuntimeClasspath`, `debugAndroidTestRuntimeClasspath` y `debugTestFixturesRuntimeClasspath`, todas las `kotlinx-coroutines-*` resuelven 1.11.0; las que piden 1.7.3, 1.8.1 o 1.9.0 suben a 1.11.0. |
| S31-A05 | Pasa | 1 prueba sin fallos. Con una regla vacía falló con "Dispatchers.Main was accessed when the platform dispatcher was absent and the test dispatcher was unset". |
| S31-A06 | Pasa | 1 prueba sin fallos en el Xiaomi. |
| S31-A07 | Pasa | Sin resultados. |
| S31-A08 | Pasa | Sin resultados. El único import de reglas es `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`, en `MainActivitySmokeTest`. |
| S31-A09 | Pasa | Las dos búsquedas, sin resultados. |
| S31-A10 | Pasa | `find` y la búsqueda de comillas invertidas, sin resultados. Los 40 métodos `@Test` nuevos cumplen el patrón. Clases nuevas: `FakeExerciseRepositoryTest`, `FakeSessionRepositoryTest`, `MainDispatcherRuleTest`, `RoomExerciseRepositoryContractTest`, `RoomSessionRepositoryContractTest` y `MainActivitySmokeTest`; contratos `ExerciseRepositoryContract` y `SessionRepositoryContract`. |
| S31-A11 | Pasa | Copia limpia de los archivos no ignorados en una carpeta nueva, con `ANDROID_HOME` en lugar del `local.properties` ignorado: `BUILD SUCCESSFUL`, 50 tareas ejecutadas, 96 pruebas en 11 clases, sin fallos, errores ni omitidas. |
| S31-A12 | Pasa | En otra copia limpia, el comando completo: `BUILD SUCCESSFUL`, 89 tareas ejecutadas. JVM: 96 pruebas en 11 clases. Instrumentadas, en el Xiaomi: 89 pruebas en 9 clases. Sin fallos, errores ni omitidas. |
| S31-A13 | Pasa | S30-A01: solo `26:include(":app")`. S30-A02, A03, A04 y A07: sin resultados. S30-A05: tres líneas, todas de `AppContainer.kt` (21, 27 y 31). S30-A06: solo `AppContainer.kt` y `WeightrackApplication.kt`. S30-A08: sin resultados. S30-A09: `grep -c` devuelve 1 y no hay `androidx.fragment`. S30-A10: solo `versionName = "1.0"`; los bloques `plugins` usan `alias`, con `apply false` en la raíz. S30-A11: la corrida de S31-A11 en copia limpia. |
| S31-A14 | Pasa | D1 a D8 conservan sus alternativas. Los choques con `testing-setup` están en "Skills consultadas" y remiten a D1, D2 y D3, y a D4 de la Spec 30. |
| S31-A15 | No aplica todavía | Lo comprueba cada spec F. |

Las dos corridas instrumentadas (S31-A03, S31-A06 y S31-A12) terminaron antes de que el dueño desconectara el teléfono. Después se volvió a correr solo el nivel sin dispositivo en el árbol de trabajo, `./gradlew assembleDebug test --rerun-tasks`: `BUILD SUCCESSFUL`, 96 pruebas en 11 clases, sin fallos. La próxima corrida instrumentada necesita un dispositivo conectado.

Además, `./gradlew :app:lintDebug` termina sin errores. Solo quedan los avisos de plantilla que acepta S03-R20 (versión de AGP, etiqueta redundante y recursos sin usar) y ninguno en código de pruebas.

## Hechos de la implementación

Comprobados al implementar el 2026-10-11:

- `testFixtures { enable = true }` funciona en el módulo de aplicación con AGP 9.4.1 y Kotlin integrado. AGP crea `compileDebugTestFixturesKotlin` (y `kspDebugTestFixturesKotlin`, que no genera nada). Los fixtures ven lo `internal` de `src/main`: `InMemoryStore` usa `nameKey`, igual que el mapper de la Spec 20 (S20-R12).
- Sin el runtime de Compose en los fixtures, la compilación falla con "The Compose Compiler requires the Compose Runtime to be on the class path". Por eso los fixtures declaran `androidx.compose.runtime:runtime` con la BOM.
- Sin `MainDispatcherRule`, la prueba de la regla falla con "Dispatchers.Main was accessed when the platform dispatcher was absent and the test dispatcher was unset". Así se vio el paso en rojo de S31-R22.
- En `ui-test-junit4` 1.12.1, `createAndroidComposeRule` de `androidx.compose.ui.test.junit4` sale deprecada en favor del paquete `v2` (aviso del compilador). `MainActivitySmokeTest` usa `v2`.
- Room pasó el contrato en la primera corrida: no apareció ninguna diferencia entre los fakes y Room.
- Los fakes guardan los ejercicios tal como llegan y ordenan el catálogo con `activeCatalog` del dominio, que compara `nameKey(name)`. Room ordena por `normalized_name`. Coinciden para los nombres que guarda la app, que el dominio ya normalizó (S10-R04).
- `FakeExerciseRepository.update` comprueba el nombre antes que la existencia del ejercicio, en el mismo orden en que Room informa la colisión (S20-R41).
- `connectedDebugAndroidTest` desinstala la app al terminar: estaba instalada antes de la corrida y no después (`adb shell pm list packages`). Desinstalar borra sus datos. Hoy no hay datos reales que perder, porque la app todavía no registra nada.
- Las pruebas de plantilla se eliminaron. Las cuentas quedan así: JVM, de 58 pruebas en 9 clases a 96 en 11; instrumentadas, de 51 en 7 clases a 89 en 9.
- Robolectric se probó y se descartó antes de implementar, con los hallazgos de D1. No quedó ningún rastro de esa prueba en el build.

### Riesgos abiertos

- **Datos reales y pruebas instrumentadas.** Desde F01, si el dueño registra entrenamientos con la build debug en el mismo teléfono donde corre `connectedDebugAndroidTest`, cada corrida borra esos datos. **Mitigación elegida el 2026-10-11: `applicationIdSuffix = ".debug"` en el build type `debug`; se implementa en F00.** Así las instrumentadas instalan y desinstalan solo `com.juancholopes.weightrack.debug`, y la app de uso diario (`com.juancholopes.weightrack`, build `release`) queda aparte. Para instalar la build `release` hace falta firmarla, algo que la Spec 03 deja hoy fuera de alcance; F00 lo tiene en cuenta.
- **DSL incubadora.** `testFixtures` está marcada como incubadora en AGP. Si cambia en una versión futura, el bloque de `app/build.gradle.kts` se ajusta en la spec que actualice AGP.

## Cambios que esta spec pide a otras specs

Esta spec no edita otras specs por su cuenta. El dueño aprobó estos cambios el 2026-10-11, y ese mismo día se aplicaron las filas de las Specs 03, 20 y 30. La fila de la Spec 00 sigue sin aplicar.

| Spec | Cambio propuesto | Motivo | Estado |
| --- | --- | --- | --- |
| 00 Constitución | En "Definición de hecho y calidad", punto 2, nombrar el comando: `./gradlew assembleDebug test connectedDebugAndroidTest`, con un dispositivo (S31-R24). En "Pruebas", cambiar "La estrategia completa se define en la Spec 31" por una referencia a S31-R01 a S31-R27. Anotar la enmienda con fecha. | Que "un comando" signifique lo mismo en todas las specs. | Aprobado el 2026-10-11; pendiente de aplicar |
| 03 Configuración | S03-R14 y S03-A09: comando sin dispositivo `./gradlew assembleDebug test` y comando completo de S31-R24, con `connectedDebugAndroidTest` en lugar de `connectedAndroidTest`. S03-R08: incluir `testFixtures` entre los source sets de prueba. Esqueleto: anotar `src/testFixtures` (fakes y contratos, paquete `testing`). Estado actual: `androidx.test.ext:junit` 1.3.0 y `espresso-core` 3.7.0 son las últimas estables (verificado el 2026-10-11). | Alinear la configuración con esta spec. | Aplicado el 2026-10-11 |
| 03 Configuración | Proteger los datos reales de la app frente a `connectedDebugAndroidTest`, que desinstala la app al terminar. Decidido el 2026-10-11: `applicationIdSuffix = ".debug"` en el build type `debug`. F00 lo implementa y ajusta S03-R07, que hoy fija un solo `applicationId`. | Riesgo comprobado el 2026-10-11 (ver "Riesgos abiertos"). | Decidido; se implementa en F00 |
| 20 Persistencia | Marcar como aplicada su fila "31 Estrategia de pruebas": esta spec toma como dada la opción A. En "Pruebas", anotar que `RoomExerciseRepositoryContractTest` y `RoomSessionRepositoryContractTest` corren el contrato de S31-R07 contra los repositorios Room, además de las pruebas de S20-R46, que no cambian. En S20-A40, `connectedDebugAndroidTest`. | Trazabilidad de las pruebas nuevas sobre Room. | Aplicado el 2026-10-11 |
| 30 Arquitectura | Marcar como aplicada su fila "31 Estrategia de pruebas". En "Costuras para otras specs", apuntar a S31-R04 a S31-R16. En "Skills consultadas", fila `testing-setup`: Robolectric, capturas y Jacoco quedan resueltos en D1 a D3 de esta spec. En S30-A15, citar S31-R11. | Cerrar el pendiente de la Spec 30. | Aplicado el 2026-10-11 |
| 41 Sistema de diseño | Decidir si entran pruebas de capturas (D2) y pruebas con escala de fuente 1,5 con `DeviceConfigurationOverride` (skill `testing-setup`). | Costura que esta spec deja abierta. | Pendiente |
| 42 Pantallas y navegación | Fijar la pantalla inicial y el comportamiento de atrás que comprueba S31-R16, incluida la salida por la pantalla inicial si hay varias pilas. | S31-R16 necesita esa definición. | Pendiente |
| F00 a F05 | Aplicar esta spec: la prueba primero (S31-R22), la evidencia (S31-R25) y el contrato y el fake al cambiar una interfaz (S31-R08). F00 escribe las pruebas de navegación (S31-R16), adapta `MainActivitySmokeTest` (S31-R17), arma el shell para que esas pruebas no escriban en `weightrack.db` (S31-R18) y agrega `applicationIdSuffix = ".debug"` al build type `debug` (ver "Riesgos abiertos"). La primera spec con un ViewModel usa `MainDispatcherRule` (S31-R10). | Aplicar la estrategia en cada corte vertical. | Pendiente |

## Fuentes

Consultadas el 2026-10-11:

- [kotlinx.coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases) y Maven Central (`kotlinx-coroutines-test`): 1.11.0, publicada el 8 de mayo de 2026, última estable.
- Maven Central (`junit:junit`): 4.13.2, última de JUnit 4.
- Google Maven: `androidx.test.ext:junit` 1.3.0 y `espresso-core` 3.7.0, últimas estables; la BOM 2026.09.00 fija `runtime`, `ui` y `ui-test-junit4` en 1.12.1.
- [Robolectric releases](https://github.com/robolectric/robolectric/releases) y Maven Central: 4.17, del 10 de septiembre de 2026, "supports SDK 37".
- [Test Kotlin coroutines on Android](https://developer.android.com/kotlin/coroutines/test): `MainDispatcherRule` con `UnconfinedTestDispatcher` por defecto; no reemplazar `Main` en pruebas instrumentadas; `runTest` usa `StandardTestDispatcher`.
- [Test Kotlin flows on Android](https://developer.android.com/kotlin/flow/test): colector vacío en `backgroundScope` para `stateIn`; fakes para los repositorios; tratar `StateFlow` como contenedor y leer `value`; Turbine como opción de terceros.
- [Migrate to v2 testing APIs](https://developer.android.com/develop/ui/compose/testing/migrate-v2): las reglas de `androidx.compose.ui.test.junit4` pasan a `junit4.v2` y usan `StandardTestDispatcher`.
- [Compose testing common patterns](https://developer.android.com/develop/ui/compose/testing/common-patterns): `createAndroidComposeRule<ComponentActivity>()` con `ui-test-manifest`, `StateRestorationTester`, `DeviceConfigurationOverride`.
- [Android Gradle plugin 9.0 release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes): soporte completo de Android Studio para test fixtures; `android.onlyEnableUnitTestForTheTestedBuildType` pasa a `true`.
- [Android Gradle plugin 7.2 release notes](https://developer.android.com/build/releases/past-releases/agp-7-2-0-release-notes): `testFixtures { enable = true }`; lint analiza los fixtures.
- [Android Gradle plugin release notes](https://developer.android.com/build/releases/gradle-plugin): la línea estable es 9.4; no hay AGP 9.5 estable.
- [Compose Preview Screenshot Testing release notes](https://developer.android.com/studio/preview/compose-screenshot-testing-release-notes): 0.0.1-alpha16, en alfa.
- [Test and debug your database](https://developer.android.com/training/data-storage/room/testing-db): Robolectric desaconsejado para Room (Spec 20).
- Skill `.claude/skills/testing-setup` (2026-09-23) y sus referencias: `compose-screenshot-testing.md`, `compose-screenshot-testing-with-testsuites.md`, `hilt-testing.md` y `common-patterns.md`.
