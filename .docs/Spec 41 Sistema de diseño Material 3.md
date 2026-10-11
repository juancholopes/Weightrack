# Spec 41: Sistema de diseño Material 3

Oct 11, 2026 · @Juan

| ID | Estado | Fase | Depende de |
| --- | --- | --- | --- |
| 41 | Aprobada | 3 UX y UI | 00, 40 |

## Propósito

Esta spec fija la mecánica del sistema de diseño de Weightrack:

- Dónde viven los tokens y cómo los lee una pantalla.
- El color dinámico y su respaldo.
- La fuente Roboto Flex: cómo se carga y cómo se exponen sus ejes.
- Los componentes compartidos.
- Los patrones que pidió la Spec 40: diálogos, avisos, errores, estados vacíos y formatos.
- Un ciclo visual rápido con previews y capturas.

No fija valores visuales. Colores, tamaños, espacios, formas y escala tipográfica son valores por defecto que el dueño ajusta cuando quiera. Ningún criterio de aceptación comprueba un valor visual exacto.

Es una spec ligera a propósito. El dueño va a iterar mucho el diseño y quiere "algo que me guste visualmente", no un diseño fijado por reglas. Por eso, en lugar de reglas, esta spec le da opciones para reaccionar: tres direcciones visuales que se cambian con una línea (ver "Direcciones visuales").

Cómo se aplica:

- La Fase 2 implementa el tema: color dinámico, Roboto Flex, tipografía, tokens y previews (ver "Plan de la Fase 2"). Se implementó el 2026-10-11 sin dispositivo; sus criterios instrumentados y las capturas siguen pendientes (ver "Verificación de la Fase 2").
- Cada spec F usa el tema y construye los componentes compartidos que le tocan (tabla "Componentes compartidos").
- La Spec 42 aplica los patrones de esta spec en cada pantalla.

El 2026-10-11 el dueño aceptó de antemano las recomendaciones y pidió avanzar rápido. Todas las decisiones quedan cerradas con la opción recomendada y conservan sus alternativas.

## Decisiones del dueño tomadas como dadas

| Tema | Decisión | Fuente |
| --- | --- | --- |
| Diseño | Material 3 (`androidx.compose.material3`) y sus componentes | Constitución |
| Tema dinámico | Color dinámico de Material You en API 31 o más. Un esquema de respaldo claro y oscuro para API 26 a 30 | Dueño, 2026-10-11 |
| Fuente | Roboto Flex, fuente variable, incluida en `res/font`. Sin fuentes descargables, porque la app funciona sin conexión | Dueño, 2026-10-11; constitución |
| Ejes de la fuente | El dueño quiere jugar con el ancho (`wdth`) y el peso (`wght`) | Dueño, 2026-10-11 |
| Diseño iterativo | Los valores visuales son ajustables. Se prefieren opciones a listas de reglas | Dueño, 2026-10-11 |
| Referencias | Slider de Material 3 Expressive (pista gruesa, manija de barra, hueco, marcas), inicio con tarjetas grandes y redondeadas, pantalla de estadísticas con barras en píldora | Imágenes del dueño, 2026-10-11 |
| Dispositivo | Solo teléfono | Spec 01 |
| Accesibilidad | Componentes Material 3 sin recortar su accesibilidad, áreas táctiles de al menos 48 dp | Constitución |
| Idioma | Textos de la interfaz en español, en `strings.xml` | Constitución |
| Capturas | Sin pruebas de capturas en la primera versión; esta spec decide si entran | Spec 31, D2 |
| Aprobación | El dueño aceptó de antemano las recomendaciones | Dueño, 2026-10-11 |

## Estado actual

Revisado el 2026-10-11 sobre el commit `f229bb9`.

| Elemento | En el código |
| --- | --- |
| `ui/theme/Theme.kt` | Plantilla de Android Studio. `WeightrackTheme(darkTheme, dynamicColor = true)`: color dinámico en API 31 o más; si no, esquemas morados de la plantilla con solo `primary`, `secondary` y `tertiary` |
| `ui/theme/Type.kt` | Solo `bodyLarge`, con `FontFamily.Default` |
| `ui/theme/Color.kt` | Seis colores de la plantilla |
| `res/values/themes.xml` | `android:Theme.Material.Light.NoActionBar`, sin versión nocturna: en modo oscuro la ventana arranca clara un instante |
| `res/values/colors.xml` | Colores de la plantilla sin uso |
| `MainActivity` | `enableEdgeToEdge()` y `WeightrackTheme { Scaffold { Text(app_name) } }` |
| Versiones | BOM 2026.09.00: `material3` 1.4.0, `ui` y `foundation` 1.12.1. `ui-tooling` en `debugImplementation` |
| Fuentes, iconos y componentes propios | No hay |

Hechos verificados el 2026-10-11 en el código fuente de las versiones de la BOM:

- En `material3` 1.4.0 las API de Material 3 Expressive (`MaterialExpressiveTheme`, `MotionScheme`, `MaterialShapes`, `ButtonGroup`) son `internal`. Llegan públicas con `material3` 1.5.0, que hoy está en beta (1.5.0-beta01, 7 de octubre de 2026).
- El `Slider` estable de `material3` 1.4.0 ya dibuja el estilo de la referencia: pista de 16 dp, manija de barra de 4 × 44 dp, hueco de 6 dp y marca de parada al final.
- `Font(resId, weight, style, loadingStrategy, variationSettings)` y `FontVariation` (`weight`, `width`, `grade`, `opticalSizing`) son estables en `ui-text` 1.12.1. Las variaciones se aplican en API 26 o más, que es el `minSdk`.
- Compose suma el ajuste de "texto en negrita" del sistema al eje `wght` de una fuente variable en API 31 o más.
- Compose guarda en caché las últimas 16 tipografías resueltas (`LruCache(16)` en `FontFamilyResolver`).
- `material3` 1.4.0 no trae iconos. `material-icons-core` está congelado en 1.7.8 dentro de la BOM.

## Principios

1. **La mecánica es fija; los valores se ajustan.** Dónde vive un token y cómo se lee no cambia sin enmendar esta spec. Su valor lo cambia el dueño cuando quiera.
2. **Material 3 primero.** Se usan sus componentes y sus roles de color (`primary`, `onPrimary`, `primaryContainer`...). Un color nunca se escribe en una pantalla.
3. **Un solo lugar para cada token.** Todo vive en `ui/theme/`.
4. **El suelo de accesibilidad no se negocia.** Contraste, áreas táctiles, escala de fuente, movimiento reducido y nada que dependa solo del color.
5. **Se ve antes de decidir.** Cada cambio visual se puede ver en previews en segundos.
6. **Sin dependencias nuevas.** Lo que falta de Material 3 Expressive se imita con poco código o espera a `material3` 1.5.0 estable.

## Mecánica

### Archivos y tokens

Todo vive en `app/src/main/java/com/juancholopes/weightrack/ui/theme/`.

| Archivo | Contiene | Notas |
| --- | --- | --- |
| `Theme.kt` | `@Composable fun WeightrackTheme(darkTheme, dynamicColor, direction, content)`, el objeto `WeightrackTheme` con sus lectores (`spacing`, `numbers`, `motion`, `direction`) y `LocalWeightrackTokens` | Único punto de entrada del tema |
| `ColorSchemes.kt` | `selectColorScheme(...)` (función pura) y los esquemas de respaldo `FallbackLightColors` y `FallbackDarkColors` con todos los roles | Reemplaza a `Color.kt` |
| `RobotoFlex.kt` | Rangos de los ejes, `robotoFlex(...)`, `robotoFlexFamily(...)` y `quantizeAxis(...)` | Ver "Roboto Flex" |
| `Type.kt` | `weightrackTypography(width)` y `NumberStyles` para pesos y repeticiones | |
| `Shape.kt` | Las formas de Material 3 (`Shapes`) por dirección | |
| `Spacing.kt` | `Spacing` con la escala de espacios | |
| `Motion.kt` | Resortes y duraciones, `rememberReducedMotion()` | Imita los tokens de movimiento de Material 3 |
| `Directions.kt` | `VisualDirection` y `DefaultDirection` | Ver "Direcciones visuales" |
| `Previews.kt` | `@ThemePreviews`, `ThemeGallery` y el laboratorio `DesignLab` | Solo previews |
| `Formats.kt` | Formatos de peso, repeticiones, serie y fecha | Lo agrega la primera spec F que muestra un peso o una fecha (F02) |
| `component/` | Componentes compartidos sin estado | Ver "Componentes compartidos" |

Los tokens propios que Material 3 no tiene (`Spacing`, `NumberStyles`, `MotionTokens`, la dirección) viajan juntos en `WeightrackTokens` por un `staticCompositionLocalOf`. Los colores, la tipografía de texto y las formas van en `MaterialTheme`.

### Cómo recibe el tema una pantalla

