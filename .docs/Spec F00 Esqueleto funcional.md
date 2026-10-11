# Spec F00: Esqueleto funcional

Oct 11, 2026 · @Juan

| ID | Estado | Fase | Depende de |
| --- | --- | --- | --- |
| F00 | Aprobada | 4 Features | 20, 30, 42 |

## Propósito

F00 deja la app lista para recibir features. Arma el shell de navegación, agrega las dependencias aprobadas y prepara el build para el uso diario en el teléfono.

Al terminar, la app abre en una pantalla de arranque temporal. F01 y F02 agregan sus pantallas sin rehacer el shell. El dueño aceptó de antemano las recomendaciones el 2026-10-11 y quiere una app que funcione hoy.

Implementa S40-F02 (moverse, volver y salir) y los criterios de la Spec 42 marcados para F00: S42-A01 (en parte), S42-A03 (en parte), S42-A04, S42-A22 y S42-A24.

## Alcance

- Dependencias de la Spec 30 ("Dependencias") y `navigationevent-compose` de la Spec 42.
- El shell en `ui/`: `WeightrackApp`, una sola pila, `NavDisplay`, decoradores, transiciones y la pantalla de arranque `StartKey`.
- La forma de registrar las entradas de cada feature.
- La extensión `CreationExtras.appContainer` (ADR-30-04).
- `android:enableOnBackInvokedCallback` en el manifiesto.
- Build `debug` aparte de la `release`, y firma de la `release` cuando hay credenciales.
- Pruebas de navegación (S31-R16) y la prueba de humo adaptada (S31-R17).

## Requisitos

| ID | Requisito |
| --- | --- |
| SF00-R01 | El catálogo de versiones agrega `navigation3-runtime` y `navigation3-ui` 1.2.0, y `lifecycle-viewmodel-navigation3`, `lifecycle-viewmodel-compose` y `lifecycle-runtime-compose` 2.11.0. Las versiones de Lifecycle comparten una sola referencia, `lifecycle` (hoy `lifecycleRuntimeKtx`). Todas van en `implementation`. |
| SF00-R02 | El plugin `org.jetbrains.kotlin.plugin.serialization` entra al catálogo con `version.ref = "kotlin"`, se declara en la raíz con `apply false` y se aplica en `:app`. `kotlinx-serialization-core` pasa de `runtimeOnly` a `implementation`. |
| SF00-R03 | El catálogo agrega `androidx.navigationevent:navigationevent-compose` 1.2.0 en `implementation` (Spec 42, D13). No se agrega ninguna otra dependencia. |
| SF00-R04 | `ui/WeightrackApp.kt` define `WeightrackApp()`. Crea una sola pila con `rememberNavBackStack(StartKey)` y la muestra con `NavDisplay`, con `onBack = { backStack.removeLastOrNull() }` y los decoradores `rememberSaveableStateHolderNavEntryDecorator()` y `rememberViewModelStoreNavEntryDecorator()` (S30-R37). |
| SF00-R05 | El shell no envuelve `NavDisplay` en un `Scaffold` ni le pone padding (S42-R10). `MainActivity` llama a `enableEdgeToEdge()` y muestra `WeightrackTheme { WeightrackApp() }`, sin otro contenido. |
| SF00-R06 | Las transiciones siguen la tabla "Transiciones" de la Spec 42 con los resortes de `WeightrackTheme.motion` (Spec 41). Avanzar: la nueva pantalla entra desde la derecha un 10 % del ancho con el resorte espacial normal y aparece con el resorte de efectos normal; la anterior sale un 10 % a la izquierda y se funde. Volver: lo inverso. Atrás predictivo: el valor por defecto de `NavDisplay`. Con `motion.reducedMotion`, las tres son solo fundidos con el resorte de efectos rápido. |
| SF00-R07 | `ui/StartScreen.kt` declara `@Serializable data object StartKey : NavKey` y `StartScreen`. La pantalla tiene su `Scaffold` con una barra superior centrada que dice el nombre de la app y un texto corto de `strings.xml`. Recibe `onCreateExercise: (() -> Unit)? = null` y `onOpenArchived: (() -> Unit)? = null`; con `null`, sus botones no se muestran. ~~No tiene ViewModel ni lee datos.~~ *Cambiado el 2026-10-11 por el coordinador (D2): tiene ViewModel, ver SF00-R16.* F02 la reemplaza por `HomeKey` y la borra (Spec 42). |
| SF00-R08 | Cada feature registra sus entradas así: expone `fun EntryProviderScope<NavKey>.<feature>Entries(...)` en su `ui/`, con lambdas y sin la pila (S30-R39, S30-R40). El shell tiene un archivo puente por feature, `ui/<Feature>Destinations.kt`, que llama a esa función y traduce cada lambda a una operación sobre la pila. `WeightrackApp` llama a cada puente con una línea dentro de `entryProvider { }`. F00 deja el puente de la pantalla de arranque, `startDestination(backStack)`. |
| SF00-R09 | `WeightrackNavDisplay(backStack, entryProvider, modifier)` reúne `NavDisplay`, los decoradores, `onBack` y las transiciones, para que una prueba lo use con claves propias. `WeightrackApp` solo crea la pila y el `entryProvider`. |
| SF00-R10 | `WeightrackApplication.kt` declara `val CreationExtras.appContainer: AppContainer`, que lee la app con `APPLICATION_KEY` (ADR-30-04). F00 no agrega miembros al `AppContainer`: el `Clock` (S30-R36) y los casos de uso son de F02. |
| SF00-R11 | El manifiesto declara `android:enableOnBackInvokedCallback="true"` en `<application>`, con `tools:targetApi="33"` (S42-R05). |
| SF00-R12 | El build type `debug` tiene `applicationIdSuffix = ".debug"` y `versionNameSuffix = "-debug"` (S03-R07, Spec 31 "Riesgos abiertos"). El lanzador muestra "Weightrack Debug" en debug y "Weightrack" en release. El texto de la app (`app_name`) no cambia. |
| SF00-R13 | `app/build.gradle.kts` crea la firma `release` solo si existen y no están vacías las cuatro propiedades de Gradle `WEIGHTRACK_RELEASE_STORE_FILE`, `WEIGHTRACK_RELEASE_STORE_PASSWORD`, `WEIGHTRACK_RELEASE_KEY_ALIAS` y `WEIGHTRACK_RELEASE_KEY_PASSWORD`, leídas con `providers.gradleProperty(...)`. Sin ellas, la `release` sale sin firmar y el build no falla. Ninguna credencial ni ruta de la clave entra al repositorio (Spec 01, "Firma de los APK"). |
| SF00-R14 | Las pruebas de navegación viven en `src/androidTest/.../ui/` y se escriben antes del código (S31-R22). Comprueban que atrás saca la entrada superior y que atrás en `StartKey` saca la app del primer plano (S31-R16). `MainActivitySmokeTest` comprueba la pantalla de arranque (S31-R17). Ninguna prueba abre `weightrack.db` (S31-R18). |
| SF00-R15 | Todo texto nuevo vive en `strings.xml`. Lint no reporta errores (S03-R20). |
| SF00-R16 | *Agregado el 2026-10-11 por el coordinador (D2).* La pantalla de arranque sigue la convención de pantallas de la constitución: `StartScreen` (obtiene el ViewModel con `viewModel(factory = StartViewModel.Factory)` y recolecta con `collectAsStateWithLifecycle()`), `StartContent` (sin estado), `StartViewModel` y `StartUiState` (`Loading` o `Ready(activeExerciseCount)`). El ViewModel recibe `ExerciseRepository`, cuenta los ejercicios activos de `observeActiveCatalog()` con `stateIn(viewModelScope, WhileSubscribed(5_000), Loading)` y declara su fábrica en el `companion` con `appContainer` (ADR-30-04, S30-R16). La pantalla muestra la cuenta. Tiene una prueba JVM con `FakeExerciseRepository` y `MainDispatcherRule` (S30-A15, S31-R11). |

