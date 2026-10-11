# Spec 03: Configuración inicial del proyecto Android

Oct 10, 2026 · @Juan

| ID | Estado | Fase | Depende de |
| --- | --- | --- | --- |
| 03 | Implementada | 0 Fundamentos | 00 |

## Propósito

Esta spec fija la base técnica del proyecto: versiones, plugins, identificador de la app, estructura de paquetes y build. Todas las specs posteriores se apoyan en ella, en especial la Spec 20 (persistencia con Room 3) y la F00 (esqueleto funcional).

Parte del proyecto tal como lo generó Android Studio y lo alinea con la constitución.

No incluye el esquema de Room, la inyección de dependencias, la navegación ni ninguna feature. Eso vive en las Specs 20, 30 y 42 y en las specs F.

## Estado actual

Lo que generó Android Studio, comparado con lo que exige la constitución. Las versiones estables se verificaron el 2026-10-10 en las fuentes citadas al final.

| Elemento | Hoy en el proyecto | Exige la constitución o última estable | Brecha |
| --- | --- | --- | --- |
| Kotlin | 2.2.10 | Línea 2.4.x. Última estable: 2.4.21 (8 de octubre de 2026) | Sí |
| Android Gradle Plugin | 9.4.1 | Línea 9.4, última estable 9.4.1 (verificado el 2026-10-10) | No |
| Gradle wrapper | 9.6.0 | Mínimo de AGP 9.4: 9.6.0 | No |
| Compose BOM | 2026.02.01 | BOM estable más reciente: 2026.09.00, con `compose.ui` 1.12.1 y `material3` 1.4.0 (mapeo por confirmar) | Sí |
| KSP | No declarado | Necesario para Room 3. Última estable: 2.3.12. Compatible con Kotlin 2.4.21: verificado el 2026-10-11 al implementar la Spec 20 | Sí |
| Room 3 | No declarado | `androidx.room3` 3.0.3 (9 de septiembre de 2026) | Le toca a la Spec 20 |
| `core-ktx` | 1.10.1 | 1.19.1 (23 de septiembre de 2026) | Sí |
| `lifecycle-runtime-ktx` | 2.6.1 | 2.11.0 (17 de junio de 2026) | Sí |
| `activity-compose` | 1.8.0 | 1.13.0 (11 de marzo de 2026) | Sí |
| `androidx.test.ext:junit` y `espresso-core` | 1.1.5 y 3.5.1 | 1.3.0 y 3.7.0, últimas estables (verificado el 2026-10-11, Spec 31) | No: el catálogo ya usa 1.3.0 y 3.7.0 |
| `junit` y `kotlinx-coroutines-test` | 4.13.2; `kotlinx-coroutines-test` no estaba | 4.13.2, última de JUnit 4, y 1.11.0, última estable (verificado el 2026-10-11, Spec 31) | No |
| `kotlinx-coroutines-core` y `-android` | 1.9.0, transitivas | 1.11.0, declaradas el 2026-10-11 (Spec 20): `kotlinx-coroutines-test` 1.11.0 no funciona con el `core` 1.9.0 que heredaba la app | Le toca a la Spec 20 |
| `kotlinx-serialization-core` | 1.7.3, transitiva vía `lifecycle` 2.11.0 | 1.11.0 `runtimeOnly`, declarada el 2026-10-11 (Spec 20): `room3-testing` 3.0.3 exige 1.8.1 o más | Le toca a la Spec 20 |
| Versiones | Todas en `gradle/libs.versions.toml` | Solo en `libs.versions.toml` | No |
| Plugin de Kotlin | Solo `kotlin.plugin.compose`. AGP 9 trae Kotlin integrado | Sin cambio | No |
| `compileSdk` y `targetSdk` | 37 | Última versión estable de Android. AGP 9.4 soporta hasta API 37 | No |
| `minSdk` | 26 | No lo fija la constitución | Se mantiene |
| Java | `sourceCompatibility` y `targetCompatibility` 11. JDK del daemon 25 vía toolchain (foojay) | AGP 9.4 exige JDK 17 o superior para ejecutar el build | No |
| `namespace` y `applicationId` | `com.example.weightrack` | Un id `com.example` no se puede publicar en Google Play | Decidido: `com.juancholopes.weightrack` |
| Permiso `INTERNET` | No declarado en el manifiesto fuente | S01-R05 | No |
| Edge-to-edge | `enableEdgeToEdge()` en `MainActivity` | Sin requisito previo | No |
| Textos en código | `Greeting` usa `"Hello $name!"` y `"Juan"` escritos en el código | Textos en recursos de strings | Sí |
| Paquetes | Solo `ui/theme` y `MainActivity` | `feature/` por funcionalidad y tres capas | Sí, se documenta abajo |
| `.gitignore` | No ignora `.kotlin/`, que Kotlin 2.x crea en la raíz | Repositorio sin archivos generados | Sí |
| Respaldo automático | `android:allowBackup="true"` con `backup_rules.xml` y `data_extraction_rules.xml` por defecto | Decidido el 2026-10-10: se mantiene (S01-R05) | No |

