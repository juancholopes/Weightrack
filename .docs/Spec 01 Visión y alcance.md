# Spec 01: Visión y alcance

Oct 10, 2026 · @Juan

| ID | Estado | Fase | Depende de |
| --- | --- | --- | --- |
| 01 | Aprobada | 0 Fundamentos | 00 |

## Propósito

Esta spec fija para quién es Weightrack, qué problema resuelve y qué entra en la primera versión. Las specs de dominio, flujos y features se apoyan en ella para no construir de más ni de menos.

No repite lo que ya dice la constitución: stack, principios y reglas de trabajo se leen allí.

## Visión

En el gimnasio, saber con qué peso trabajar en cada máquina sin tener que recordarlo, y ver con datos que se está progresando.

A largo plazo, Weightrack es el registro personal del entrenamiento: lo que se levanta, cómo cambia el cuerpo y cómo se planifica cada sesión, todo en un mismo lugar y en el dispositivo.

## Misión

Sacar una primera versión pequeña y útil con las features ya definidas (F00 a F04) sobre una arquitectura que permita agregar features nuevas sin rehacer lo que ya funciona.

La primera versión se mide por dos cosas: que se use de verdad en el gimnasio y que la siguiente feature se pueda sumar como un corte vertical nuevo, sin tocar los existentes.

## Problema

Quien entrena varias veces por semana usa muchos ejercicios y máquinas, cada una con su peso. Ese peso se olvida de una sesión a otra, se anota en papel o en notas sueltas que no permiten comparar, y se pierde la noción de si se está avanzando.

## Usuario

Una sola persona: el dueño del proyecto, que entrena en el gimnasio y registra desde su teléfono Android. No hay otros perfiles, roles ni cuentas.

El uso típico ocurre entre series, con el teléfono en la mano y poco tiempo, así que registrar debe tomar segundos.

## Objetivos de la primera versión

| ID | Requisito |
| --- | --- |
| S01-R01 | El usuario mantiene su propio catálogo de ejercicios o máquinas. |
| S01-R02 | El usuario registra una sesión de entrenamiento con las series de cada ejercicio: peso y repeticiones. |
| S01-R03 | Al registrar un ejercicio, el usuario ve qué peso y repeticiones usó la última vez. |
| S01-R04 | El usuario consulta y gestiona el historial de sus sesiones anteriores. |
| S01-R05 | Todo funciona sin conexión y los datos viven solo en el dispositivo. |
| S01-R10 | El usuario puede tomar notas de texto libre de dos tipos: una nota fija por ejercicio, por ejemplo la posición del asiento, y una nota por sesión, por ejemplo cómo se sintió el entrenamiento. |
| S01-R12 | El usuario ve cómo evoluciona cada ejercicio en el tiempo, con una gráfica de sus sesiones y sus récords. |

S01-R05 se refiere a las funciones de la app: Weightrack no envía datos a ningún servidor propio ni de terceros. El respaldo automático del sistema Android (Auto Backup), que copia los datos a la cuenta de Google del usuario, se acepta como excepción (decidido el 2026-10-10).

## Alcance

Entra en la primera versión, con la feature que lo implementa:

| Feature | Qué cubre | Requisito |
| --- | --- | --- |
| F00 Esqueleto funcional | App que arranca, navega y persiste, sin funcionalidad de negocio | Base para todos |
| F01 Catálogo de ejercicios | Crear, editar, archivar y eliminar ejercicios | S01-R01 |
| F02 Registrar sesión y series | Iniciar una sesión y anotar series de peso y repeticiones | S01-R02 |
| F03 Última vez por ejercicio | Mostrar todas las series del ejercicio en la sesión más reciente que lo contiene | S01-R03 |
| F04 Historial de sesiones | Listar, ver, corregir y eliminar sesiones pasadas | S01-R04 |
| F01 Catálogo de ejercicios | Escribir, ver y editar la nota fija de un ejercicio | S01-R10 |
| F02 Registrar sesión y series | Escribir, ver y editar la nota de una sesión | S01-R10 |
| F05 Estadísticas por ejercicio | Gráfica de la evolución del peso de cada ejercicio y sus récords | S01-R12 |

La Spec 10 fijó el 2026-10-10 dónde vive cada nota: `Exercise.notes` y `WorkoutSession.notes`. No hay nota del ejercicio dentro de una sesión ni nota por serie.

