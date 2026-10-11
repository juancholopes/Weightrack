# Spec 00: Constitución de Weightrack

Oct 9, 2026 · @Juan

| ID | Estado | Fase | Depende de |
| --- | --- | --- | --- |
| 00 | Borrador | 0 Fundamentos | Ninguna |

## Propósito y uso

Esta constitución es el documento base de Weightrack. Todo agente la lee primero y ninguna otra spec puede contradecirla.

Reúne lo que no cambia de una funcionalidad a otra: el producto, las decisiones técnicas, los principios, las reglas de trabajo y la definición de hecho. Las demás specs se apoyan en ella y solo agregan lo propio de su tema.

Cómo la usa un agente:

1. La lee completa antes de cada tarea, junto con la spec que le toca.
2. Si una spec la contradice, gana la constitución y el agente avisa del conflicto.
3. Ante un vacío, no improvisa: pregunta.

## El producto

Weightrack es una app nativa para Android que registra el peso usado en cada ejercicio y máquina del gimnasio y guarda su historial para medir el progreso.

El usuario debe poder gestionar ese historial y ver cómo evoluciona cada ejercicio que ejecuta. La versión inicial es de uso personal, con datos locales en el dispositivo, sin cuentas ni servidor.

Todo el desarrollo es guiado por specs y lo implementan agentes. Por eso cada spec debe ser autosuficiente, verificable y trazable por IDs, y el proyecto debe poder crecer funcionalidad por funcionalidad sin romper lo que ya funciona.

## Decisiones técnicas fijas

El stack es nativo de Android: Kotlin, Jetpack Compose, Material 3 y Room 3 para datos locales. Cambiar cualquiera de estas decisiones exige modificar esta constitución.

| Componente | Decisión | Estado |
| --- | --- | --- |
| Lenguaje | Kotlin, última versión estable (línea 2.4.x, la 2.4.0 salió en junio de 2026) | Decidido |
| UI | Jetpack Compose con la BOM estable más reciente (línea 1.12 en agosto de 2026) | Decidido |
| Diseño | Material 3 (`androidx.compose.material3`) y los componentes que trae | Decidido |
| Persistencia | Room 3 (`androidx.room3`) con KSP, base de datos local en el dispositivo | Decidido |
| Asincronía | Coroutines y Flow | Decidido el 2026-10-10 |
| Navegación | Navigation para Compose | Propuesto |
| Build | Gradle con Kotlin DSL y version catalog | Propuesto |
| Inyección de dependencias | Manual con un `AppContainer` creado en una `Application` propia | Decidido el 2026-10-10 |
| Idioma | Identificadores de código en inglés y textos de la interfaz en español | Decidido el 2026-10-10 |
| Calidad de código | Solo el lint integrado de Android, sin ktlint ni detekt por ahora. Agregar una herramienta exige un ADR | Decidido el 2026-10-10 |

Las versiones se fijan en un solo archivo y el agente confirma la última estable antes de escribirlas. Room 3 usa un paquete nuevo: el agente usa `androidx.room3` y nunca `androidx.room` (línea 2.x).