## Plan

**Capas.** Solo el shell (`ui/`), el arranque (`MainActivity`, `WeightrackApplication`), el manifiesto, los recursos y Gradle. No toca `feature/`, el dominio ni la base de datos.

**Archivos.**

| Archivo | Cambio |
| --- | --- |
| `gradle/libs.versions.toml` | Versiones `navigation3 = "1.2.0"` y `navigationevent = "1.2.0"`; `lifecycleRuntimeKtx` pasa a `lifecycle`. Librerías y el plugin `kotlin-serialization` (SF00-R01 a R03) |
| `build.gradle.kts` | `alias(libs.plugins.kotlin.serialization) apply false` |
| `app/build.gradle.kts` | Plugin de serialización, dependencias, `debug` con sufijos, `signingConfigs` y firma condicional de `release` |
| `app/src/main/AndroidManifest.xml` | `enableOnBackInvokedCallback`, `android:label="@string/app_label"` en `<application>` y sin `android:label` en la actividad |
| `app/src/main/res/values/strings.xml` | `app_label` ("Weightrack"), `start_message` y los dos textos de los botones de arranque |
| `app/src/debug/res/values/strings.xml` | Nuevo: `app_label` ("Weightrack Debug") |
| `MainActivity.kt` | `WeightrackTheme { WeightrackApp() }` |
| `WeightrackApplication.kt` | `CreationExtras.appContainer` |
| `ui/WeightrackApp.kt` | Pila y `entryProvider { startDestination(backStack) }` |
| `ui/WeightrackNavDisplay.kt` | `NavDisplay`, decoradores, `onBack` y transiciones |
| `ui/ShellTransitions.kt` | Funciones que arman cada `ContentTransform` a partir de `MotionTokens` |
| `ui/StartScreen.kt` | `StartKey`, `StartScreen`, `StartContent` (con `@ThemePreviews`) y `startDestination` |
| `ui/StartViewModel.kt` | Nuevo por el cambio de D2: `StartViewModel`, su `Factory` y `StartUiState` (SF00-R16) |
| `src/test/.../ui/StartViewModelTest.kt` | Nuevo por el cambio de D2 (SF00-A15) |
| `src/androidTest/.../ui/ShellNavigationTest.kt` | Nuevo (SF00-A06, A07) |
| `src/androidTest/.../WeightrackApplicationTest.kt` | Nuevo (SF00-A09) |
| `src/androidTest/.../MainActivitySmokeTest.kt` | Adaptada (SF00-A05) |