## Versiones

La primera versión se entrega por etapas. Cada etapa es una etiqueta de git `v<versión>` sobre un APK que se instala en el teléfono. Decidido el 2026-10-11 por el dueño: el MVP es la 0.0.1 y la primera versión completa es la 0.1.0.

| Versión | Qué trae | Se etiqueta cuando |
| --- | --- | --- |
| 0.0.1 (MVP) | F00, F01, F02 y F03: crear ejercicios, registrar una sesión con sus series y ver la última vez de cada ejercicio | En modo avión, el usuario crea un ejercicio, registra una sesión con dos series y, en una sesión de otro día, ve la última vez. El build y las pruebas pasan desde cero |
| 0.0.2 | F05 Estadísticas por ejercicio | El build y las pruebas pasan, y la versión se instala encima de la anterior sin perder datos (S01-R08) |
| 0.0.3 a 0.0.x | F04 Historial de sesiones y las correcciones que salgan del uso real en el gimnasio | El build y las pruebas pasan, y la versión se instala encima de la anterior sin perder datos (S01-R08) |
| 0.1.0 | La primera versión completa: F00 a F05 | Todos los criterios S01-A01 a S01-A09 tienen evidencia, incluido S01-A04 en el teléfono |

F03 entra en el MVP porque saber qué peso se usó la última vez es el motivo para abrir la app en el gimnasio. F05 entra justo después porque el dueño quiere ver la evolución de cada ejercicio (decidido el 2026-10-11). F04 queda después porque el historial completo no hace falta para registrar y comparar.

| ID | Requisito |
| --- | --- |
| S01-R11 | Cada versión etiquetada tiene `versionName` igual a su etiqueta sin la `v`, y un `versionCode` mayor que el de la versión anterior. |

## Visión a futuro

Features que forman parte de la visión pero no de la primera versión. No tienen spec ni fecha. Están aquí para que la arquitectura y los datos de la primera versión no las bloqueen.

| Feature futura | Qué aportaría | Qué necesita de la primera versión |
| --- | --- | --- |
| Mediciones corporales | Registrar peso corporal y medidas para seguir su evolución | Que el registro de entrenamiento no asuma que es el único tipo de dato que existe |
| Rutinas | Definir planes de ejercicios y usarlos al iniciar una sesión | Que una sesión sea independiente de cómo se armó, para poder crearla después desde una rutina |

Ninguna se implementa hasta tener su propia spec aprobada. Mientras tanto, ningún agente agrega código, tablas ni pantallas para ellas.

## Fuera de alcance

No se construye en esta versión ni forma parte de la visión actual. Agregar cualquiera exige una spec nueva y actualizar esta.

- Cuentas, inicio de sesión, servidor o sincronización en la nube.
- Copias de seguridad, exportación o importación de datos como funciones de la app. El respaldo automático del sistema Android sí se permite (ver S01-R05).
- Temporizador de descanso.
- Nutrición.
- Funciones sociales o compartir.
- Tablets, relojes u otros dispositivos distintos del teléfono.

## Lineamientos para la arquitectura de datos

Esta spec no diseña el modelo ni las tablas: eso lo hacen la Spec 10 (modelo de dominio) y la Spec 20 (persistencia). Sí fija lo que esas specs deben garantizar para que la visión sea alcanzable.

| ID | Requisito |
| --- | --- |
| S01-R06 | Cada serie se guarda como un registro propio con su ejercicio, peso, repeticiones y la fecha de su sesión. Nada se guarda solo como resumen o promedio. |
| S01-R07 | Los datos históricos no se pierden al editar el catálogo: renombrar un ejercicio conserva su historial, y eliminar uno con historial requiere una regla explícita en la Spec 10. |
| S01-R08 | El esquema evoluciona solo con migraciones de Room versionadas. Una actualización de la app nunca borra los datos del usuario. |
| S01-R09 | Cada feature es dueña de sus propios datos. Una feature futura agrega sus tablas y relaciones sin modificar las de otra, salvo por un contrato explícito. |

## Criterios de aceptación