## Requisitos

### Versiones y plugins

| ID | Requisito |
| --- | --- |
| S03-R01 | Kotlin y el plugin `kotlin.plugin.compose` usan la última estable de la línea 2.4.x, confirmada por el agente antes de escribirla. |
| S03-R02 | La Compose BOM es la estable más reciente. Ninguna librería de Compose declara versión propia fuera de la BOM. |
| S03-R03 | AGP se mantiene en 9.4.1. `core-ktx`, `lifecycle-runtime-ktx`, `activity-compose` y las librerías de prueba existentes usan su última estable. No se agregan librerías nuevas fuera de lo que esta spec nombra. |
| S03-R04 | AGP y el Gradle wrapper quedan en versiones estables y compatibles entre sí según la tabla oficial de AGP. |
| S03-R05 | Toda versión vive solo en `gradle/libs.versions.toml`. Ningún `build.gradle.kts` escribe una versión literal. |
| S03-R06 | El plugin KSP queda declarado en el catálogo con una versión compatible con el Kotlin elegido, y en el `build.gradle.kts` raíz con `apply false`. Desde la Spec 20 se aplica en `:app`, y el plugin `androidx.room3` también se declara en la raíz con `apply false` y se aplica en `:app` (enmienda del 2026-10-10). |

Qué le toca a cada spec:

| Elemento | Spec 03 | Spec 20 |
| --- | --- | --- |
| Versión de KSP en el catálogo y plugin raíz con `apply false` | Sí | No |
| Aplicar KSP en `app` | No | Sí |
| `room3-runtime`, `room3-compiler` y su versión | No | Sí |
| Plugin de Gradle `androidx.room3` y `schemaDirectory` | No | Sí, si la Spec 20 exporta esquemas para migraciones (S01-R08) |

### Identidad y estructura

| ID | Requisito |
| --- | --- |
| S03-R07 | `namespace` y `applicationId` son `com.juancholopes.weightrack`. |
| S03-R08 | El paquete de código coincide con el `namespace` elegido, incluidas las pruebas de `test`, `androidTest` y `testFixtures` (Spec 31). |
| S03-R09 | La estructura de paquetes sigue el esqueleto de abajo. Esta spec lo documenta. Los paquetes se crean cuando la primera clase los necesita, no vacíos. |
| S03-R10 | Se elimina el composable de plantilla `Greeting` y su preview. `MainActivity` muestra un contenido mínimo sin textos escritos en el código. |
| S03-R11 | Todo texto visible para el usuario vive en `res/values/strings.xml`. |
| S03-R12 | `MainActivity` mantiene `enableEdgeToEdge()`. |
| S03-R17 | Los identificadores de código (clases, funciones, propiedades y paquetes) están en inglés. Los textos visibles están en español en `res/values/strings.xml`. |

Esqueleto de paquetes bajo `com.juancholopes.weightrack`. Los nombres de paquete van en inglés, en minúsculas y sin guiones bajos, por ejemplo `feature/catalog` o `feature/session`. El árbol y los nombres de feature los fija la Spec 30 (S30-R05 y S30-R06), y la composición de cada pantalla, S30-R12.

```
com/juancholopes/weightrack/
  MainActivity.kt           única Activity: enableEdgeToEdge() y el shell
  WeightrackApplication.kt  Application propia, crea el AppContainer (Spec 20)
  AppContainer.kt           inyección manual: construye la base de datos y los repositorios (Spec 20)
  data/                     base de datos y conversores compartidos por las features (Spec 20)
  ui/                       shell de F00: composable raíz y navegación
    theme/                  tema Material 3 compartido
  feature/
    catalog/                F01
      domain/               modelos, reglas, interfaces de repositorio y casos de uso compartidos
      data/                 implementación con Room 3, solo si la feature es dueña de tablas
      ui/                   por pantalla: <Nombre>Screen, <Nombre>Content, <Nombre>ViewModel y <Nombre>UiState
    session/                F02: domain/, data/ y ui/
    lastperformance/        F03: solo ui/
    history/                F04: solo ui/
```