**Cómo crece el shell.** F01 agrega `ui/CatalogDestinations.kt` y una línea `catalogDestinations(backStack)` en `WeightrackApp`; además pasa sus lambdas a `startDestination`. F02 agrega `ui/SessionDestinations.kt` y su línea, cambia `StartKey` por `HomeKey` y borra `StartScreen.kt`. Así, F01 y F02 solo comparten una línea cada una en `WeightrackApp.kt`. El aviso `PendingNotice` (Spec 42, D6) lo agrega la primera spec que lo usa.

```kotlin
// ui/WeightrackApp.kt (guía; los nombres finales salen de la implementación)
@Composable
fun WeightrackApp(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(StartKey)
    WeightrackNavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            startDestination(backStack)
            // F01: catalogDestinations(backStack)
        },
        modifier = modifier,
    )
}
```

**Firma.** El patrón guía de `app/build.gradle.kts`:

```kotlin
val releaseSigning = listOf(
    "WEIGHTRACK_RELEASE_STORE_FILE", "WEIGHTRACK_RELEASE_STORE_PASSWORD",
    "WEIGHTRACK_RELEASE_KEY_ALIAS", "WEIGHTRACK_RELEASE_KEY_PASSWORD",
).associateWith { providers.gradleProperty(it).orNull?.takeIf(String::isNotBlank) }
val hasReleaseSigning = releaseSigning.values.all { it != null }
// android { signingConfigs { if (hasReleaseSigning) create("release") { ... } } }
// buildTypes { release { if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release") } }
```

La clave existe en `~/keystores/weightrack-release.jks` y sus credenciales en `~/.gradle/gradle.properties`, con ruta absoluta (comprobado el 2026-10-11 sin leer los secretos).

**Decisiones.** Todas decididas el 2026-10-11 (el dueño aceptó de antemano las recomendaciones).

- [x] **D1. Nombre en el lanzador.** A: una cadena `app_label` en `src/main` y otra en `src/debug/res/values/strings.xml`. B: `resValue` en el build type. C: un `manifestPlaceholder`. Por qué A: el texto vive en `strings.xml` (S03-R11) y es el mecanismo normal de los source sets. B y C escriben el texto en Gradle. Costo: una cadena más.
- [x] **D2. Pantalla de arranque ~~sin ViewModel~~ con un ViewModel mínimo.** A: `StartScreen` sin ViewModel ni `UiState`. B: un ViewModel vacío para cumplir la convención de pantallas. C: un ViewModel mínimo que lee un dato real. ~~Por qué A: no tiene estado ni datos, y vive hasta F02. B sería código sin uso (principio 6). Costo: es la única pantalla sin ViewModel; ver "Riesgos".~~ **Cambiado el 2026-10-11 por el coordinador a C.** Por qué: A choca con la convención de la constitución ("cada pantalla se compone de un composable de pantalla, un ViewModel y un estado de UI inmutable"), y ante un choque gana la constitución (lo preveía "Riesgos"). C, a diferencia de B, no es código sin uso: la pantalla muestra cuántos ejercicios activos hay y prueba de punta a punta la cadena contenedor, fábrica de ADR-30-04, `CreationExtras.appContainer`, ViewModel, `StateFlow` con `WhileSubscribed` y `collectAsStateWithLifecycle`, que F01 y F02 repiten (SF00-R16). Costo: un archivo y una prueba más que F02 borra junto con `StartScreen.kt`, y la prueba de humo pasa a abrir la base de datos de la build debug (ver "Riesgos").
- [x] **D3. Qué significa "sale de la app".** Desde Android 12 (API 31), atrás en la actividad raíz del lanzador manda la tarea al fondo en vez de terminarla. A: la prueba acepta que la actividad quede detenida o destruida y que la pila no cambie. B: forzar `finish()` con un manejador propio. Por qué A: es el comportamiento del sistema y conserva la animación de volver al inicio. B agrega un manejador que la Spec 42 no pide. Costo: S42-A03 dice "termina la actividad"; en API 31 o más se cumple como "sale del primer plano".
- [x] **D4. Lifecycle completo en F00.** F00 agrega también `lifecycle-viewmodel-compose` y `lifecycle-runtime-compose`, que la Spec 30 dejaba a la primera spec con ViewModel o pantalla. Así F01 y F02 no tocan el catálogo. Ya llegan de forma transitiva con `lifecycle-viewmodel-navigation3`.

**Riesgos.**

