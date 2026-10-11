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
| SF00-R07 | `ui/StartScreen.kt` declara `@Serializable data object StartKey : NavKey` y `StartScreen`. La pantalla tiene su `Scaffold` con una barra superior centrada que dice el nombre de la app y un texto corto de `strings.xml`. Recibe `onCreateExercise: (() -> Unit)? = null` y `onOpenArchived: (() -> Unit)? = null`; con `null`, sus botones no se muestran. No tiene ViewModel ni lee datos. F02 la reemplaza por `HomeKey` y la borra (Spec 42). |
| SF00-R08 | Cada feature registra sus entradas así: expone `fun EntryProviderScope<NavKey>.<feature>Entries(...)` en su `ui/`, con lambdas y sin la pila (S30-R39, S30-R40). El shell tiene un archivo puente por feature, `ui/<Feature>Destinations.kt`, que llama a esa función y traduce cada lambda a una operación sobre la pila. `WeightrackApp` llama a cada puente con una línea dentro de `entryProvider { }`. F00 deja el puente de la pantalla de arranque, `startDestination(backStack)`. |
| SF00-R09 | `WeightrackNavDisplay(backStack, entryProvider, modifier)` reúne `NavDisplay`, los decoradores, `onBack` y las transiciones, para que una prueba lo use con claves propias. `WeightrackApp` solo crea la pila y el `entryProvider`. |
| SF00-R10 | `WeightrackApplication.kt` declara `val CreationExtras.appContainer: AppContainer`, que lee la app con `APPLICATION_KEY` (ADR-30-04). F00 no agrega miembros al `AppContainer`: el `Clock` (S30-R36) y los casos de uso son de F02. |
| SF00-R11 | El manifiesto declara `android:enableOnBackInvokedCallback="true"` en `<application>`, con `tools:targetApi="33"` (S42-R05). |
| SF00-R12 | El build type `debug` tiene `applicationIdSuffix = ".debug"` y `versionNameSuffix = "-debug"` (S03-R07, Spec 31 "Riesgos abiertos"). El lanzador muestra "Weightrack Debug" en debug y "Weightrack" en release. El texto de la app (`app_name`) no cambia. |
| SF00-R13 | `app/build.gradle.kts` crea la firma `release` solo si existen y no están vacías las cuatro propiedades de Gradle `WEIGHTRACK_RELEASE_STORE_FILE`, `WEIGHTRACK_RELEASE_STORE_PASSWORD`, `WEIGHTRACK_RELEASE_KEY_ALIAS` y `WEIGHTRACK_RELEASE_KEY_PASSWORD`, leídas con `providers.gradleProperty(...)`. Sin ellas, la `release` sale sin firmar y el build no falla. Ninguna credencial ni ruta de la clave entra al repositorio (Spec 01, "Firma de los APK"). |
| SF00-R14 | Las pruebas de navegación viven en `src/androidTest/.../ui/` y se escriben antes del código (S31-R22). Comprueban que atrás saca la entrada superior y que atrás en `StartKey` saca la app del primer plano (S31-R16). `MainActivitySmokeTest` comprueba la pantalla de arranque (S31-R17). Ninguna prueba abre `weightrack.db` (S31-R18). |
| SF00-R15 | Todo texto nuevo vive en `strings.xml`. Lint no reporta errores (S03-R20). |

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
| `ui/StartScreen.kt` | `StartKey`, `StartScreen` y `startDestination` |
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
- [x] **D2. Pantalla de arranque sin ViewModel.** A: `StartScreen` sin ViewModel ni `UiState`. B: un ViewModel vacío para cumplir la convención de pantallas. Por qué A: no tiene estado ni datos, y vive hasta F02. B sería código sin uso (principio 6). Costo: es la única pantalla sin ViewModel; ver "Riesgos".
- [x] **D3. Qué significa "sale de la app".** Desde Android 12 (API 31), atrás en la actividad raíz del lanzador manda la tarea al fondo en vez de terminarla. A: la prueba acepta que la actividad quede detenida o destruida y que la pila no cambie. B: forzar `finish()` con un manejador propio. Por qué A: es el comportamiento del sistema y conserva la animación de volver al inicio. B agrega un manejador que la Spec 42 no pide. Costo: S42-A03 dice "termina la actividad"; en API 31 o más se cumple como "sale del primer plano".
- [x] **D4. Lifecycle completo en F00.** F00 agrega también `lifecycle-viewmodel-compose` y `lifecycle-runtime-compose`, que la Spec 30 dejaba a la primera spec con ViewModel o pantalla. Así F01 y F02 no tocan el catálogo. Ya llegan de forma transitiva con `lifecycle-viewmodel-navigation3`.

**Riesgos.**

- *Constitución.* La convención pide un ViewModel por pantalla. `StartScreen` no lo tiene (D2). Si el dueño lo ve como un choque, gana la constitución y se agrega un ViewModel mínimo.
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
| T3 | Crear `StartScreen.kt` con `StartKey`, `StartScreen`, `startDestination` y sus textos en `strings.xml`. | SF00-A05, A08 |
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
| SF00-A14 | El comando completo pasa. Ninguna prueba crea `weightrack.db`. | SF00-R14, S31-R18, S31-R24 | `./gradlew assembleDebug test connectedDebugAndroidTest`; revisión de que `StartScreen` y las pruebas no tocan el `AppContainer` salvo `WeightrackApplicationTest`, que no lee sus repositorios | Sí |

Si el teléfono no está conectado, los criterios con dispositivo quedan como pendientes en la verificación, con la fecha, y se cierran al conectarlo.

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