Las pruebas usan los mismos paquetes en tres source sets (Spec 31): `src/test` (JVM), `src/androidTest` (dispositivo) y `src/testFixtures`, que comparten los dos anteriores. Los fakes y los contratos de los repositorios viven en `src/testFixtures`, paquete `testing` (S31-R04), y `MainDispatcherRule` en `src/test`, paquete `testing` (S31-R10).

### Build, entorno y repositorio

| ID | Requisito |
| --- | --- |
| S03-R13 | El manifiesto fusionado no declara el permiso `INTERNET` ni otro permiso de red (S01-R05). |
| S03-R14 | Sin dispositivo, `./gradlew assembleDebug test` compila y ejecuta las pruebas JVM desde un clon limpio. Con un teléfono o emulador conectado, el comando completo suma las pruebas instrumentadas: `./gradlew assembleDebug test connectedDebugAndroidTest`. Es el comando de la definición de hecho (S31-R23, S31-R24; enmiendas del 2026-10-10 y del 2026-10-11). |
| S03-R15 | El JDK del build se resuelve con el toolchain de Gradle (`gradle/gradle-daemon-jvm.properties`), en una versión que cumpla el mínimo de AGP. El nivel de bytecode (Java 11 hoy) se mantiene salvo que el plan justifique cambiarlo. |
| S03-R16 | `.gitignore` excluye `.kotlin/`, `.gradle`, `build/` y `local.properties`. Ningún archivo generado queda versionado. |
| S03-R18 | El manifiesto mantiene `android:allowBackup="true"` y las referencias a `@xml/backup_rules` y `@xml/data_extraction_rules`, con el contenido por defecto que generó Android Studio. |
| S03-R19 | La calidad de código se comprueba solo con el lint integrado de Android. No se agregan ktlint, detekt ni otra herramienta sin un ADR. |
| S03-R20 | Lint no reporta errores. Se aceptan los avisos que vienen de la plantilla (colores sin usar, etiqueta redundante de la actividad y versión más nueva de Gradle). Los colores se resuelven en la Spec 41 y la versión de Gradle cuando una spec la actualice. |

## Fuera de alcance de esta spec

- Esquema, entidades, DAOs y migraciones de Room: Spec 20.
- Inyección de dependencias: Spec 30.
- Navegación y pantallas: Spec 42.
- Tema y sistema de diseño más allá del tema generado: Spec 41.
- Estrategia de pruebas y librerías nuevas de pruebas: Spec 31.
- Formateo y análisis estático adicionales, por ejemplo ktlint o detekt: descartados por ahora (S03-R19). Se agregan después con un ADR.
- Modularización en varios módulos Gradle.
- Firma, ofuscación y publicación en Google Play.

## Criterios de aceptación