```kotlin
// MainActivity: sin cambio de forma
setContent { WeightrackTheme { WeightrackApp() } }

// Cualquier Content o componente
Text(
    text = stringResource(R.string.example),
    style = MaterialTheme.typography.titleLarge,
    color = MaterialTheme.colorScheme.onSurface,
)
Spacer(Modifier.height(WeightrackTheme.spacing.l))
Text(text = weightText, style = WeightrackTheme.numbers.hero)
```

- Una pantalla nunca crea su propio `MaterialTheme` ni lee un color, una fuente o un tamaño de texto escrito a mano.
- Cada pantalla tiene su propio `Scaffold`, con su barra superior y su `SnackbarHost` (Spec 42). El shell no envuelve `NavDisplay` en otro `Scaffold`, para no aplicar los insets dos veces (skill `edge-to-edge`).
- Los espacios estructurales de una sola pantalla pueden escribirse en `dp` en su archivo si no se repiten. Lo que se repite sale de `Spacing`.

### Color dinámico con respaldo

```kotlin
fun selectColorScheme(
    sdkInt: Int,
    dynamicColor: Boolean,
    darkTheme: Boolean,
    dynamicLight: () -> ColorScheme,
    dynamicDark: () -> ColorScheme,
): ColorScheme = when {
    dynamicColor && sdkInt >= Build.VERSION_CODES.S -> if (darkTheme) dynamicDark() else dynamicLight()
    darkTheme -> FallbackDarkColors
    else -> FallbackLightColors
}
```

- `WeightrackTheme` la llama con `Build.VERSION.SDK_INT`, `dynamicLightColorScheme(context)` y `dynamicDarkColorScheme(context)`. Al ser pura, se prueba en la JVM.
- `dynamicColor` vale `true` por defecto.
- **Respaldo.** Esquemas claro y oscuro completos, con todos los roles, generados fuera de la app con Material Theme Builder a partir de una semilla y pegados en `ColorSchemes.kt`. Semilla por defecto: `#6750A4`, el morado de referencia de Material 3, que es el tono de las imágenes del dueño. Es ajustable.
- **Modo oscuro.** Sigue al sistema con `isSystemInDarkTheme()`. No hay selector dentro de la app: la Spec 40 deja fuera la pantalla de ajustes.
- **Ventana.** `res/values-night/themes.xml` usa `android:Theme.Material.NoActionBar`, para que la ventana no arranque clara en modo oscuro.
- **Barras del sistema.** `enableEdgeToEdge()` de `ComponentActivity` ajusta el color de los iconos solo. No se tocan `isAppearanceLightStatusBars` ni `isAppearanceLightNavigationBars` (skill `edge-to-edge`).
- **Solo roles.** No hay colores semánticos propios, por ejemplo "éxito" o "récord". Se usan `tertiary` y sus contenedores. Un color propio no se armonizaría con el color dinámico sin una librería nueva.

### Roboto Flex

**Archivo.** `RobotoFlex[GRAD,XOPQ,XTRA,YOPQ,YTAS,YTDE,YTFI,YTLC,YTUC,opsz,slnt,wdth,wght].ttf` de `google/fonts`, carpeta `ofl/robotoflex`. Viene de la versión 3.200 del proyecto Roboto Flex (30 de junio de 2023) y pesa 1 787 292 bytes. En la app se llama `res/font/roboto_flex.ttf`, porque Android solo acepta minúsculas, dígitos y guiones bajos en los nombres de recursos.

**Licencia.** SIL Open Font License 1.1. Lo que obliga al incluirla en la app:

- Se puede incluir, incrustar y redistribuir dentro de un programa, incluso uno que se venda.
- La fuente no se puede vender sola.
- Cada copia lleva el aviso de copyright y el texto de la licencia. Puede ir como archivo de texto aparte. Weightrack lo incluye en `app/src/main/assets/licenses/roboto_flex_ofl.txt`, con la línea "Copyright 2017 The Roboto Flex Project Authors".
- Una versión modificada, por ejemplo recortada, sigue bajo la misma licencia. Roboto Flex no declara un nombre de fuente reservado, así que una versión recortada puede conservar el nombre.

**Ejes.** Rangos tomados del `METADATA.pb` de `google/fonts`.

| Eje | Etiqueta | Rango | Por defecto | ¿Se expone? |
| --- | --- | --- | --- | --- |
| Peso | `wght` | 100 a 1000 | 400 | Sí |
| Ancho | `wdth` | 25 a 151 | 100 | Sí |
| Grado (peso sin cambiar el ancho) | `GRAD` | −200 a 150 | 0 | Sí |
| Tamaño óptico | `opsz` | 8 a 144 | 14 | Sí, opcional |
| Inclinación | `slnt` | −10 a 0 | 0 | No |
| Ejes paramétricos | `XOPQ`, `XTRA`, `YOPQ`, `YTAS`, `YTDE`, `YTFI`, `YTLC`, `YTUC` | Varios | Los de la fuente | No |

**Ayudante.** Un solo lugar construye las fuentes:

```kotlin
// ui/theme/RobotoFlex.kt
object RobotoFlexAxes {
    val Weight = 100..1000
    val Width = 25f..151f
    val Grade = -200..150
    val OpticalSize = 8f..144f
}

fun robotoFlex(weight: Int = 400, width: Float = 100f, grade: Int = 0): Font {
    val w = weight.coerceIn(RobotoFlexAxes.Weight)
    return Font(
        resId = R.font.roboto_flex,
        weight = FontWeight(w),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(w),
            FontVariation.width(width.coerceIn(RobotoFlexAxes.Width)),
            FontVariation.grade(grade.coerceIn(RobotoFlexAxes.Grade)),
        ),
    )
}

fun robotoFlexFamily(
    width: Float = 100f,
    weights: List<Int> = listOf(300, 400, 500, 700, 800, 900),
    grade: Int = 0,
): FontFamily = FontFamily(weights.map { robotoFlex(it, width, grade) })

/** Redondea un valor animado a pasos, para no crear una tipografía nueva por cuadro. */
fun quantizeAxis(value: Float, step: Float): Float = (value / step).roundToInt() * step
```

- **Tipografía de texto.** `weightrackTypography(width)` copia la escala de Material 3 y le pone `robotoFlexFamily(width)` a cada estilo. Los pesos de cada estilo siguen los de Material 3 (400 y 500).
- **Números.** `NumberStyles` tiene los estilos de pesos y repeticiones: `hero` (el número grande del registro), `card` (el peso de una tarjeta) y `inline` (las series en una lista). Usan otra familia, por ejemplo ancha y pesada, con cifras tabulares (`fontFeatureSettings = "tnum"`) para que los dígitos no salten al cambiar el valor. Si la fuente no trae `tnum`, la Fase 2 lo anota y se quita.
- **Animar un eje.** Se anima un `Float`, se redondea con `quantizeAxis` (por ejemplo, pasos de 4 en `wdth` y de 50 en `wght`) y se recuerda la familia por valor redondeado. Así una animación usa pocas variantes y cabe en la caché de 16 de Compose. Si un número animado baja de 60 cuadros por segundo, se cambia por una escala con `graphicsLayer`.
- **Accesibilidad.** La escala de fuente del sistema se respeta siempre, porque los tamaños van en `sp`. El ajuste de "texto en negrita" del sistema suma al eje `wght` sin código extra.

### Formas, espacio y movimiento

Valores por defecto de la dirección "Expresiva". Son ajustables y ningún criterio los comprueba.

| Token | Por defecto |
| --- | --- |
| `Spacing` | `xxs` 2, `xs` 4, `s` 8, `m` 12, `l` 16, `xl` 24, `xxl` 32, `xxxl` 48 (dp) |
| `Shapes` | `extraSmall` 8, `small` 12, `medium` 16, `large` 24, `extraLarge` 32 (dp). Tarjetas con `extraLarge` y botones en píldora |
| Texto | Escala de Material 3 con `wdth` 100 |
| Números | `hero` 64 sp, `wdth` 151, `wght` 800; `card` 32 sp, `wdth` 140, `wght` 700; `inline` del tamaño del texto, `wght` 600 |

**Movimiento.** `MotionScheme` es `internal` en `material3` 1.4.0, así que `Motion.kt` copia los valores de sus tokens (código fuente de `material3` 1.4.0, revisado el 2026-10-11):

| Resorte | Expresivo (amortiguación / rigidez) | Estándar |
| --- | --- | --- |
| Espacial rápido | 0,6 / 800 | 0,9 / 1400 |
| Espacial normal | 0,8 / 380 | 0,9 / 700 |
| Espacial lento | 0,8 / 200 | 0,9 / 300 |
| Efectos rápido | 1,0 / 3800 | 1,0 / 3800 |
| Efectos normal | 1,0 / 1600 | 1,0 / 1600 |
| Efectos lento | 1,0 / 800 | 1,0 / 800 |

- "Espacial" mueve cosas (posición, escala). "Efectos" cambia color u opacidad, sin rebote.
- Cada dirección elige su esquema (ver "Direcciones visuales"). Cuando `material3` 1.5.0 sea estable, `Motion.kt` pasa a leer `MaterialTheme.motionScheme` sin cambiar a quien lo usa.