- *Constitución.* La convención pide un ViewModel por pantalla. ~~`StartScreen` no lo tiene (D2). Si el dueño lo ve como un choque, gana la constitución y se agrega un ViewModel mínimo.~~ Resuelto el 2026-10-11: el coordinador lo vio como choque y cambió D2; `StartScreen` tiene `StartViewModel` (SF00-R16).
- *Prueba de humo y la base de datos (por el cambio de D2).* `MainActivitySmokeTest` arranca la app real, así que `StartViewModel` lee `observeActiveCatalog()` por el `AppContainer` real y Room abre (y crea, si no existe) `weightrack.db` de `com.juancholopes.weightrack.debug`. Solo lee; no escribe filas. Choca con la letra de S31-R18 ("ninguna prueba abre ni escribe el archivo `weightrack.db`"). Mitigación: el sufijo `.debug` aísla esa base de la app de uso diario y `connectedDebugAndroidTest` desinstala la build debug al terminar. `ShellNavigationTest` no la abre: registra `StartKey` con un `StartViewModel` sobre `FakeExerciseRepository`. Se propone a la Spec 31 exceptuar la prueba de humo (ver "Cambios que esta spec pide a otras specs").
- *R8 en release.* La `release` tiene `optimization` activo y las claves usan serialización. `kotlinx-serialization-core` y Navigation 3 traen sus reglas de R8. Se comprueba al probar la `release` en el teléfono (SF00-A11).
- *Plugin de serialización con el Kotlin integrado de AGP 9.* Se aplica igual que `kotlin.plugin.compose`. Lo comprueba el build (SF00-A01).
- *Skill `navigation-event`.* Su guía declara también `navigationevent`. No hace falta: `navigationevent-compose` 1.2.0 la expone como `api` en su variante Android (metadatos de Gradle en Google Maven, 2026-10-11).
- *Spec 41.* Su tabla de componentes todavía dice que F00 construye `PillTabs` y escribe la prueba con fuente al 200 %. La Spec 42 pide pasarlo a F02 (fila pendiente). F00 no los construye.
- *Teléfono.* Puede estar desconectado. Los criterios con dispositivo se verifican al conectarlo y se anotan como pendientes mientras tanto.

## Tareas

Una a la vez, en este orden. Cada una termina con su evidencia.

| ID | Tarea | Criterio |
| --- | --- | --- |
| T1 | Agregar al catálogo y a Gradle las dependencias y el plugin de SF00-R01 a R03. Renombrar `lifecycleRuntimeKtx` a `lifecycle`. | SF00-A01, A02 |
| T2 | Escribir primero `ShellNavigationTest` y `WeightrackApplicationTest`, y adaptar `MainActivitySmokeTest`. Verlas fallar (no compilan sin el shell). | SF00-A05, A06, A07, A09 |
| T3 | Crear `StartScreen.kt` con `StartKey`, `StartScreen`, `startDestination` y sus textos en `strings.xml`. Por el cambio de D2, antes escribir `StartViewModelTest`, verla fallar y crear `StartViewModel.kt`. | SF00-A05, A08, A15 |
| T4 | Crear `ShellTransitions.kt` y `WeightrackNavDisplay.kt` con decoradores, `onBack` y transiciones. | SF00-A06, A08, A10 |
| T5 | Crear `WeightrackApp.kt` y cambiar `MainActivity` para mostrarlo. | SF00-A05, A07, A08 |
| T6 | Agregar `CreationExtras.appContainer` en `WeightrackApplication.kt`. | SF00-A09 |
| T7 | Agregar `enableOnBackInvokedCallback`, `app_label` y quitar la etiqueta de la actividad en el manifiesto. | SF00-A04 |
| T8 | Agregar los sufijos de `debug` y `src/debug/res/values/strings.xml`. | SF00-A11 |
| T9 | Agregar la firma condicional de `release`. Probar con y sin credenciales. | SF00-A12 |
| T10 | Correr lint y el comando completo. Con el teléfono conectado, correr las instrumentadas y las pruebas manuales. | SF00-A13, A14, A10, A11 |

## Criterios de aceptación

`<apk-debug>` es `app/build/outputs/apk/debug/app-debug.apk`. `aapt2` es el de las build-tools del SDK.