| ID | Criterio | Comprueba | Evidencia |
| --- | --- | --- | --- |
| S03-A01 | Kotlin está en la última estable 2.4.x y la Compose BOM es la estable más reciente, con la fecha de verificación anotada en el plan. | S03-R01, R02 | `gradle/libs.versions.toml` y `./gradlew :app:dependencies --configuration debugRuntimeClasspath` |
| S03-A02 | Ninguna librería de Compose ni de AndroidX queda en una versión anterior a su última estable. | S03-R03 | `./gradlew :app:dependencies --configuration debugRuntimeClasspath` |
| S03-A03 | Ningún `build.gradle.kts` contiene una versión literal. | S03-R04, R05 | `grep -nE '"[0-9]+\.[0-9]+' build.gradle.kts app/build.gradle.kts` sin resultados de dependencias o plugins |
| S03-A04 | KSP y el plugin `androidx.room3` aparecen en el catálogo y en el `build.gradle.kts` raíz con `apply false`, y se aplican en `app/build.gradle.kts` (Spec 20). | S03-R06 | Revisión de los tres archivos |
| S03-A05 | `namespace`, `applicationId` y el paquete de código usan `com.juancholopes.weightrack`. | S03-R07, R08 | `grep -rn "com.example" app/` sin resultados |
| S03-A06 | No quedan `Greeting` ni textos visibles escritos en el código. | S03-R10, R11 | `grep -rn "Greeting\|Hello" app/src/main/` sin resultados |
| S03-A07 | `MainActivity` llama a `enableEdgeToEdge()`. | S03-R12 | Revisión de `MainActivity.kt` |
| S03-A08 | El manifiesto fusionado no contiene `android.permission.INTERNET`. | S03-R13 | `./gradlew :app:processDebugMainManifest` y `grep INTERNET app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml` sin resultados |
| S03-A09 | Build y pruebas pasan desde un clon limpio. | S03-R14, R15 | `git clone <repo> /ruta/limpia && cd /ruta/limpia && ./gradlew assembleDebug test` y, con un dispositivo, `./gradlew assembleDebug test connectedDebugAndroidTest`. Si hay cambios sin commit, se copian a una carpeta nueva todos los archivos no ignorados y se ejecutan los mismos comandos allí |
| S03-A10 | Tras un build, Git no muestra archivos generados sin seguimiento. | S03-R16 | `./gradlew assembleDebug && git status --short` sin `.kotlin/`, `build/` ni `.gradle/` |
| S03-A11 | La app arranca sin cierres en la API mínima (26) y en la API 37. | S03-R07 a R12 | `./gradlew installDebug` en un emulador o teléfono físico por cada API y prueba de que la actividad queda abierta sin cierres (`adb logcat -b crash` vacío). Si no hay dispositivo para una API, se anota como pendiente y se comprueba en cuanto lo haya |
| S03-A12 | Los paquetes de código están en inglés y no hay textos visibles fuera de `strings.xml`. | S03-R17 | `find app/src -type d` bajo `com/juancholopes/weightrack` y revisión de nombres |
| S03-A13 | El manifiesto fuente contiene `android:allowBackup="true"`, `android:fullBackupContent="@xml/backup_rules"` y `android:dataExtractionRules="@xml/data_extraction_rules"`. | S03-R18 | `grep -nE 'allowBackup\|fullBackupContent\|dataExtractionRules' app/src/main/AndroidManifest.xml` |
| S03-A14 | Ningún archivo de Gradle aplica ktlint, detekt ni otro plugin de análisis estático. | S03-R19 | `grep -rniE 'ktlint\|detekt\|spotless' build.gradle.kts app/build.gradle.kts gradle/libs.versions.toml` sin resultados |

## Decisiones abiertas

- [x] **Identificador de la app.** Decidido el 2026-10-10: `com.juancholopes.weightrack` para `applicationId`, `namespace` y paquete de código. Cambiarlo después de publicar crea otra app.
- [x] **Idioma del código y de la interfaz.** Decidido el 2026-10-10 en la constitución: identificadores y paquetes en inglés, textos de la app en español en `strings.xml` (S03-R17).
- [x] **Calidad de código.** Decidido el 2026-10-10 en la constitución: ninguna herramienta adicional por ahora, solo el lint integrado de Android (S03-R19). Si se adopta una, entra con su propio ADR.
- [x] **Ajustes por la Spec 20.** Aprobado el 2026-10-10: KSP y el plugin `androidx.room3` se aplican en `:app` (S03-R06, S03-A04), el esqueleto suma `data/`, `AppContainer` y `WeightrackApplication`, y las pruebas de base de datos corren aparte con `./gradlew connectedAndroidTest` (S03-R14).
- [x] **Respaldo automático de Android.** Decidido el 2026-10-10: `allowBackup="true"` se mantiene con las reglas por defecto. El respaldo del sistema a la cuenta de Google del usuario se acepta como excepción a S01-R05, que se refiere a funciones de la app (S03-R18).
- [x] **Ajustes por la Spec 30.** Aprobado el 2026-10-11: el esqueleto de paquetes sigue el árbol de ADR-30-03, con los nombres de feature, el `ui/` raíz del shell y la composición de cada pantalla (S30-R05, S30-R06 y S30-R12).
- [x] **Ajustes por la Spec 31.** Aprobado el 2026-10-11: S03-R14 y S03-A09 usan el comando sin dispositivo y el comando completo con `connectedDebugAndroidTest` (S31-R23, S31-R24); S03-R08 y el esqueleto incluyen `src/testFixtures`; las versiones de las librerías de prueba quedan verificadas en "Estado actual".

## Fuentes

Consultadas el 2026-10-10:

- [Kotlin releases](https://kotlinlang.org/docs/releases.html): 2.4.21.
- [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping): 2026.09.00.
- [Room 3 releases](https://developer.android.com/jetpack/androidx/releases/room3): 3.0.3.
- [KSP releases](https://github.com/google/ksp/releases): 2.3.12.
- [Android Gradle Plugin release notes](https://developer.android.com/build/releases/gradle-plugin): 9.4.1, Gradle 9.6.0, JDK 17.
- [Activity](https://developer.android.com/jetpack/androidx/releases/activity), [Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle) y [Core](https://developer.android.com/jetpack/androidx/releases/core) releases.