**Movimiento reducido.** Compose respeta la escala de animaciones del sistema. Además, `rememberReducedMotion()` devuelve `true` cuando `ValueAnimator.areAnimatorsEnabled()` es `false` (API 26 o más; lo activan "Quitar animaciones" de Accesibilidad y el ahorro de batería). Con movimiento reducido:

- Se quita el desplazamiento y la escala; se conserva la opacidad.
- Se quitan los rebotes.
- No se animan los ejes de la fuente: el número cambia de golpe.
- Nada se repite en bucle.

### Componentes compartidos

Viven en `ui/theme/component/`, no tienen estado, usan componentes Material 3 por dentro, miden al menos 48 dp donde se tocan y exponen su semántica. Cada uno lo construye la primera spec que lo necesita, con previews (S41-R16).

| Componente | Qué es | Lo construye |
| --- | --- | --- |
| `PillTabs` | Fila de pestañas en píldora: la elegida, rellena; las demás, solo texto. Rol `Tab`, grupo seleccionable | F00 |
| `ExerciseCard` | Tarjeta grande de un ejercicio: nombre, subtítulo, un hueco para el peso, menú y toque. Recibe un `CardTone` (`Primary`, `Secondary`, `Tertiary`, `Neutral`) y no conoce el dominio | F01 |
| `WeightNumber` | Número de peso o repeticiones con `NumberStyles`, con cambio animado de dígitos y ejes | F02 |
| `EmptyState` | Estado vacío: texto, acción opcional | F01 |
| `ConfirmDialog` | `AlertDialog` con título de acción, consecuencia y botones con verbo | F01 |

Los controles de peso y repeticiones los usa solo F02, así que viven en `feature/session/ui` (Spec 42).

### Patrones de la Spec 40

| Patrón | Cómo se hace |
| --- | --- |
| Diálogo de confirmación | `ConfirmDialog`: el título dice la acción como pregunta ("¿Eliminar esta serie?"), el texto dice la consecuencia y los botones dicen la acción y la alternativa ("Eliminar serie" / "Conservar"). La acción destructiva va con el color `error`. Atrás o tocar fuera cancela. Si el diálogo está abierto lo dice el `UiState` |
| Aviso (snackbar) | Uno por pantalla, en el `SnackbarHost` de su `Scaffold`. Sin acción: `SnackbarDuration.Short` (4 s). Con acción, por ejemplo "Deshacer": `SnackbarDuration.Long` (10 s). Compose alarga los dos según el ajuste de accesibilidad "Tiempo para actuar" (`calculateRecommendedTimeoutMillis`). Nunca `Indefinite` |
| Error junto al campo | En un campo de texto: `isError` y `supportingText` con el mensaje. En un grupo (equipo, músculos, peso): el mensaje debajo del grupo, con semántica de error y región en vivo. Tras un guardado fallido, el foco va al primer campo con error (S40-R47) |
| Estado vacío | `EmptyState`: qué es la vista, por qué está vacía y la acción útil (tabla "Estados vacíos" de la Spec 40) |
| Carga | Mientras el `UiState` no tiene su primer dato, la pantalla no muestra su estado vacío. Así no parpadea "Aún no tienes ejercicios" antes de que llegue la lista |
| Marca sin color | Principal o secundario, archivado, error y récord llevan texto o forma además del color (S40-R49) |

### Formatos

Funciones puras en `ui/theme/Formats.kt`, probadas en la JVM. Los nombres de días y meses llegan como listas leídas de `strings.xml`, así que la prueba no depende del idioma del equipo.

| Valor | Formato visible | Lectura de TalkBack |
| --- | --- | --- |
| Peso | "62,5 kg". Coma decimal, sin ceros sobrantes ("60 kg", "62,25 kg"), sin separador de miles ("1000 kg") | "62,5 kilogramos"; "1 kilogramo" |
| Repeticiones | "10" junto a su etiqueta | "10 repeticiones"; "1 repetición" |
| Serie | "60 kg × 10" | "60 kilogramos, 10 repeticiones" |
| Fecha corta | "jue 8 oct". "Hoy" y "Ayer" para esos días. El año solo si no es el actual: "jue 8 oct 2025" | "jueves 8 de octubre" |

- Las abreviaturas son minúsculas y sin punto: lun, mar, mié, jue, vie, sáb, dom; ene, feb, mar, abr, may, jun, jul, ago, sept, oct, nov, dic.
- No se usa `DateTimeFormatter` con `Locale("es")`, porque Android y la JVM no dan las mismas abreviaturas (puntos, "sept" o "sep.").
- "Hoy" sale del reloj del `AppContainer` (ADR-30-09): el `UiState` trae la fecha de hoy y el `Content` formatea.

### Tono de los textos

Lo aplica la Spec 42 a todos los textos, con la skill `ux-copy`:

- Tú, cercano y corto. Frases de pocas palabras: el usuario lee entre series.
- Las acciones empiezan con un verbo y dicen lo que pasa ("Guardar serie", no "Aceptar").
- Los errores dicen qué pasó y cómo corregirlo, sin culpar.
- Los términos del dominio son los del glosario (Spec 02). "Máquina" solo es la etiqueta del equipo `MACHINE`.

### Ciclo visual: previews, laboratorio y capturas

1. **Previews en Android Studio.** `@ThemePreviews` es una anotación múltiple con seis vistas: claro, oscuro, dinámico con fondo rojo, dinámico con fondo azul en oscuro, respaldo (`apiLevel = 30`, que fuerza el esquema de respaldo) y fuente al 200 %. Cada `Content` y cada componente compartido la usa con datos de ejemplo. Los datos de ejemplo pueden ir en el código porque el usuario no los ve.
2. **Laboratorio (`DesignLab`).** Una preview interactiva en `Previews.kt` con sliders para `wdth`, `wght` y `GRAD`, un selector de dirección, claro u oscuro y color dinámico sí o no. Muestra la escala de texto, los números, una tarjeta y un slider de muestra con los tokens de la dirección elegida. Su contenido es solo de muestra y no se reutiliza en la app.
3. **En el teléfono.** Android Studio puede correr cualquier preview en el teléfono ("Run Preview"). Usa `PreviewActivity`, que llega con `ui-tooling` solo en la build debug. Un agente lo abre y saca una captura así:

   ```
   adb shell am start -n com.juancholopes.weightrack/androidx.compose.ui.tooling.PreviewActivity \
     --es composable com.juancholopes.weightrack.ui.theme.PreviewsKt.DesignLabPreview
   adb exec-out screencap -p > design-lab.png
   ```

4. **Sin pruebas de capturas automáticas** en la primera versión (D9). Las capturas son evidencia manual para que el dueño reaccione.

## Direcciones visuales

Tres direcciones para que el dueño reaccione, hechas con la skill `genjutsu:paint`. Todas usan color dinámico y Roboto Flex. Ninguna queda fija: las tres viven como presets de `VisualDirection`, se comparan en `DesignLab` y la app usa la que diga `DefaultDirection`. Cambiar de dirección es cambiar esa línea.

Lo que cambia entre direcciones: formas, ancho y peso de texto y números, escala de espacios, cómo se colorean las tarjetas y el esquema de movimiento. El esquema de color de respaldo es uno solo, porque en el teléfono del dueño (API 31 o más) manda el color dinámico.

**A. Expresiva (por defecto).** La más cercana a las referencias del dueño.

- *Tesis visual.* Tarjetas grandes y planas en los contenedores tonales del color dinámico (`primaryContainer`, `secondaryContainer`, `tertiaryContainer`), una por tono según el equipo del ejercicio. Esquinas muy redondeadas (32 dp en tarjetas, píldora en botones y pestañas). Pesos con números anchos y pesados (`wdth` 151, `wght` 800) sobre texto normal (`wdth` 100). Espacio amplio (base 16 dp, 24 dp entre secciones). Sin sombras: el color separa.
- *Tesis de interacción.* Resortes del esquema expresivo, de 250 a 400 ms. Al pulsar una tarjeta, escala 0,96 y cambio de tono. Al cambiar un peso, los dígitos ruedan y el número se ensancha y se engruesa un instante (`wdth` 151 → 125 → 151, `wght` 800 → 900). Al desplazar, nada salvo el color de la barra superior. Prohibido: paralaje, bucles, rebote mayor al 10 % y animar tamaños de layout.

**B. Marcador.** El peso como un marcador de estadio.

- *Tesis visual.* Superficies de alto contraste (`surfaceContainerHighest`) con un borde de color `primary` a la izquierda de cada tarjeta. Números condensados y altos (`wdth` 25 a 50, `wght` 900), texto algo estrecho (`wdth` 85). Esquinas medias (12 dp). Espacio denso (base 8 dp, 16 dp entre secciones). Plano, con bordes.
- *Tesis de interacción.* Rápida y seca, de 120 a 200 ms, con la curva de desaceleración de Material 3 y sin rebote. Los dígitos cambian como una paleta que gira. Al pulsar, cambio de tono sin escala. Prohibido: resortes con rebote, escalas y paralaje.

**C. Cuaderno.** Calma y aire.