| ID | Criterio | Comprueba | Evidencia | Dispositivo |
| --- | --- | --- | --- | --- |
| SF00-A01 | Sin dispositivo, el build y las pruebas JVM pasan desde un clon limpio. | SF00-R01 a R03, S03-R14 | `./gradlew assembleDebug test` en una copia limpia (S03-A09) | No |
| SF00-A02 | El classpath trae `navigation3-ui` 1.2.0, `lifecycle-viewmodel-navigation3` 2.11.0 y `navigationevent-compose` 1.2.0 (ya no 1.1.1), y `kotlinx-serialization-core` está al compilar. Las versiones de Lifecycle salen de una sola referencia y ningún `build.gradle.kts` escribe versiones. | SF00-R01 a R03, S30-R41, S42-A04 | `./gradlew :app:dependencies --configuration debugCompileClasspath`; `grep -n "lifecycle" gradle/libs.versions.toml`; la búsqueda de S03-A03 | No |
| SF00-A03 | No entra ninguna dependencia fuera de SF00-R01 a R03. | SF00-R03 | `git diff gradle/libs.versions.toml app/build.gradle.kts` revisado contra la tabla de "Dependencias" de la Spec 30 | No |
| SF00-A04 | El manifiesto fusionado tiene `android:enableOnBackInvokedCallback="true"` y `android:label="@string/app_label"` en `<application>`, y la actividad no tiene `android:label`. | SF00-R11, SF00-R12, S42-R05 | `./gradlew :app:processDebugMainManifest` y `grep` sobre el manifiesto fusionado (ruta de S03-A08) | No |
| SF00-A05 | La app abre en la pantalla de arranque: se ven el nombre de la app y `start_message`, y no hay botones de F01. | SF00-R07, S31-R17 | `MainActivitySmokeTest.launch_showsAppName` (sigue siendo la evidencia de S31-A06) | Sí |
| SF00-A06 | Con la pila [`StartKey`, `TestKey`], atrás deja [`StartKey`] y muestra la pantalla de arranque. `TestKey` es una clave `@Serializable` de la prueba. | SF00-R04, SF00-R09, S31-R16 | `ShellNavigationTest.back_withEntryAboveStart_popsToStart`, con `WeightrackNavDisplay` y `Espresso.pressBack()` | Sí |
| SF00-A07 | En `StartKey`, atrás saca la app del primer plano: la actividad queda detenida o destruida (D3) y la pila no cambia. | SF00-R04, S42-A03 (parte de F00) | `ShellNavigationTest.back_onStart_leavesForeground`, con `Espresso.pressBackUnconditionally()` y el estado de `ActivityScenario` | Sí |
| SF00-A08 | `NavDisplay` usa los dos decoradores y `onBack` saca la entrada superior. Ningún archivo del shell envuelve `NavDisplay` en un `Scaffold` ni le pone padding. `StartKey` es `@Serializable` e implementa `NavKey`. | SF00-R04, R05, R07, R08, S42-A01, S42-A24 | Revisión de `ui/`; `grep -n "Scaffold\|padding" app/src/main/java/com/juancholopes/weightrack/ui/WeightrackNavDisplay.kt app/src/main/java/com/juancholopes/weightrack/ui/WeightrackApp.kt` sin resultados; `grep -rn ": NavKey" app/src/main/java` | No |
| SF00-A09 | `CreationExtras.appContainer` devuelve el `container` de la app cuando `APPLICATION_KEY` es la `WeightrackApplication`. | SF00-R10 | `WeightrackApplicationTest.appContainer_readsContainerFromApplicationKey` | Sí |
| SF00-A10 | Las transiciones usan `WeightrackTheme.motion`. Con `reducedMotion`, no hay desplazamiento ni escala: solo fundidos. | SF00-R06, S42-A22 | Revisión de `ShellTransitions.kt`. Prueba manual con "Quitar animaciones" en cuanto exista una segunda pantalla (F01) | Sí, manual |
| SF00-A11 | La debug se instala como `com.juancholopes.weightrack.debug`, con `versionName` "1.0-debug" y la etiqueta "Weightrack Debug". La release se llama "Weightrack". Las dos conviven en el teléfono. | SF00-R12, S03-R07 | `aapt2 dump badging <apk-debug>` muestra `package: name='com.juancholopes.weightrack.debug'`, `versionName='1.0-debug'` y `application-label:'Weightrack Debug'`; con el teléfono, `adb shell pm list packages \| grep weightrack` muestra los dos paquetes | Parcial |
| SF00-A12 | Con las credenciales, la release sale firmada con la clave de la Spec 01. Sin ellas, sale sin firmar y el build no falla. El repositorio no contiene la clave ni sus credenciales. | SF00-R13 | `./gradlew assembleRelease` y `apksigner verify --print-certs` sobre `app-release.apk`: la huella SHA-256 es la de la Spec 01. `./gradlew assembleRelease -PWEIGHTRACK_RELEASE_STORE_FILE=` deja `app-release-unsigned.apk`. `git ls-files \| grep -E "\.jks$\|keystore"` y `git grep -n "WEIGHTRACK_RELEASE_.*="` sin resultados | No |
| SF00-A13 | Lint no reporta errores y los textos nuevos están en `strings.xml`. | SF00-R15 | `./gradlew lintDebug`; `grep -rn "Text(\"" app/src/main/java/com/juancholopes/weightrack/ui` sin resultados | No |
| SF00-A14 | El comando completo pasa. Ninguna prueba crea `weightrack.db`, salvo `MainActivitySmokeTest`, que solo la lee (cambio de D2, ver "Riesgos"). | SF00-R14, S31-R18, S31-R24 | `./gradlew assembleDebug test connectedDebugAndroidTest`; revisión de que ~~`StartScreen` y~~ las pruebas no tocan el `AppContainer` salvo `WeightrackApplicationTest`, que no lee sus repositorios, y la prueba de humo, que recorre la app real | Sí |
| SF00-A15 | *Agregado el 2026-10-11 (D2).* `StartViewModel` empieza en `Loading`; con colector, cuenta solo los ejercicios activos y se actualiza al crear uno. Su fábrica es la única pieza de la pantalla que nombra el contenedor. | SF00-R16, S30-A06, S30-A14, S30-A15 | `StartViewModelTest` (JVM, con `FakeExerciseRepository` y `MainDispatcherRule`); `grep -rlE "AppContainer\|appContainer" app/src/main` | No |

Si el teléfono no está conectado, los criterios con dispositivo quedan como pendientes en la verificación, con la fecha, y se cierran al conectarlo.

## Verificación

Implementado el 2026-10-11 en el árbol de trabajo, sin commit, sobre `dd9b6cb`. El teléfono (Xiaomi 24117RN76L, API 36) estaba conectado, pero rechazó toda instalación por USB con `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`, tanto desde `connectedDebugAndroidTest` como con `adb install -r` y `installDebug` (cuatro intentos). Por eso los criterios con dispositivo quedan **pendientes de dispositivo (2026-10-11)** y la spec sigue Aprobada (S31-R25). Para cerrarlos hay que aceptar la instalación en el teléfono o activar "Instalar vía USB" en las opciones de desarrollador, y correr el comando completo.

