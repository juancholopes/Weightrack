# Spec 01: Visión y alcance

Oct 10, 2026 · @Juan

| ID | Estado | Fase | Depende de |
| --- | --- | --- | --- |
| 01 | Borrador | 0 Fundamentos | 00 |

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

La Spec 10 fijó el 2026-10-10 dónde vive cada nota: `Exercise.notes` y `WorkoutSession.notes`. No hay nota del ejercicio dentro de una sesión ni nota por serie.

## Visión a futuro

Features que forman parte de la visión pero no de la primera versión. No tienen spec ni fecha. Están aquí para que la arquitectura y los datos de la primera versión no las bloqueen.

| Feature futura | Qué aportaría | Qué necesita de la primera versión |
| --- | --- | --- |
| Gráficas de progreso | Ver la evolución del peso y las repeticiones de cada ejercicio en el tiempo | Que cada serie quede guardada con su fecha y su ejercicio, sin resumir ni sobrescribir |
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
| S01-A01 | Cada requisito S01-R01 a S01-R04 y S01-R10 tiene al menos una feature asignada en la tabla de alcance. | S01-R01 a R04, S01-R10 | Revisión de esta spec |
| S01-A02 | Ninguna spec de features (F00 a F04) incluye algo de la lista de fuera de alcance. | Alcance | Revisión de cada spec F |
| S01-A03 | El manifiesto de la app no declara el permiso `INTERNET`. | S01-R05 | `AndroidManifest.xml` fusionado |
| S01-A04 | Con F01 a F04 implementadas, el usuario completa en modo avión: crear un ejercicio, registrar una sesión con dos series, ver la última vez y consultar la sesión en el historial. | S01-R01 a R05 | Captura o video en el teléfono |
| S01-A05 | Las Specs 10 y 20 muestran cómo cumplen S01-R06 a S01-R09, cada una con su ID. | S01-R06 a R09 | Revisión de las Specs 10 y 20 |
| S01-A06 | Tras renombrar un ejercicio con sesiones registradas, su historial y su última vez siguen visibles. | S01-R07 | Prueba de la lógica de F01 y F04 |
| S01-A07 | Instalar una versión con cambio de esquema sobre una anterior conserva todas las sesiones. | S01-R08 | Prueba de migración de Room |
| S01-A08 | Ninguna spec de F00 a F04 agrega código, tablas ni pantallas de las features de la visión a futuro. | Visión a futuro | Revisión de cada spec F |

## Decisiones abiertas

- [x] **Unidad de peso.** Decidido el 2026-10-10: la primera versión usa solo kilogramos, sin selector de unidad, con precisión de 0,25 kg. El peso se guarda siempre en unidades métricas (gramos enteros, ver la Spec 10), así que elegir la unidad puede llegar después como una conversión al mostrar, sin migrar datos. Esa opción queda como candidata de la visión a futuro.
- [x] **Notas.** Decidido el 2026-10-10: el usuario puede tomar notas (S01-R10), no por serie. Una serie guarda solo peso y repeticiones. El término se define en la Spec 02.
- [x] **Alcance de la nota.** Decidido el 2026-10-10 en la Spec 10: una nota fija del ejercicio (F01) y una nota de la sesión (F02). No hay nota del ejercicio dentro de una sesión.
- [x] **Alcance de "Última vez".** Decidido el 2026-10-10: todas las series del ejercicio en la sesión más reciente que lo contiene, ordenadas como se registraron. Detalle en la Spec 10.
- [x] **Respaldo automático de Android.** Decidido el 2026-10-10: `android:allowBackup="true"` se mantiene. S01-R05 y la exclusión de copias de seguridad se refieren a funciones de la app, no al respaldo del sistema.