- *Tesis visual.* Casi todo en `surface`, tarjetas con borde fino (`outlineVariant`) y esquinas de 20 dp. Números grandes y ligeros (`wdth` 110, `wght` 300) que ganan énfasis con `GRAD` en lugar de con peso. Mucho espacio (base 20 dp, 32 dp entre secciones). El color solo en las acciones.
- *Tesis de interacción.* Tranquila, de 250 a 350 ms, con la curva estándar de Material 3 (0,2; 0; 0; 1). Fundidos. Al cambiar un peso, fundido cruzado y `wght` de 300 a 500. Al pulsar, solo la onda de Material. Prohibido: rebote, desplazamientos de más de 24 dp y dígitos que ruedan.

| | A. Expresiva | B. Marcador | C. Cuaderno |
| --- | --- | --- | --- |
| Tarjetas | Contenedor tonal | Superficie alta con borde de color | Superficie con borde fino |
| Esquinas | 32 dp | 12 dp | 20 dp |
| Números | Anchos y pesados | Condensados y muy pesados | Ligeros, énfasis con grado |
| Espacio | Amplio | Denso | Muy amplio |
| Movimiento | Resortes expresivos | Curvas rápidas sin rebote | Curvas estándar tranquilas |
| Riesgo | Mucho color en catálogos largos | Puede verse duro en modo claro | Menos jerarquía entre series |

## Requisitos

| ID | Requisito |
| --- | --- |
| S41-R01 | Los tokens viven solo en `ui/theme/`, en los archivos de "Archivos y tokens". Fuera de `ui/theme/` no se escriben colores (`Color(0x...)`), fuentes (`Font(`, `FontFamily(`) ni tamaños de texto (`fontSize =`). |
| S41-R02 | `WeightrackTheme` es el único punto de entrada del tema. `MainActivity` envuelve el shell con él y cada preview también. Sus parámetros son `darkTheme` (por defecto, el del sistema), `dynamicColor` (por defecto, `true`) y `direction` (por defecto, `DefaultDirection`). |
| S41-R03 | El esquema de color sale de `selectColorScheme`: dinámico en API 31 o más con `dynamicColor`; si no, el respaldo claro u oscuro, con todos los roles definidos. |
| S41-R04 | El modo oscuro sigue al sistema y no tiene selector en la app. `res/values-night/themes.xml` evita que la ventana arranque clara en modo oscuro. |
| S41-R05 | Roboto Flex vive en `res/font/roboto_flex.ttf`. La app no usa fuentes descargables ni `ui-text-google-fonts`, y no pide permiso de red (S01-R05). |
| S41-R06 | La app incluye el texto de la licencia OFL 1.1 y el aviso de copyright de Roboto Flex en `app/src/main/assets/licenses/roboto_flex_ofl.txt`. Una versión recortada de la fuente sigue bajo OFL. |
| S41-R07 | Toda fuente de la app se construye con `robotoFlex` o `robotoFlexFamily`, que limitan cada eje a su rango. Se exponen `wght`, `wdth`, `GRAD` y, de forma opcional, `opsz`. |
| S41-R08 | Un eje animado se redondea con `quantizeAxis` antes de crear la familia, para que una animación use pocas variantes. |
| S41-R09 | Formas, espacios y movimiento son tokens de `ui/theme/`. Los resortes copian los tokens de Material 3 de "Movimiento" hasta que `material3` exponga `MotionScheme`. |
| S41-R10 | Toda animación tiene una versión de movimiento reducido según "Movimiento reducido", decidida con `rememberReducedMotion()`. |
| S41-R11 | Cada pantalla tiene su propio `Scaffold` y aplica los insets con un solo método (skill `edge-to-edge`). El shell no agrega padding alrededor de `NavDisplay`. |
| S41-R12 | Los componentes compartidos viven en `ui/theme/component/`, no tienen estado, no importan ningún paquete `feature`, miden al menos 48 dp donde se tocan y exponen su semántica. |
| S41-R13 | Diálogos, avisos, errores, estados vacíos, carga y marcas siguen la tabla "Patrones de la Spec 40". |
| S41-R14 | Peso, repeticiones, series y fechas se muestran y se leen con los formatos de "Formatos", desde `ui/theme/Formats.kt`. |
| S41-R15 | El suelo de accesibilidad se cumple: texto sobre fondo con pares de roles de Material 3, áreas táctiles de 48 dp, la escala de fuente del sistema hasta 200 % sin perder la acción principal de una pantalla, y nada que dependa solo del color. |
| S41-R16 | Cada `Content` y cada componente compartido tiene una preview con `@ThemePreviews`, envuelta en `WeightrackTheme`. |
| S41-R17 | `DesignLab` permite cambiar `wdth`, `wght`, `GRAD`, la dirección, claro u oscuro y el color dinámico, y se puede abrir en el teléfono con `PreviewActivity`. |
| S41-R18 | Las capturas para el dueño se sacan a mano desde las previews o desde el teléfono. No hay pruebas de capturas automáticas en la primera versión (D9). |
| S41-R19 | Los valores visuales son ajustables. El dueño los cambia sin enmendar esta spec. Un agente no cambia valores visuales por su cuenta: solo cuando su tarea lo pide. |
| S41-R20 | Las direcciones A, B y C viven como presets de `VisualDirection`. `DefaultDirection` elige la de la app, y cambiarla es cambiar una línea. |
| S41-R21 | Los iconos son Material Symbols (redondeados) copiados como vectores en `res/drawable`, solo los que se usan. No se agrega ninguna librería de iconos. |

## Decisiones

Todas cerradas el 2026-10-11 con la opción recomendada. Cada una conserva sus alternativas.