### Criterios

| Criterio | Resultado | Evidencia |
| --- | --- | --- |
| SF00-A01 | Pasa | `./gradlew assembleDebug test` en una copia limpia de los archivos no ignorados (ver "Comandos"). |
| SF00-A02 | Pasa | En `debugCompileClasspath`: `navigation3-ui:1.2.0`, `navigation3-runtime:1.2.0`, `lifecycle-viewmodel-navigation3:2.11.0`, `lifecycle-viewmodel-compose:2.11.0`, `lifecycle-runtime-compose:2.11.0`, `navigationevent-compose:1.2.0` (las transitivas 1.0.0 y 1.1.1 suben a 1.2.0) y `kotlinx-serialization-core:1.11.0` (la 1.7.3 transitiva sube). `grep -n "lifecycle" gradle/libs.versions.toml`: una sola versión, `lifecycle = "2.11.0"`, y las cuatro librerías de Lifecycle usan `version.ref = "lifecycle"`. La búsqueda de S03-A03 solo devuelve `versionName = "1.0"`. |
| SF00-A03 | Pasa | `git diff gradle/libs.versions.toml app/build.gradle.kts` solo agrega las seis librerías y el plugin de la tabla "Dependencias" de la Spec 30, y pasa `kotlinx-serialization-core` a `implementation`. No entra nada más; `navigationevent` no se declara aparte (ver "Riesgos"). |
| SF00-A04 | Pasa | Manifiesto fusionado de debug: `package="com.juancholopes.weightrack.debug"`, `android:enableOnBackInvokedCallback="true"` y `android:label="@string/app_label"` en `<application>`; `MainActivity` sin `android:label`. |
| SF00-A05 | Pendiente de dispositivo | `MainActivitySmokeTest.launch_showsAppName` compila (`assembleDebugAndroidTest`). Comprueba el nombre, `start_message` y que no existen "Crear ejercicio" ni "Archivados". |
| SF00-A06 | Pendiente de dispositivo | `ShellNavigationTest.back_withEntryAboveStart_popsToStart` compila. |
| SF00-A07 | Pendiente de dispositivo | `ShellNavigationTest.back_onStart_leavesForeground` compila. Espera hasta 5 s a que la actividad quede en `CREATED` o `DESTROYED` (D3). |
| SF00-A08 | Pasa | `WeightrackNavDisplay` pasa `rememberSaveableStateHolderNavEntryDecorator()` y `rememberViewModelStoreNavEntryDecorator()`, y `onBack = { backStack.removeLastOrNull() }`. El `grep` de `Scaffold\|padding` sobre `WeightrackNavDisplay.kt` y `WeightrackApp.kt` no da resultados. `grep -rn ": NavKey" app/src/main/java`: solo `ui/StartScreen.kt:31: data object StartKey : NavKey`, con `@Serializable`. |
| SF00-A09 | Pendiente de dispositivo | `WeightrackApplicationTest.appContainer_readsContainerFromApplicationKey` compila. |
| SF00-A10 | Pasa la revisión; prueba manual pendiente | `ShellTransitions.kt`: avanzar y volver usan `slideIn/OutHorizontally` de un décimo del ancho con `motion.defaultSpatial` y `fadeIn/Out` con `motion.defaultEffectsSpec()`; atrás predictivo usa `defaultPredictivePopTransitionSpec()`. Con `motion.reducedMotion`, las tres son `fadeIn togetherWith fadeOut` con `motion.fastEffectsSpec()`, sin desplazamiento ni escala. La prueba manual con "Quitar animaciones" espera a F01. |
| SF00-A11 | Pasa en parte | `aapt2 dump badging`: debug `package: name='com.juancholopes.weightrack.debug' versionCode='1' versionName='1.0-debug'` y `application-label:'Weightrack Debug'`; release `name='com.juancholopes.weightrack'`, `versionName='1.0'` y `application-label:'Weightrack'`. Que convivan en el teléfono queda pendiente de dispositivo. |
| SF00-A12 | Pasa | `./gradlew assembleRelease` deja `app-release.apk`; `apksigner verify --print-certs` da la huella SHA-256 `4D:2E:1F:C8:C5:9E:A7:0A:1A:15:CD:C1:59:90:2D:66:17:BD:6C:CF:A0:3D:6F:AC:3D:05:99:B3:F0:18:EE:AF`, la de la Spec 01. `./gradlew assembleRelease -PWEIGHTRACK_RELEASE_STORE_FILE=` termina con `BUILD SUCCESSFUL` y deja `app-release-unsigned.apk` (`apksigner verify`: "DOES NOT VERIFY, Missing META-INF/MANIFEST.MF"). `git ls-files \| grep -E "\.jks$\|keystore"`: sin resultados. `git grep -n "WEIGHTRACK_RELEASE_.*="`: solo la línea de este mismo criterio en esta spec, que cita el patrón; ningún archivo de código o de Gradle. |
| SF00-A13 | Pasa | `./gradlew lintDebug`: 0 errores y 1 aviso, `AndroidGradlePluginVersion` en `gradle-wrapper.properties`, que ya existía (S03-R20). `grep -rn "Text(\"" .../ui`: sin resultados. |
| SF00-A14 | Pendiente de dispositivo | El nivel sin dispositivo pasa (SF00-A01). La revisión pasa: `ShellNavigationTest` usa un `StartViewModel` sobre `FakeExerciseRepository`; `WeightrackApplicationTest` solo compara `extras.appContainer` con `application.container`; la prueba de humo es la única que abre la base de la build debug, solo para leer (ver "Riesgos"). |
| SF00-A15 | Pasa | `StartViewModelTest`: 3 pruebas, sin fallos (`uiState_beforeAnyCollector_isLoading`, `uiState_withActiveAndArchivedExercises_countsOnlyActiveOnes`, `uiState_afterAnExerciseIsCreated_updatesTheCount`). Antes de crear `StartViewModel.kt` fallaron por compilación ("Unresolved reference 'StartViewModel'"). S30-A06 devuelve solo `AppContainer.kt`, `WeightrackApplication.kt` y `ui/StartViewModel.kt`. |