| ID | Criterio | Comprueba | Evidencia |
| --- | --- | --- | --- |
| S01-A01 | Cada requisito S01-R01 a S01-R04, S01-R10 y S01-R12 tiene al menos una feature asignada en la tabla de alcance. | S01-R01 a R04, S01-R10, S01-R12 | Revisión de esta spec |
| S01-A02 | Ninguna spec de features (F00 a F04) incluye algo de la lista de fuera de alcance. | Alcance | Revisión de cada spec F |
| S01-A03 | El manifiesto de la app no declara el permiso `INTERNET`. | S01-R05 | `AndroidManifest.xml` fusionado |
| S01-A04 | Con F01 a F04 implementadas, el usuario completa en modo avión: crear un ejercicio, registrar una sesión con dos series, ver la última vez y consultar la sesión en el historial. | S01-R01 a R05 | Captura o video en el teléfono |
| S01-A05 | Las Specs 10 y 20 muestran cómo cumplen S01-R06 a S01-R09, cada una con su ID. | S01-R06 a R09 | Revisión de las Specs 10 y 20 |
| S01-A06 | Tras renombrar un ejercicio con sesiones registradas, su historial y su última vez siguen visibles. | S01-R07 | Prueba de la lógica de F01 y F04 |
| S01-A07 | Instalar una versión con cambio de esquema sobre una anterior conserva todas las sesiones. | S01-R08 | Prueba de migración de Room |
| S01-A08 | Ninguna spec de F00 a F04 agrega código, tablas ni pantallas de las features de la visión a futuro. | Visión a futuro | Revisión de cada spec F |
| S01-A09 | Cada etiqueta `v<versión>` apunta a un commit cuyo `versionName` es esa versión y cuyo `versionCode` supera al de la etiqueta anterior. | S01-R11 | `git show v<versión>:app/build.gradle.kts` |

## Decisiones abiertas

- [x] **Unidad de peso.** Decidido el 2026-10-10: la primera versión usa solo kilogramos, sin selector de unidad, con precisión de 0,25 kg. El peso se guarda siempre en unidades métricas (gramos enteros, ver la Spec 10), así que elegir la unidad puede llegar después como una conversión al mostrar, sin migrar datos. Esa opción queda como candidata de la visión a futuro.
- [x] **Notas.** Decidido el 2026-10-10: el usuario puede tomar notas (S01-R10), no por serie. Una serie guarda solo peso y repeticiones. El término se define en la Spec 02.
- [x] **Alcance de la nota.** Decidido el 2026-10-10 en la Spec 10: una nota fija del ejercicio (F01) y una nota de la sesión (F02). No hay nota del ejercicio dentro de una sesión.
- [x] **Alcance de "Última vez".** Decidido el 2026-10-10: todas las series del ejercicio en la sesión más reciente que lo contiene, ordenadas como se registraron. Detalle en la Spec 10.
- [x] **Respaldo automático de Android.** Decidido el 2026-10-10: `android:allowBackup="true"` se mantiene. S01-R05 y la exclusión de copias de seguridad se refieren a funciones de la app, no al respaldo del sistema.
- [x] **Firma de los APK.** Decidido el 2026-10-11: A. Desde la 0.0.1, el APK de release se firma con una clave propia (`weightrack`, PKCS12, RSA 4096, 10 000 días), guardada fuera del repositorio en `~/keystores/weightrack-release.jks`. Las credenciales viven en `~/.gradle/gradle.properties` (`WEIGHTRACK_RELEASE_*`) y nunca se suben a git. Una actualización solo se instala encima de la anterior si la firma no cambia; cambiar de clave obligaría a desinstalar y perder los datos (S01-R08). Si la clave se pierde, no hay forma de publicar actualizaciones encima, así que el dueño guarda una copia de respaldo del archivo y de sus credenciales. Huella SHA-256 del certificado: `4D:2E:1F:C8:C5:9E:A7:0A:1A:15:CD:C1:59:90:2D:66:17:BD:6C:CF:A0:3D:6F:AC:3D:05:99:B3:F0:18:EE:AF`.
- [x] **Gráficas de progreso.** Decidido el 2026-10-11: el dueño las pasa de la visión a futuro a la primera versión, como F05 Estadísticas por ejercicio en la 0.0.2 (S01-R12). Cubren el "ver cómo evoluciona cada ejercicio" de la constitución. Se apoyan en la consulta de evolución de la Spec 10 (S10-R27). La referencia visual es una gráfica semanal de barras redondeadas con récords marcados, como las de Material 3 Expressive.