Versiones verificadas por búsqueda el 2026-10-09 en las páginas de [Room 3](https://developer.android.com/jetpack/androidx/releases/room3) y de [Compose](https://developer.android.com/jetpack/androidx/releases/compose) de Android Developers.

## Principios de arquitectura

El detalle se decide en la Spec 30 con ADRs. Estos principios los respeta toda spec para que la app escale sin romperse.

1. **Tres capas.** UI (Compose y ViewModel), dominio (modelos, reglas y casos de uso cuando haya lógica compartida) y datos (Room y repositorios).
2. **Flujo unidireccional.** La UI emite eventos, el ViewModel produce un estado único e inmutable y la UI solo lo dibuja.
3. **Una sola fuente de verdad.** La base de datos local. La UI nunca lee datos de otro lugar.
4. **Dependencias hacia adentro.** El dominio no conoce Android, Room ni Compose. Los repositorios se declaran como interfaces en el dominio.
5. **Cortes verticales por feature.** Cada funcionalidad agrega su propia UI, estado y casos de uso sin modificar lo existente, salvo por contratos explícitos.
6. **Sin sobreingeniería.** La modularización y las abstracciones nuevas se agregan cuando una necesidad concreta lo exige y quedan registradas en un ADR.

## Reglas para agentes

Un agente implementa solo lo que está escrito en la spec vigente.

**Antes de actuar**

1. Leer esta constitución, la spec de la tarea y las specs de su campo "Depende de".
2. No implementar una spec que esté en estado Borrador.

**Mientras trabaja**

3. No agregar librerías, módulos ni patrones que ninguna spec mencione. Si hace falta uno, proponer un ADR.
4. Trabajar una tarea a la vez, en cambios pequeños, sin romper lo que ya funciona.
5. No editar specs ni esta constitución por cuenta propia. Propone el cambio y espera la aprobación.

**Cuándo detenerse y preguntar**

- La spec es ambigua o choca con el código o con esta constitución.
- La tarea exige tocar algo fuera de su alcance.
- Un criterio de aceptación no se puede verificar tal como está escrito.

**Al terminar**

6. Entregar evidencia de cada criterio de aceptación: un comando, una prueba o una captura.
7. Anotar cada fallo propio en el registro de fallos del agente.
8. Hacer commits solo cuando el dueño lo pida, con Conventional Commits (`feat:`, `fix:`, `docs:`, etc.) y sin ninguna atribución a una IA: nada de `Co-Authored-By` ni menciones a un agente o modelo.
9. Usar las skills de Android instaladas en `.claude/skills/` como referencia de buenas prácticas y leer las que apliquen antes de trabajar. Si una skill choca con una spec, el agente no la ignora ni la aplica por su cuenta: la registra como propuesta de cambio a la spec, con su motivo, y el dueño decide.

## Convenciones de código

Estas convenciones valen para todo el código del proyecto. Las specs específicas pueden agregar otras, nunca quitar estas.

- Cada pantalla se compone de un composable de pantalla, un ViewModel y un estado de UI inmutable.
- Los composables no contienen lógica de negocio ni acceden a Room.
- Los modelos de dominio no dependen de Android ni de Room.
- Las versiones de librerías viven solo en `gradle/libs.versions.toml`.
- Cada funcionalidad nueva vive en su propio paquete dentro de `feature/`.
- Los textos visibles para el usuario van en recursos de strings, no escritos dentro del código.
- Los identificadores de código (clases, funciones, propiedades y paquetes) se escriben en inglés. Los textos de la interfaz se escriben en español en `strings.xml`.

## Ciclo de trabajo por spec

Cada funcionalidad recorre el mismo ciclo, de lo general a lo específico. Si en cualquier paso se descubre que una spec base está mal, se corrige la spec antes que el código.

1. **Spec.** Qué se construye y con qué criterios de aceptación.
2. **Plan.** Cómo se construye: capas afectadas, decisiones y riesgos.
3. **Tareas.** Pasos pequeños y verificables, cada uno ligado a un criterio.
4. **Implementación.** El agente ejecuta las tareas una a una.
5. **Verificación.** Se comprueba cada criterio con su evidencia.
6. **Registro de fallos.** Se anota qué falló el agente y qué ajuste de la spec lo evita.

Estados de una spec: Borrador, Aprobada, Implementada y Obsoleta. Solo se implementa una spec Aprobada.

Trazabilidad: los requisitos se numeran `S<spec>-R<nn>` y los criterios de aceptación `S<spec>-A<nn>`. Cada criterio apunta al requisito que comprueba y a la evidencia que lo demuestra.

## Definición de hecho y calidad

Una spec está hecha cuando cumple las cuatro condiciones:

1. Todos sus criterios de aceptación tienen evidencia.
2. El build y las pruebas pasan desde cero con un comando.
3. Las specs afectadas están actualizadas con lo que realmente se construyó.
4. No se agregó ninguna dependencia ni patrón fuera de las specs.

Requisitos de calidad que valen para toda la app desde el primer día:

- **Funciona sin conexión.** Todos los datos viven en el dispositivo y la app no declara permiso de red.
- **Accesibilidad.** Se usan los componentes de Material 3 sin recortar su comportamiento accesible, con áreas táctiles de al menos 48 dp.
- **Pruebas.** Cada funcionalidad trae pruebas de su lógica. La estrategia completa se define en la Spec 31.
- **Compatibilidad.** La app arranca sin cierres en la versión mínima y en la última versión estable de Android.

## Mapa de specs

El roadmap va de lo general a lo específico y cada spec declara de cuáles depende. Las dependencias son una propuesta inicial que se ajusta sin rehacer lo anterior.

| ID | Spec | Fase | Depende de |
| --- | --- | --- | --- |
| 00 | Constitución (esta spec) | 0 Fundamentos | Ninguna |
| 01 | Visión y alcance | 0 Fundamentos | 00 |
| 02 | Glosario | 0 Fundamentos | 00 |
| 03 | Configuración inicial del proyecto Android | 0 Fundamentos | 00 |
| 10 | Modelo de dominio | 1 Dominio y datos | 01, 02 |
| 20 | Persistencia con Room 3 | 1 Dominio y datos | 03, 10 |
| 30 | Arquitectura con ADRs | 2 Arquitectura | 00, 10 |
| 31 | Estrategia de pruebas | 2 Arquitectura | 30 |
| 40 | Flujos de usuario | 3 UX y UI | 01, 10 |
| 41 | Sistema de diseño Material 3 | 3 UX y UI | 00, 40 |
| 42 | Pantallas y navegación | 3 UX y UI | 40, 41 |
| F00 | Esqueleto funcional | 4 Features | 20, 30, 42 |
| F01 | Catálogo de ejercicios | 4 Features | F00 |
| F02 | Registrar sesión y series | 4 Features | F01 |
| F03 | Última vez por ejercicio | 4 Features | F02 |
| F04 | Historial de sesiones | 4 Features | F02 |

## Cambios a esta constitución y decisiones abiertas

Solo el dueño del proyecto aprueba cambios a esta constitución. Un agente propone el cambio con su motivo y espera. Cada enmienda aprobada se anota con fecha y se revisan las specs que dependían de lo cambiado.

Enmiendas aprobadas:

- **2026-10-10.** El dueño cierra dos decisiones abiertas: el idioma (código en inglés, interfaz en español) y la calidad de código (solo el lint integrado de Android). Se agregan a la tabla de decisiones técnicas y a las convenciones de código. Specs revisadas: 02, 03 y 10.
- **2026-10-10.** El dueño agrega la regla 8 para agentes: commits con Conventional Commits y sin atribución a una IA.
- **2026-10-10.** Al aprobar la Spec 20, el dueño cierra la inyección de dependencias (manual, con un `AppContainer` creado en una `Application` propia) y pasa "Asincronía: Coroutines y Flow" de Propuesto a Decidido. Specs revisadas: 03, 10 y 20.
- **2026-10-11.** El dueño agrega la regla 9 para agentes: las skills de Android instaladas son la referencia de buenas prácticas, y sus choques con una spec se proponen como cambio a la spec.

Decisiones:

- [x] **Inyección de dependencias.** Decidido el 2026-10-10 en la Spec 20: manual, con un `AppContainer` creado en una `Application` propia. La Spec 30 lo registra en su ADR.
- [x] **Idioma del código y de la interfaz.** Decidido el 2026-10-10: identificadores de código en inglés y textos de la app en español, en `strings.xml`.
- [x] **Calidad de código.** Decidido el 2026-10-10: ninguna herramienta adicional por ahora, solo el lint integrado de Android. ktlint, detekt u otra se pueden agregar después con un ADR.
- [ ] **Registro de fallos del agente.** Dónde vive y con qué formato.