Criterios de otras specs comprobados aquí:

- **S30-A01 a S30-A11 (S31-A13):** pasan. A01: solo `26:include(":app")`. A02, A03, A04, A07, A08: sin resultados. A05: tres líneas de `AppContainer.kt` (21, 27, 31). A06: `AppContainer.kt`, `WeightrackApplication.kt` y `ui/StartViewModel.kt`. A09: `grep -c` devuelve 1 y no hay `androidx.fragment`. A10: solo `versionName = "1.0"`; los bloques `plugins` usan `alias`, con `apply false` en la raíz, incluido el nuevo `kotlin.serialization`. A11: la corrida de SF00-A01.
- **S30-A12 a S30-A16 para la pantalla de arranque:** tiene `StartScreen`, `StartContent`, `StartViewModel` y `StartUiState`, en `ui/` por la enmienda de ADR-30-03; `StartContent` no recibe el ViewModel. Sin `Channel`, `SharedFlow`, `LiveData` ni `MutableStateFlow` públicos. `collectAsStateWithLifecycle` en `StartScreen.kt` y ningún `collectAsState(`. `StartViewModel.kt` no importa `androidx.navigation`. S30-A17 devuelve `StartKey` en `ui/`, que es la excepción de ADR-30-03 hasta F02.
- **S30-A21:** el catálogo tiene `navigation3`, `navigationevent` y `lifecycle-viewmodel-navigation3`; `debugRuntimeClasspath` no tiene `androidx.navigation:navigation-compose`.
- **S31-A07 a S31-A10:** sin resultados en las cuatro búsquedas. Los métodos nuevos siguen `sujeto_condición_resultado`.
- **S42-A01 (parte de F00), S42-A04, S42-A22 y S42-A24:** pasan por SF00-A08, SF00-A04 y SF00-A02, SF00-A10 y SF00-A08. **S42-A03 (parte de F00):** pendiente de dispositivo con SF00-A07.
- **Spec 41, S41-A04 y S41-A08** (`WeightrackThemeTest`, `RobotoFlexRenderingTest`): pendientes de dispositivo por el mismo rechazo de instalación.

### Comandos

| Comando | Resultado |
| --- | --- |
| `./gradlew assembleDebug test` (árbol de trabajo) | `BUILD SUCCESSFUL`. `testDebugUnitTest`: 131 pruebas en 18 clases, sin fallos, errores ni omitidas (antes de F00: 128 en 17; F00 suma `StartViewModelTest`, 3) |
| `./gradlew assembleDebug test` (copia limpia) | Copia de los archivos no ignorados (`git ls-files -co --exclude-standard`) en una carpeta nueva, con `ANDROID_HOME` en lugar del `local.properties` ignorado: `BUILD SUCCESSFUL`, 50 tareas ejecutadas, 131 pruebas en 18 clases, sin fallos, errores ni omitidas |
| `./gradlew assembleDebugAndroidTest` | `BUILD SUCCESSFUL`: compilan `ShellNavigationTest`, `WeightrackApplicationTest` y la `MainActivitySmokeTest` adaptada |
| `./gradlew assembleDebug test connectedDebugAndroidTest` | Sin resultado: la instalación falla con `INSTALL_FAILED_USER_RESTRICTED`. Gradle termina con `BUILD SUCCESSFUL` aunque no corrió ninguna prueba instrumentada, así que ese mensaje no sirve de evidencia |
| `./gradlew assembleRelease` | `BUILD SUCCESSFUL`, R8 incluido; `app-release.apk` firmado (SF00-A12) |
| `./gradlew assembleRelease -PWEIGHTRACK_RELEASE_STORE_FILE=` | `BUILD SUCCESSFUL`; `app-release-unsigned.apk` |
| `./gradlew lintDebug` | `BUILD SUCCESSFUL`; 0 errores, 1 aviso previo |
| `./gradlew :app:processDebugMainManifest` | Manifiesto fusionado de SF00-A04 |

### Hechos de la implementación