- [x] **D1. Dónde viven los tokens.** Decidido el 2026-10-11: A, `ui/theme/` con `MaterialTheme` más un `CompositionLocal` propio (el dueño aceptó de antemano las recomendaciones). A: colores, tipografía y formas en `MaterialTheme`; espacios, números, movimiento y dirección en `WeightrackTokens`. B: solo `MaterialTheme` y constantes de nivel superior para lo demás. C: la API Styles (D6). Recomendación: A. Es la forma de los temas personalizados de Compose y permite cambiar de dirección en una preview. B no deja variar espacios ni números por dirección. Costo: un `CompositionLocal` más.
- [x] **D2. Color de respaldo para API 26 a 30.** Decidido el 2026-10-11: A, esquemas fijos generados fuera de la app (el dueño aceptó de antemano las recomendaciones). A: generarlos con Material Theme Builder desde una semilla y pegarlos completos. B: generarlos al vuelo con `material-color-utilities`. C: los colores de la plantilla. Recomendación: A. Sin dependencias y con todos los roles. B agrega una librería para pocos teléfonos. C deja roles sin definir y se ve como cualquier plantilla. Costo: regenerar y pegar si cambia la semilla.
- [x] **D3. Modo oscuro.** Decidido el 2026-10-11: A, sigue al sistema (el dueño aceptó de antemano las recomendaciones). A: `isSystemInDarkTheme()`, sin selector. B: un selector en la app. Recomendación: A. B exige una pantalla de ajustes, fuera de alcance (Spec 40). Costo: ninguno hoy.
- [x] **D4. Archivo de la fuente.** Decidido el 2026-10-11: A, la fuente variable completa (el dueño aceptó de antemano las recomendaciones). A: el TTF completo de 1,79 MB. B: recortarlo con fonttools a latín y a los ejes expuestos. C: instancias estáticas. Recomendación: A. Es lo más simple y deja jugar con todos los ejes. B ahorra espacio pero agrega un paso de build fuera de Gradle y una versión modificada. C pierde los ejes continuos, que es lo que el dueño quiere. Costo: el APK crece unos 1,8 MB; B queda como mejora posterior.
- [x] **D5. Ejes expuestos.** Decidido el 2026-10-11: A, `wght`, `wdth`, `GRAD` y `opsz` opcional (el dueño aceptó de antemano las recomendaciones). A: esos cuatro. B: los trece. C: solo `wght`. Recomendación: A. Cubre lo que pidió el dueño (ancho y peso) más el grado, que da énfasis sin mover el texto. B expone ejes paramétricos difíciles de usar bien. C desperdicia la fuente. Costo: ninguno.
- [x] **D6. API Styles de Compose.** Decidido el 2026-10-11: A, no por ahora (el dueño aceptó de antemano las recomendaciones). A: no usarla. B: usarla para los componentes propios. Recomendación: A. Existe en `foundation` 1.12.1, pero es experimental (`ExperimentalFoundationStyleApi`), no sirve para los componentes Material 3 (lo dice la skill `styles`) y Weightrack tendrá pocos componentes propios. Se reconsidera si Material 3 agrega parámetros `Style` a sus componentes o si hay tres o más componentes propios con estados. Costo: si llega, migrar esos componentes.
- [x] **D7. Movimiento.** Decidido el 2026-10-11: A, tokens propios que copian los de Material 3 (el dueño aceptó de antemano las recomendaciones). A: `Motion.kt` con los resortes de los tokens. B: subir `material3` a 1.5.0-beta01 para usar `MotionScheme`. C: solo curvas con duración fija. Recomendación: A. Da el mismo movimiento con la versión estable. B es beta y trae cambios que rompen `Slider` (sobrecargas con estado, `onValueChange` obligatorio). C pierde la sensación de Material 3 Expressive. Costo: cambiar `Motion.kt` cuando 1.5.0 sea estable.
- [x] **D8. Versión de Material 3.** Decidido el 2026-10-11: A, seguir con la BOM 2026.09.00 y `material3` 1.4.0 (el dueño aceptó de antemano las recomendaciones). A: la BOM actual. B: `material3` 1.5.0-beta01 fuera de la BOM. Recomendación: A. El slider de la referencia ya está en 1.4.0. La constitución pide la BOM estable más reciente, y 2026.09.00 lo es. Lo que falta de Expressive (formas de galleta, grupos de botones, esquema de movimiento) se imita o espera. Se reconsidera cuando una BOM estable traiga `material3` 1.5.0. Costo: algunas piezas expresivas hechas a mano.
- [x] **D9. Pruebas de capturas.** Decidido el 2026-10-11: A, sin capturas automáticas; ciclo con previews, `DesignLab` y capturas a mano (el dueño aceptó de antemano las recomendaciones). A: lo descrito en "Ciclo visual". B: Compose Preview Screenshot Testing. C: Roborazzi. D: Dropshots. Recomendación: A. Con un diseño que el dueño cambia cada semana, las imágenes de referencia caducarían enseguida. B sigue en alfa y su plugin queda deprecado con AGP 9.5, que hoy está en alfa (9.5.0-alpha09). C exige Robolectric, descartado en la Spec 31 (D1). D agrega un plugin de terceros. Se reconsidera cuando el diseño se estabilice y el proyecto use un AGP 9.5 estable. Costo: un cambio visual no deseado se ve a mano.
- [x] **D10. Pruebas con fuente grande.** Decidido el 2026-10-11: A, una prueba instrumentada por pantalla principal con la fuente al 200 % (el dueño aceptó de antemano las recomendaciones). A: con `DeviceConfigurationOverride.FontScale(2f)`, comprobar que la acción principal de Hoy y del registro de series se ve y se puede tocar. Las escriben F00 y F02. B: solo previews. C: nada. Recomendación: A. La escala de fuente grande es justo lo que rompe un panel inferior fijo, y la prueba usa `ui-test`, que ya está. Costo: dos pruebas más en el dispositivo.
- [x] **D11. Direcciones visuales.** Decidido el 2026-10-11: A, tres presets intercambiables, con "Expresiva" por defecto (el dueño aceptó de antemano las recomendaciones). A: A, B y C como presets. B: elegir una y borrar las otras. C: una sola dirección sin alternativas. Recomendación: A. El dueño quiere reaccionar a opciones, y los presets son valores, no pantallas distintas. Costo: tres juegos de valores; cuando el dueño elija, los demás se borran.
- [x] **D12. Paquete de los componentes compartidos.** Decidido el 2026-10-11: A, `ui/theme/component/` (el dueño aceptó de antemano las recomendaciones). A: dentro de `ui/theme/`. B: un paquete nuevo `ui/designsystem/`. C: cada feature con los suyos. Recomendación: A. Queda dentro del árbol aprobado de ADR-30-03 y todas las features pueden importarlo sin ciclos. B exige cambiar ese árbol. C impide compartir la tarjeta entre el catálogo y Hoy, porque una feature no importa la UI de otra (S30-R10). Costo: `ui/theme/` crece; se anota en la Spec 30.
- [x] **D13. Iconos.** Decidido el 2026-10-11: A, Material Symbols como vectores en `res/drawable` (el dueño aceptó de antemano las recomendaciones). A: copiar solo los que se usan, en estilo redondeado. B: `material-icons-core` 1.7.8. C: `material-icons-extended`. Recomendación: A. No agrega librerías. B está congelado desde la línea 1.7 y no tiene los símbolos nuevos. C pesa varios megas. Costo: copiar cada icono a mano. Material Symbols usa la licencia Apache 2.0, que se anota junto a la de la fuente.
- [x] **D14. Formato de fecha.** Decidido el 2026-10-11: A, "jue 8 oct" con "Hoy" y "Ayer" (el dueño aceptó de antemano las recomendaciones). A: abreviaturas propias en `strings.xml`, año solo si no es el actual. B: `DateTimeFormatter` con `Locale("es")`. C: números ("08/10"). Recomendación: A. Es el ejemplo de la Spec 40, se lee rápido y se prueba en la JVM. B da resultados distintos en Android y en la JVM. C es ambiguo y menos legible. Costo: dos listas de nombres en `strings.xml`.
- [x] **D15. Formato de peso.** Decidido el 2026-10-11: A, "62,5 kg" (el dueño aceptó de antemano las recomendaciones). A: coma decimal, sin ceros sobrantes y sin separador de miles. B: siempre dos decimales ("62,50 kg"). C: el formato numérico de la configuración regional. Recomendación: A. Es el formato de la Spec 02 y el más corto. B agrega ruido. C pondría "1.000 kg", que confunde con un decimal. Costo: ninguno.

## Fuera de alcance

- Valores visuales definitivos: son ajustables (S41-R19).
- Selector de tema o de dirección dentro de la app.
- Esquemas propios de contraste medio y alto.
- Colores semánticos propios (éxito, aviso, récord).
- `material3` 1.5.0 y las API de Material 3 Expressive que todavía son `internal`.
- La API Styles (D6).
- Pruebas de capturas automáticas (D9).
- Recortar la fuente (D4, opción B).
- Tablets, plegables y layouts adaptables (Spec 01).
- Las pantallas, sus layouts y sus textos: Spec 42.

## Criterios de aceptación

Comprueban la mecánica, nunca un valor visual. S41-A01 a S41-A09 los comprueba la Fase 2. S41-A10 a S41-A12 los comprueba cada spec F. La evidencia sigue la Spec 31 (S31-R21, S31-R25). `<pkg>` es `app/src/main/java/com/juancholopes/weightrack`.

| ID | Criterio | Comprueba | Evidencia |
| --- | --- | --- | --- |
| S41-A01 | Fuera de `ui/theme/` no hay colores, fuentes ni tamaños de texto escritos a mano. | S41-R01, S41-R07 | `grep -rnE "Color\(0x\|Font\(\|FontFamily\(\|fontSize *=" <pkg> \| grep -v "/ui/theme/"` sin resultados |
| S41-A02 | El esquema es dinámico en API 31 o más con `dynamicColor`, y de respaldo en API 30, sin `dynamicColor` o en modo oscuro sin dinámico. | S41-R03 | `ColorSchemesTest` en `src/test`, con los cuatro casos de `selectColorScheme` |
| S41-A03 | En los esquemas de respaldo, los pares de texto principales tienen un contraste de al menos 4,5:1. | S41-R03, S41-R15 | `ColorSchemesTest.fallbackSchemes_textPairs_meetContrast`, que calcula el contraste con `Color.luminance()` para `onPrimary`/`primary`, `onSurface`/`surface`, `onSurfaceVariant`/`surface` y los tres pares de contenedores |
| S41-A04 | En un teléfono con API 31 o más, el tema usa el esquema dinámico. | S41-R03 | `WeightrackThemeTest.dynamicColor_onApi31OrHigher_usesDynamicScheme` en `src/androidTest`, que se omite por debajo de API 31 |
| S41-A05 | La fuente viene en la app y no hay fuentes descargables ni permiso de red. | S41-R05 | `ls app/src/main/res/font/roboto_flex.ttf`; `grep -rn "google-fonts\|GoogleFont" gradle/libs.versions.toml app/` sin resultados; S01-A03 sobre el manifiesto fusionado |
| S41-A06 | La licencia de la fuente viene en la app. | S41-R06 | `grep -c "SIL OPEN FONT LICENSE Version 1.1\|Copyright 2017 The Roboto Flex Project Authors" app/src/main/assets/licenses/roboto_flex_ofl.txt` devuelve 2 |
| S41-A07 | El ayudante limita los ejes a su rango y `quantizeAxis` redondea a pasos. | S41-R07, S41-R08 | `RobotoFlexTest` en `src/test` |
| S41-A08 | En el teléfono, la fuente carga y sus ejes funcionan: un texto con `wdth` 151 mide más que el mismo texto con `wdth` 25, y uno con `wght` 900 más que con `wght` 100. | S41-R05, S41-R07 | `RobotoFlexRenderingTest` en `src/androidTest`, con `TextMeasurer` |
| S41-A09 | `DesignLab` abre en el teléfono y cambia con sus controles. | S41-R17, S41-R18, S41-R20 | El comando `adb` de "Ciclo visual" y tres capturas, una por dirección, anotadas en la verificación de la Fase 2 |
| S41-A10 | Cada `Content` y cada componente compartido tiene una preview con `@ThemePreviews`. | S41-R16 | `for f in $(find <pkg> -name "*Content.kt" -o -path "*/ui/theme/component/*.kt"); do grep -L "@ThemePreviews" $f; done` sin resultados |
| S41-A11 | Hoy y el registro de series conservan su acción principal visible y tocable con la fuente al 200 %. | S41-R15, D10 | Pruebas de UI con `DeviceConfigurationOverride.FontScale(2f)` de F00 y F02 |
| S41-A12 | Diálogos, avisos, errores y estados vacíos siguen los patrones, y los valores se leen con su unidad. | S41-R13, S41-R14 | Pruebas de UI con aserciones de semántica de cada spec F y las pruebas de `Formats.kt` de F02 |
| S41-A13 | No se agregaron la API Styles, librerías de color, de formas, de iconos ni de fuentes descargables. | S41-R21, D2, D6, D13 | `grep -rnE "ExperimentalFoundationStyleApi\|material-color-utilities\|graphics-shapes\|material-icons\|google-fonts" gradle/libs.versions.toml app/build.gradle.kts app/src` sin resultados |

## Plan de la Fase 2

La Fase 2 implementa el tema. Empieza cuando el dueño o el coordinador lo indiquen, después de la Spec 31. Sigue la prueba primero de la Spec 31 (S31-R22): cada tarea escribe su prueba, la ve fallar y luego escribe el código.

| Tarea | Qué hace | Archivos | Pruebas y evidencia | Estado |
| --- | --- | --- | --- | --- |
| T1. Esquemas de color | `selectColorScheme` pura. Esquemas de respaldo completos, generados con Material Theme Builder desde `#6750A4`. Borrar `Color.kt` de la plantilla y los colores sin uso de `colors.xml` | `ui/theme/ColorSchemes.kt`, `res/values/colors.xml` | `ColorSchemesTest` (S41-A02, S41-A03) | Hecha el 2026-10-11 |
| T2. Fuente | Descargar el TTF de `google/fonts`, anotar su SHA-256 en la verificación, copiarlo como `res/font/roboto_flex.ttf` y copiar `OFL.txt` como `assets/licenses/roboto_flex_ofl.txt`. Escribir `RobotoFlex.kt` | `res/font/roboto_flex.ttf`, `assets/licenses/roboto_flex_ofl.txt`, `ui/theme/RobotoFlex.kt` | `RobotoFlexTest` (S41-A07), `RobotoFlexRenderingTest` (S41-A08), comandos de S41-A05 y S41-A06 | Hecha el 2026-10-11; `RobotoFlexRenderingTest`, pendiente de dispositivo |
| T3. Tipografía | `weightrackTypography(width)` y `NumberStyles`. Comprobar si la fuente trae `tnum` | `ui/theme/Type.kt` | Previews de `ThemeGallery` | Hecha el 2026-10-11. La fuente no trae `tnum` (ver "Verificación de la Fase 2") |
| T4. Formas, espacio y movimiento | `Shape.kt`, `Spacing.kt`, `Motion.kt` con los resortes de la tabla y `rememberReducedMotion()` | `ui/theme/Shape.kt`, `Spacing.kt`, `Motion.kt` | Revisión; las animaciones las prueban las specs F | Hecha el 2026-10-11, con `MotionTest` |
| T5. Tema y direcciones | `WeightrackTheme` con `LocalWeightrackTokens` y el objeto de lectores. `VisualDirection` con A, B y C, y `DefaultDirection = Expressive`. `values-night/themes.xml` | `ui/theme/Theme.kt`, `Directions.kt`, `res/values-night/themes.xml` | `WeightrackThemeTest` (S41-A04) | Hecha el 2026-10-11; `WeightrackThemeTest`, pendiente de dispositivo |
| T6. Previews y laboratorio | `@ThemePreviews`, `ThemeGallery` (colores, escala de texto, números, formas) y `DesignLab` interactivo | `ui/theme/Previews.kt` | S41-A09: comando `adb` y tres capturas en el teléfono | Hecha el 2026-10-11; capturas pendientes de dispositivo |
| T7. Verificación | Correr los comandos de la Spec 31. Comprobar que siguen pasando S30-A01 a S30-A11 y `MainActivitySmokeTest`, y S41-A01, S41-A05, S41-A06 y S41-A13 | — | `./gradlew assembleDebug test` y, con un dispositivo, `./gradlew assembleDebug test connectedDebugAndroidTest` (S31-R23, S31-R24) | Hecha sin dispositivo el 2026-10-11; el comando completo, pendiente de dispositivo |

Notas para la Fase 2:

- **Sin dependencias nuevas.** Todo sale de la BOM 2026.09.00. Si algo exige una dependencia, la Fase 2 se detiene y la propone.
- **Riesgo de las previews.** Las previews de Android Studio quizá no dibujen los ejes de una fuente variable igual que el teléfono. Si pasa, manda el teléfono (`PreviewActivity`) y la Fase 2 lo anota.
- **Riesgo de las pruebas instrumentadas.** `connectedDebugAndroidTest` desinstala la app al terminar (Spec 31, "Riesgos abiertos"). En la Fase 2 todavía no hay datos reales que perder.
- **Captura de evidencia.** Las tres capturas de `DesignLab` se le muestran al dueño para que reaccione.

## Verificación de la Fase 2

Hecha el 2026-10-11 sobre el árbol de trabajo: commit `532cff6` más los cambios de la Fase 2. Cada comando se ejecutó tal como está en "Criterios de aceptación", desde la raíz del proyecto. **No había ningún dispositivo conectado**: el dueño se llevó el teléfono. Los criterios instrumentados quedan "Pendiente de dispositivo" y, por S31-R25, la spec sigue en Aprobada hasta correrlos.

### Criterios

| ID | Resultado | Evidencia |
| --- | --- | --- |
| S41-A01 | Pasa | La búsqueda no da resultados. |
| S41-A02 | Pasa | `ColorSchemesTest`, seis pruebas de `selectColorScheme`: API 31 claro y oscuro con dinámico, API 30 con dinámico, sin dinámico, oscuro sin dinámico, y por debajo de API 31 nunca llama a los proveedores dinámicos. Sin fallos. |
| S41-A03 | Pasa | `ColorSchemesTest.fallbackSchemes_textPairs_meetContrast`: los seis pares, en claro y en oscuro, dan 4,5:1 o más. |
| S41-A04 | Pendiente de dispositivo | `WeightrackThemeTest.dynamicColor_onApi31OrHigher_usesDynamicScheme`, con `@SdkSuppress(minSdkVersion = 31)`, compila con `./gradlew assembleDebugAndroidTest`. La misma clase trae `dynamicColorOff_usesFallbackSchemes` y `direction_providesItsTokensAndShapes`. |
| S41-A05 | Pasa | `ls` muestra `roboto_flex.ttf` con 1 787 292 bytes. La búsqueda de `google-fonts\|GoogleFont` no da resultados. S01-A03: el manifiesto fusionado (`processDebugManifest`) solo declara `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`; no declara `INTERNET`. |
| S41-A06 | Pasa | `grep -c` devuelve 2. |
| S41-A07 | Pasa | `RobotoFlexTest`: 10 pruebas, sin fallos. |
| S41-A08 | Pendiente de dispositivo | `RobotoFlexRenderingTest` compila: `width151_isWiderThanWidth25`, `weight900_isWiderThanWeight100` y `digits_atHeroAxes_haveEqualWidths` (cifras tabulares, ver "Hechos"). |
| S41-A09 | Pendiente de dispositivo | `DesignLabPreview` está en `PreviewsKt` y compila en la build debug. Faltan el comando `adb` de "Ciclo visual" y las tres capturas, una por dirección. |
| S41-A10 a S41-A12 | No aplica todavía | Los comprueba cada spec F. La única preview con `@ThemePreviews` hoy es la de `ThemeGallery`. |
| S41-A13 | Pasa | La búsqueda no da resultados. |

T7 además comprobó:

- **S30-A01 a S30-A11.** S30-A01: solo `26:include(":app")`. S30-A02, A03, A04, A07 y A08: sin resultados. S30-A05: tres líneas, todas de `AppContainer.kt` (21, 27 y 31). S30-A06: solo `AppContainer.kt` y `WeightrackApplication.kt`. S30-A09: `grep -c` devuelve 1 y no hay `androidx.fragment`. S30-A10: solo `versionName = "1.0"`; los bloques `plugins` usan `alias`, con `apply false` en la raíz. S30-A11: la corrida en copia limpia de abajo.
- **`MainActivitySmokeTest`.** Compila; pendiente de dispositivo. `MainActivity` no cambió: ya envolvía su contenido en `WeightrackTheme { }`, y la nueva firma conserva los valores por defecto.

### Comandos