- No hizo falta adaptar ninguna versión: todas las de SF00-R01 a R03 resolvieron tal como las fija esta spec.
- El plugin `org.jetbrains.kotlin.plugin.serialization` funciona con el Kotlin integrado de AGP 9.4.1 igual que `kotlin.plugin.compose`.
- `navigation3-ui` 1.2.0 expone `defaultPredictivePopTransitionSpec()` como función pública; el shell la usa para conservar el atrás predictivo por defecto y solo la cambia con movimiento reducido.
- `startDestination(backStack)` todavía no usa `backStack`: F01 lo usa al pasar `onCreateExercise` y `onOpenArchived`. El compilador no avisa.
- `StartScreen` recibe el ViewModel como parámetro, con `viewModel(factory = StartViewModel.Factory)` por defecto. Así `ShellNavigationTest` (y las pruebas de navegación de F01) registran `StartKey` con una fábrica sobre `FakeExerciseRepository` y no abren la base de datos.
- Los textos nuevos: `app_label`, `start_message` ("Pronto registrarás aquí tus series."), `start_create_exercise` ("Crear ejercicio"), `start_open_archived` ("Archivados") y el plural `start_active_exercises`. `src/debug/res/values/strings.xml` cambia solo `app_label`.
- Las credenciales de firma se leen con `providers.gradleProperty(...)` desde `~/.gradle/gradle.properties`. Ningún valor ni ruta entró al repositorio ni a esta spec.

### Cambios que esta spec pide a otras specs

| Spec | Cambio | Motivo | Estado |
| --- | --- | --- | --- |
| 31 Estrategia de pruebas | En S31-R18, exceptuar `MainActivitySmokeTest`: como prueba de extremo a extremo, abre para leer la base de la build debug, que `connectedDebugAndroidTest` desinstala al terminar. Ajustar S31-A09 si hace falta. | Consecuencia del cambio de D2 (SF00-R16). | Propuesto; decide el dueño |
| F02 Registrar sesión y series | Al borrar `StartScreen.kt`, borrar también `ui/StartViewModel.kt` y `src/test/.../ui/StartViewModelTest.kt`, y quitar los textos `start_*` de `strings.xml`. | La pantalla de arranque ahora tiene ViewModel y prueba propios. | Propuesto |

## Fuera de alcance

- El inicio (`HomeKey`), `PillTabs`, `ExerciseCard` y la prueba con fuente al 200 %: F02 (Spec 42, D2).
- Pantallas y entradas de F01 a F05, `PendingNotice` y cualquier `NavigationBackHandler`: la primera spec que los usa.
- El `Clock` del `AppContainer` y los casos de uso: F02 (S30-R36).
- `versionName` "0.0.1", `versionCode` y la etiqueta `v0.0.1`: al cerrar la 0.0.1, después de F03 (S01-R11).
- Deep links, Scenes, tablets y `material3-adaptive-navigation3` (Spec 42).
- Publicar en Google Play.

## Fuentes

Consultadas el 2026-10-11:

- Specs 00 (ciclo de trabajo y convenciones), 01 ("Firma de los APK" y S01-R11), 03 (S03-R07, S03-R14, S03-A03, S03-A08), 20, 30 (ADR-30-03, ADR-30-04, ADR-30-10 y "Dependencias"), 31 (S31-R16 a S31-R18, S31-R22 y "Riesgos abiertos"), 41 ("Movimiento", S41-R10, S41-R11) y 42 ("Estructura de navegación", "Transiciones", S42-A01, A03, A04, A22 y A24).
- Código: `app/build.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `MainActivity.kt`, `AppContainer.kt`, `WeightrackApplication.kt`, `MainActivitySmokeTest.kt` y `ui/theme/Motion.kt` (`MotionTokens`, `reducedMotion`).
- Skills: `navigation-3` (2026-09-24; receta `animations` y guía de migración, sección de entradas por feature) y `navigation-event` (2026-09-01; `setup.md` y su checklist).
- [Google Maven, `navigation3-ui`](https://dl.google.com/android/maven2/androidx/navigation3/navigation3-ui/maven-metadata.xml): 1.2.0 es la última estable; la más nueva es 1.3.0-alpha02. Lo mismo para `navigation3-runtime`. [Notas de versión](https://developer.android.com/jetpack/androidx/releases/navigation3): 1.2.0 del 23 de septiembre de 2026.
- [Google Maven, `lifecycle-viewmodel-navigation3`](https://dl.google.com/android/maven2/androidx/lifecycle/lifecycle-viewmodel-navigation3/maven-metadata.xml), `lifecycle-viewmodel-compose` y `lifecycle-runtime-compose`: 2.11.0 es la última estable; la más nueva es 2.12.0-alpha04. [Notas de versión](https://developer.android.com/jetpack/androidx/releases/lifecycle): 2.11.0 del 17 de junio de 2026.
- [Google Maven, `navigationevent-compose`](https://dl.google.com/android/maven2/androidx/navigationevent/navigationevent-compose/maven-metadata.xml): 1.2.0 es la última estable (actualizado el 7 de octubre de 2026). Su variante Android expone `navigationevent` 1.2.0 como `api`. [Notas de versión](https://developer.android.com/jetpack/androidx/releases/navigationevent): 1.2.0 del 7 de octubre de 2026.
- [Gradle Plugin Portal, `org.jetbrains.kotlin.plugin.serialization` 2.4.21](https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization): existe en la versión de Kotlin del proyecto.
- Comportamiento de atrás en la actividad raíz desde Android 12: [Cambios de comportamiento de Android 12](https://developer.android.com/about/versions/12/behavior-changes-all#back-press).