| Comando | Resultado |
| --- | --- |
| `./gradlew assembleDebug test` | `BUILD SUCCESSFUL`. `testDebugUnitTest`: 128 pruebas en 17 clases, sin fallos, errores ni omitidas. Antes de la Fase 2 eran 96 en 11. |
| `./gradlew assembleDebug test` en una copia limpia de los archivos no ignorados, con `ANDROID_HOME` en lugar de `local.properties` | `BUILD SUCCESSFUL`, 50 tareas ejecutadas, 128 pruebas en 17 clases, sin fallos, errores ni omitidas. |
| `./gradlew assembleDebugAndroidTest` | `BUILD SUCCESSFUL`. Las instrumentadas pasan de 89 pruebas en 9 clases a 95 en 11; no se corrieron. |
| `./gradlew :app:lintDebug` | 0 errores y 2 avisos (`AndroidGradlePluginVersion` y `RedundantLabel`), los dos anteriores a la Fase 2. Antes había 9 avisos: los 7 `UnusedResources` eran los colores de la plantilla, que T1 borró. |
| `./gradlew assembleDebug test connectedDebugAndroidTest` | Pendiente de dispositivo. |

Pruebas JVM nuevas, todas en `src/test/java/com/juancholopes/weightrack/ui/theme/`: `ColorSchemesTest` (7), `RobotoFlexTest` (10), `TypeTest` (5), `MotionTest` (5), `DirectionsTest` (3) y `WeightrackTokensTest` (2). Instrumentadas nuevas, en `src/androidTest/.../ui/theme/`: `WeightrackThemeTest` (3) y `RobotoFlexRenderingTest` (3).

Prueba primero (S31-R22). Cada tarea corrió sus pruebas contra funciones vacías (`TODO()`) antes de escribir el código:

- T1: 6 de 7 fallaron con `NotImplementedError`. La de contraste pasó desde el principio, porque el esquema provisional era el de la librería, que usa la misma paleta.
- T2: las 10 fallaron.
- T3 a T5: 12 de 15 fallaron. Las tres que pasaron comprueban datos, no lógica: los resortes copiados de Material 3 y `SpringToken.spec`.

### Hechos de la implementación

- **Archivo de la fuente.** Descargado el 2026-10-11 de `raw.githubusercontent.com/google/fonts/main/ofl/robotoflex/`, con nombre `RobotoFlex[GRAD,XOPQ,XTRA,YOPQ,YTAS,YTDE,YTFI,YTLC,YTUC,opsz,slnt,wdth,wght].ttf`. 1 787 292 bytes, SHA-256 `9b523f7d82593df0107173849ebb8c817471a1df4b4fb2c3cbf40cfd810c8281`, igual en `res/font/roboto_flex.ttf`. Su `METADATA.pb` apunta a la versión 3.200 (commit `739e06dc46ebb14cddd88b9768a6c1504d4677f6`) y confirma los rangos de los ejes. `OFL.txt`: SHA-256 `9cbaed04b20c853f99840efe5dc96956f6f6120ed83a0ade35f9281a2b63e5d0`; su primera línea es "Copyright 2017 The Roboto Flex Project Authors (https://github.com/TypeNetwork/Roboto-Flex)".
- **Sin `tnum`.** La tabla `GSUB` trae `liga`, `locl`, `pnum` y `rvrn`; no trae `tnum`. Las diez cifras tienen el mismo avance (1156 unidades en `hmtx`): en Roboto Flex las cifras ya son tabulares y `pnum` las vuelve proporcionales. Por eso `NumberStyles` no usa `fontFeatureSettings`. `RobotoFlexRenderingTest.digits_atHeroAxes_haveEqualWidths` lo comprobará con los ejes de `hero` (`wdth` 151, `wght` 800).
- **Esquemas de respaldo.** Material Theme Builder es una herramienta web que el agente no pudo usar. Los esquemas son el esquema base de Material 3 para la semilla `#6750A4`: los `PaletteTokens`, `ColorLightTokens` y `ColorDarkTokens` de `material3` 1.4.0, convertidos a código con un script, con los 48 roles de cada esquema. Las versiones nuevas de Material Theme Builder pueden dar valores algo distintos para la misma semilla. Para cambiarlos: exportar desde Material Theme Builder y pegar un bloque por esquema en `ColorSchemes.kt`; `ColorSchemesTest` comprueba el contraste.
- **Texto en negrita del sistema.** En `ui-text` 1.12.1, `AndroidFontResolveInterceptor` suma `fontWeightAdjustment` al peso pedido, y Compose elige la fuente de la familia con el peso más cercano. No reescribe el eje `wght`. Por eso las familias traen las variantes de +300: la de texto, 300, 400, 500, 700, 800 y 900; cada estilo de número, su peso y ese peso más 300 (hasta 1000).
- **`opsz`.** `robotoFlex` y `robotoFlexFamily` aceptan `opticalSize: Float?`. Sin valor, la fuente usa su tamaño óptico por defecto (D5).
- **Movimiento reducido.** `WeightrackTheme` llama a `rememberReducedMotion()`. Con movimiento reducido, `WeightrackTheme.motion` es `forReducedMotion()`: resortes sin rebote y `reducedMotion = true`, que los componentes leen para quitar desplazamientos, escalas, ejes animados y bucles. En API 33 o más también sigue los cambios de la escala de animaciones con la pantalla abierta.
- **API 31.** `dynamicLightColorScheme` y `dynamicDarkColorScheme` se pasan a `selectColorScheme` como lambdas con `@SuppressLint("NewApi")`, porque solo se llaman en API 31 o más (`ColorSchemesTest.selectColorScheme_belowApi31_neverCallsDynamicProviders`). Lint no da errores.
- **Valores elegidos para las direcciones.** La spec da rangos; estos son los valores de partida, todos ajustables (S41-R19):

  | | A. Expressive | B. Scoreboard | C. Notebook |
  | --- | --- | --- | --- |
  | `Spacing` (xxs a xxxl, dp) | 2, 4, 8, 12, 16, 24, 32, 48 | 1, 2, 4, 6, 8, 16, 24, 32 | 4, 8, 12, 16, 20, 32, 40, 56 |
  | `Shapes` (extraSmall a extraLarge, dp) | 8, 12, 16, 24, 32 | 4, 6, 8, 10, 12 | 6, 10, 14, 18, 20 |
  | Texto `wdth` | 100 | 85 | 100 |
  | `hero` (sp, `wdth`, `wght`, `GRAD`) | 64, 151, 800, 0 | 72, 30, 900, 0 | 64, 110, 300, 100 |
  | `card` | 32, 140, 700, 0 | 36, 40, 900, 0 | 32, 110, 300, 50 |
  | `inline` | 16, 100, 600, 0 | 16, 50, 800, 0 | 16, 110, 400, 0 |
  | Tarjetas (`CardStyle`) | `Tonal` | `AccentEdge` | `Outlined` |
  | Movimiento | Resortes expresivos; tweens de 250 y 400 ms con desaceleración enfatizada | Resortes estándar sin rebote; 120 y 200 ms con desaceleración estándar (0; 0; 0; 1) | Resortes estándar sin rebote; 250 y 350 ms con la curva estándar (0,2; 0; 0; 1) |

- **`DesignLab`.** Elige la dirección, claro u oscuro y el color dinámico, y ajusta `wdth`, `wght` y `GRAD` de los números grandes (`hero` y `card`) y el `wdth` del texto. Los sliders redondean con `quantizeAxis` (pasos de 1 en `wdth` y de 10 en `wght` y `GRAD`). Aplica los insets de `safeDrawing`, para verse bien en `PreviewActivity` con la pantalla de borde a borde.
- **Previews sin ver.** El agente no tiene Android Studio: las previews compilan, pero no se vieron dibujadas. Sigue abierto el riesgo de "Notas para la Fase 2": que las previews no dibujen los ejes como el teléfono.

### Pendiente

- Con un teléfono conectado: `./gradlew assembleDebug test connectedDebugAndroidTest` (S31-R24), que cubre S41-A04, S41-A08 y `MainActivitySmokeTest`.
- S41-A09: el comando `adb` de "Ciclo visual" y tres capturas de `DesignLab`, una por dirección, para que el dueño reaccione.
- Ver `ThemeGallery` y `DesignLab` en Android Studio.

## Dependencias

No agrega dependencias de Gradle. Usa lo que ya está:

| Pieza | Versión | Uso |
| --- | --- | --- |
| `androidx.compose.material3:material3` | 1.4.0, por la BOM 2026.09.00 | Componentes, `Slider`, `dynamicLightColorScheme`, `dynamicDarkColorScheme` |
| `androidx.compose.ui:ui-text` | 1.12.1, por la BOM | `Font` con `variationSettings`, `FontVariation` |
| `androidx.compose.ui:ui-tooling-preview` y `ui-tooling` | 1.12.1, por la BOM | Previews, `Wallpapers` y `PreviewActivity` en debug |
| `androidx.compose.ui:ui-test` | 1.12.1, llega con `ui-test-junit4` | `DeviceConfigurationOverride.FontScale` en D10 |

Recursos que se incluyen, sin ser dependencias de Gradle:

| Recurso | Versión | Licencia | Fuente |
| --- | --- | --- | --- |
| Roboto Flex, fuente variable | 3.200 (30 de junio de 2023), 1 787 292 bytes | SIL OFL 1.1 | `google/fonts`, `ofl/robotoflex` |
| Material Symbols, solo los iconos usados | Los de fonts.google.com/icons en la fecha de cada spec F | Apache 2.0 | `google/material-design-icons` |

No se agregan:

- `material3` 1.5.0-beta01 ni ninguna versión fuera de la BOM (D8).
- `ui-text-google-fonts` ni fuentes descargables (S41-R05).
- `material-color-utilities` (D2).
- `androidx.graphics:graphics-shapes`: las formas de galleta, si hacen falta, se dibujan con un `Path` (Spec 42).
- `material-icons-core` ni `material-icons-extended` (D13).
- Compose Preview Screenshot Testing, Roborazzi ni Dropshots (D9).

## Cambios que esta spec pide a otras specs

Esta spec no edita ninguna. El dueño aprueba y aplica cada cambio.

| Spec | Cambio propuesto | Motivo | Estado |
| --- | --- | --- | --- |
| 30 Arquitectura | En el árbol de ADR-30-03, anotar `ui/theme/component/` (componentes compartidos sin estado, Spec 41) y que `ui/theme/` incluye `Formats.kt`. | D12: los componentes compartidos viven fuera de `feature/`. | Pendiente |
| 31 Estrategia de pruebas | Marcar resuelta su fila "41": sin capturas automáticas en la primera versión, con el ciclo de previews, `DesignLab` y capturas a mano (D9), y pruebas con la fuente al 200 % en Hoy y en el registro de series (D10). | Cerrar la costura que dejó la Spec 31. | Pendiente |
| 03 Configuración | En el esqueleto, anotar `res/font/`, `res/values-night/` y `assets/licenses/`. | Recursos nuevos de la Fase 2. | Pendiente |
| 40 Flujos de usuario | Marcar resuelta su fila "41 Sistema de diseño": patrones, duración de los avisos y formatos en esta spec; la revisión de los textos, en la Spec 42 con el tono de esta spec. | Cerrar el pedido de la Spec 40. | Pendiente |
| F00 a F05 | Usar `WeightrackTheme`, los tokens y los patrones; construir los componentes compartidos de la tabla; dar a cada `Content` su preview con `@ThemePreviews`. F02 agrega `Formats.kt`. F00 y F02 escriben las pruebas de D10. | Aplicar el sistema de diseño en cada corte vertical. | Pendiente |

## Fuentes

Consultadas el 2026-10-11:

- Spec 00 Constitución (Aprobada): Material 3, sin conexión, accesibilidad, textos en `strings.xml`, reglas 3 y 9.
- Spec 01 Visión y alcance (Aprobada): solo teléfono, versiones.
- Spec 02 Glosario (Aprobada): términos y "Máquina" como valor del equipo.
- Spec 30 Arquitectura (Aprobada): ADR-30-03 (`ui/theme/`), ADR-30-07 (`Content` sin estado y previews), "Costuras" de las Specs 41 y 42.
- Spec 31 Estrategia de pruebas (Aprobada): D1 (sin Robolectric), D2 (capturas), pruebas de UI en el dispositivo, fila "41".
- Spec 40 Flujos de usuario (Aprobada): fila "41" de sus cambios, avisos, estados vacíos, accesibilidad y textos.
- Código del commit `f229bb9`: `ui/theme/*`, `MainActivity.kt`, `themes.xml`, `colors.xml`, `libs.versions.toml`, `app/build.gradle.kts`.
- Código fuente en la caché de Gradle: `material3-android` 1.4.0 (`Slider.kt`, `SliderTokens.kt`, `MotionScheme.kt`, `MaterialTheme.kt`, `ExpressiveMotionTokens.kt`, `StandardMotionTokens.kt`, `SnackbarHost.kt`, `SegmentedButton.kt`, `DatePicker.kt` y su archivo `.module`), `ui-text-android` 1.12.1 (`Font.kt`, `FontVariation.kt`, `PlatformFontVariationSettings.android.kt`, `FontFamilyResolver.kt`), `ui-tooling-preview-android` 1.12.1 (`MultiPreviews.kt`, `PreviewWrapper.kt`), `ui-tooling` 1.12.1 (manifiesto con `PreviewActivity`), `foundation-android` 1.12.1 (`style/Style.kt`) y la BOM 2026.09.00.
- [Google Maven, `material3`](https://dl.google.com/android/maven2/androidx/compose/material3/material3/maven-metadata.xml): 1.4.0 estable; 1.5.0-beta01 del 7 de octubre de 2026.
- [Compose Material 3 releases](https://developer.android.com/jetpack/androidx/releases/compose-material3): 1.5.0-beta01 y cambios de `Slider` en 1.5.0-alpha28 y alpha29.
- [Google Maven, AGP](https://dl.google.com/android/maven2/com/android/tools/build/gradle/maven-metadata.xml): 9.5.0-alpha09 como la más nueva de la línea 9.5.
- [`google/fonts`, `ofl/robotoflex`](https://github.com/google/fonts/tree/main/ofl/robotoflex): `METADATA.pb` (ejes, licencia, versión 3.200), `OFL.txt`, tamaño del archivo.
- [Roboto Flex releases](https://github.com/googlefonts/roboto-flex/releases): 3.200 del 30 de junio de 2023.
- [Material Theme Builder](https://material-foundation.github.io/material-theme-builder/).
- [`google/material-design-icons`](https://github.com/google/material-design-icons): licencia Apache 2.0.

## Skills consultadas

Por la regla 9, cada choque con una spec se registra como decisión o como cambio. Ninguna skill se aplicó en silencio.

| Skill | Qué dice | Efecto en esta spec | ¿Choque? |
| --- | --- | --- | --- |
| `styles` (Android, 2026-09-08) | Integrar la API Styles; es experimental, solo para componentes y temas propios, no para componentes Material | D6: no se usa por ahora | Sí, leve: la skill empuja a migrar. Resuelto en D6 |
| `edge-to-edge` (Android, 2026-08-24) | `enableEdgeToEdge()` antes de `setContent`; insets con un solo método; `Scaffold` por pantalla; no tocar los colores de iconos con `enableEdgeToEdge` de `ComponentActivity` | S41-R11, "Color dinámico con respaldo" | No |
| `adaptive` (Android, 2026-09-22) | Previews y capturas por tamaño de dispositivo; layouts adaptables | No se aplica: solo teléfono (Spec 01) | No; se difiere |
| `design-system` (diseño) | Tokens (color, tipografía, espacio, bordes, sombras, movimiento), componentes con variantes y estados, "documentarlo todo", consistencia antes que creatividad | "Archivos y tokens", "Componentes compartidos" | Sí, leve: el dueño quiere iterar, no documentar cada variante. Esta spec documenta la mecánica y la lista de componentes; la documentación de cada componente llega cuando se estabilice (S41-R19) |
| `accessibility-review` (diseño) | WCAG 2.1 AA: contraste 4,5:1, contraste de componentes 3:1, objetivos de 44 px, foco visible, errores descritos, sin límites de tiempo | S41-R15, S41-A03, patrones | No. La constitución pide 48 dp, más que 44 px |
| `ux-copy` (diseño) | Acciones con verbo, errores con qué pasó y cómo corregir, diálogos con la acción en título y botones | "Tono de los textos"; la revisión completa va en la Spec 42 | No |
| `genjutsu:paint` | Pipeline de arte: lluvia de ideas pregunta a pregunta, tesis visual y de interacción validadas antes de implementar, `MASTER.md` en la raíz, reemplazar los tokens existentes | "Direcciones visuales": tres tesis completas para reaccionar | Sí. (1) No hubo preguntas una a una: el dueño delegó y dio referencias. (2) No se valida una sola tesis: quedan tres presets sin congelar (D11). (3) No se crea `MASTER.md`: esta spec cumple ese papel, porque las specs son la documentación del proyecto (constitución) |
| `genjutsu` `_jutsu/compose-motion` | API de animación de Compose, resortes por uso, movimiento reducido con `ValueAnimator.areAnimatorsEnabled()` | "Movimiento", `rememberReducedMotion()` | No |
| `maestro` | Modo B para un sistema de diseño desde cero; suelo de accesibilidad sin excepciones; en nativo, la dirección la da `_jutsu/motion-principles` y no `motion-design`; rebote según la personalidad | Modo B; "Movimiento reducido"; rebote acotado por dirección | Sí, leve: el coordinador pidió usar `motion-design`. Se usan las dos: `motion-design` para duraciones y personalidad; `compose-motion` para la API de Compose |
| `motion-design` | Personalidades (Corporativa por defecto para UI), tablas de duración, entrada más larga que salida, mínimo de propiedades | Tesis de interacción de cada dirección; micro-interacciones de Roboto Flex | Sí, leve: su valor por defecto es "Corporativa", y las referencias del dueño son expresivas. Resuelto en D11: la dirección A usa resortes expresivos con rebote acotado |
| `testing-setup` (por la Spec 31) | Capturas con Compose Preview Screenshot Testing; `DeviceConfigurationOverride` para la escala de fuente | D9 y D10 | Sí con las capturas: resuelto en D9. No con la escala de fuente (D10) |
