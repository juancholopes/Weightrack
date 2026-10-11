# Spec 02: Glosario

Oct 10, 2026 · @Juan

| ID | Estado | Fase | Depende de |
| --- | --- | --- | --- |
| 02 | Aprobada | 0 Fundamentos | 00 |

## Propósito

Esta spec fija el vocabulario de Weightrack. Cada término tiene un solo significado y un solo nombre.

Sin un glosario, una misma cosa termina con tres nombres en specs, código y pantallas. Eso confunde a los agentes y rompe la trazabilidad.

No define el modelo ni las tablas: eso lo hacen la Spec 10 (modelo de dominio) y la Spec 20 (persistencia). Tampoco agrega funcionalidad: solo nombra lo que ya existe en la constitución y en la Spec 01.

## Cómo se usa

Un agente consulta este glosario cada vez que escribe una spec, un nombre en el código o un texto de la interfaz.

1. **En las specs** usa el término exacto de la columna "Término", sin sinónimos.
2. **En el código** usa el identificador en inglés de la columna "Nombre en código". El idioma del código se decidió el 2026-10-10 en la constitución.
3. **En la interfaz** usa el término en español, escrito en los recursos de strings.
4. **Ante un término nuevo** no lo inventa: propone agregarlo aquí y espera la aprobación del dueño.
5. **Ante un término de la lista "a evitar"** lo reemplaza por el término canónico.

## Términos del dominio

Los términos de la primera versión cubren las features F00 a F04 de la Spec 01.

| Término | Definición | Nombre en código | Notas / Ejemplo |
| --- | --- | --- | --- |
| Ejercicio | Un movimiento que el usuario entrena y del que registra el peso. Incluye tanto los ejercicios con peso libre como los que se hacen en una máquina. | `Exercise` | Es el término canónico. "Máquina" no es una entidad aparte: es un valor del equipo de un ejercicio. Ejemplo: "Press de banca", "Prensa de piernas". |
| Equipo | Con qué se ejecuta un ejercicio. Es una lista fija: máquina, polea, mancuerna, barra y peso corporal. | `Equipment` | Valores en código: `MACHINE`, `CABLE`, `DUMBBELL`, `BARBELL`, `BODYWEIGHT`. Las etiquetas en español viven en `strings.xml`. Definido en la Spec 10. |
| Músculo | Un grupo muscular mediano que trabaja un ejercicio. Es una lista fija de 14 valores que el usuario no amplía. | `Muscle` | Ejemplo: pecho (`CHEST`), dorsales (`LATS`), cuádriceps (`QUADRICEPS`). La lista completa está en la Spec 10. |
| Músculo del ejercicio | La relación entre un ejercicio y un músculo que trabaja, marcada como principal o secundaria. | `ExerciseMuscle` | Un ejercicio no repite un músculo. Definido en la Spec 10. |
| Músculo principal | Un músculo que el ejercicio trabaja de forma principal. Los demás músculos del ejercicio son secundarios. | `isPrimary` | Todo ejercicio tiene al menos un músculo principal. |
| Archivar | Ocultar un ejercicio del catálogo y del registro de sesiones sin perder su historial. Un ejercicio archivado se puede desarchivar. | `archived` | Eliminar un ejercicio con series lo archiva. Uno sin series se borra de forma definitiva. Cumple S01-R07. Definido en la Spec 10. |
| Catálogo | La lista de ejercicios no archivados que el usuario crea, edita, archiva y elimina. Es propia del usuario y no viene precargada. | `ExerciseCatalog` | Lo implementa F01. Renombrar un ejercicio conserva su historial (S01-R07). |
| Sesión | Un entrenamiento concreto en un día del calendario, en el que el usuario registra series de uno o más ejercicios. | `WorkoutSession` | La fecha es solo el día, sin hora. Dos sesiones del mismo día se ordenan por orden de creación. En la interfaz se dice "sesión". En código se evita `Session` porque se confunde con una sesión de usuario o de login. Es independiente de cómo se armó (S01, Rutinas). |
| Serie | Una ejecución de un ejercicio dentro de una sesión, con un peso y un número de repeticiones. Se guarda como un registro propio. | `ExerciseSet` | En código se evita `Set` porque choca con `kotlin.collections.Set` y obliga a imports con alias. Ejemplo: 60 kg × 10 repeticiones. Cumple S01-R06. |
| Peso | El peso externo usado en una serie, en kilogramos con precisión de 0,25 kg. En un ejercicio de peso corporal es el peso adicional, y vale 0 cuando no hay. Nunca es negativo. | `weightGrams` | Decidido el 2026-10-10: se guarda como entero de gramos, múltiplo de 250, para no perder precisión. La interfaz siempre lo muestra en kilogramos, por ejemplo 62500 → "62,5 kg". No es el peso corporal (ver Medición corporal). |
| Repeticiones | Cuántas veces se ejecutó el movimiento en una serie. Es un número entero mayor que 0. | `reps` | Ejemplo: 10. |
| Última vez | Todas las series de un ejercicio en la sesión más reciente que lo contiene, en el orden en que se registraron, mostradas al registrarlo de nuevo. | `LastPerformance` | Decidido el 2026-10-10. Lo implementa F03 y cumple S01-R03. Se calcula a partir de las series guardadas, no se guarda aparte. |
| Evolución | Todas las series de un ejercicio con la fecha de su sesión, ordenadas en el tiempo. | `ExerciseEvolution` | Definido en la Spec 10. Es la consulta en la que se apoyan las gráficas de F05. |
| Peso más alto | El mayor peso de las series de un ejercicio en una sesión. | `heaviestWeightGrams` | Lo define S10-R34. Es el número de la tarjeta del inicio (F02, sobre la sesión más reciente) y la altura de cada barra de F05. Agregado el 2026-10-11. |
| Récord personal | Una sesión cuyo peso más alto supera al de todas las sesiones anteriores de ese ejercicio. La primera sesión no cuenta y un empate no es récord. | `isPersonalRecord` | Lo define S10-R35. Se muestra en F05 (S01-R12). Agregado el 2026-10-11. |
| Historial | El conjunto de sesiones pasadas del usuario, que puede listar, ver, corregir y eliminar. | `SessionHistory` | Lo implementa F04 y cumple S01-R04. |
| Nota | Un texto libre opcional que el usuario escribe. Hay solo dos tipos: la nota fija de un ejercicio y la nota de una sesión. | `Exercise.notes`, `WorkoutSession.notes` | Decidido el 2026-10-10. La nota del ejercicio la implementa F01 (ejemplo: "Asiento en la posición 4"). La nota de la sesión la implementa F02 (ejemplo: "Me sentí cansado"). No hay nota por serie ni nota del ejercicio dentro de una sesión. |

Términos de la visión a futuro. Se definen para reservar el nombre y evitar confusiones. **Futuro, no se implementa**: ningún agente agrega código, tablas ni pantallas para ellos (S01-A08).

| Término | Definición | Nombre en código | Notas / Ejemplo |
| --- | --- | --- | --- |
| Rutina | Un plan de ejercicios definido de antemano que se usa para iniciar una sesión. | `Routine` | Futuro, no se implementa. Una rutina no es una sesión: la rutina es el plan, la sesión es lo que se hizo. |
| Medición corporal | Un registro del peso corporal o de una medida del cuerpo en una fecha. | `BodyMeasurement` | Futuro, no se implementa. El peso corporal nunca se llama "peso" a secas. |
| Gráfica de progreso | Una vista de la evolución del peso y las repeticiones de un ejercicio en el tiempo. | `ProgressChart` | Se implementa en F05 Estadísticas por ejercicio (S01-R12). Se alimenta de las series guardadas (S01-R06). |

## Términos del proceso

Estos términos vienen de la constitución. Aquí solo se fija su significado.

| Término | Definición |
| --- | --- |
| Spec | Documento numerado que describe qué se construye y con qué criterios de aceptación. Es autosuficiente y verificable. |
| ADR | Registro de una decisión de arquitectura: qué se decidió, por qué y qué se descartó. Se usa para agregar librerías, módulos o patrones. |
| Requisito | Algo que el sistema debe cumplir. Se numera `S<spec>-R<nn>`, por ejemplo S02-R01. |
| Criterio de aceptación | Una condición comprobable que demuestra un requisito. Se numera `S<spec>-A<nn>` y apunta al requisito que comprueba y a su evidencia. |
| Evidencia | Lo que demuestra que un criterio se cumple: un comando, una prueba o una captura. |
| Feature | Una funcionalidad de la app con su propia spec, identificada con `F<nn>`. Vive en su propio paquete dentro de `feature/`. |
| Corte vertical | La forma de construir una feature: agrega su propia UI, estado, casos de uso y datos sin modificar las features existentes, salvo por contratos explícitos. |
| Estados de una spec | Borrador, Aprobada, Implementada y Obsoleta. Solo se implementa una spec Aprobada. |
| Registro de fallos | La anotación de cada fallo de un agente y del ajuste de la spec que lo evita. Su ubicación y formato siguen abiertos en la constitución. |

## Términos a evitar

Cada fila lista palabras que no se usan en specs, código ni interfaz, y su reemplazo.

| No usar | Usar | Motivo |
| --- | --- | --- |
| Máquina (como entidad aparte) | Ejercicio | Separarlos duplicaría el catálogo. "Máquina" solo aparece como valor del equipo de un ejercicio o dentro del nombre que el usuario le da. Por eso la pestaña del inicio con el catálogo se llama "Ejercicios" y no "Máquinas" (Spec 42, D3; anotado el 2026-10-11). |
| Rutina (para hablar de lo que se hizo hoy) | Sesión | Una rutina es un plan futuro, no un entrenamiento registrado. |
| Entreno, workout, entrenamiento (como entidad) | Sesión | Un solo nombre para el mismo concepto. "Sesión de entrenamiento" se acepta en prosa. |
| Set (en specs e interfaz en español) | Serie | El término del dominio en español es "serie". |
| `Set`, `Session` (como nombres de clase) | `ExerciseSet`, `WorkoutSession` | Chocan con `kotlin.collections.Set` y con el concepto de sesión de usuario. |
| Carga, kilos, libras | Peso | "Peso" siempre en kilogramos. No hay libras en la primera versión. En peso corporal se dice "peso adicional". |
| Peso (para el peso del cuerpo) | Medición corporal | "Peso" a secas es siempre el de una serie. |
| Récord (a secas), máximo, último peso | Última vez, peso más alto o récord personal | "Última vez" es el término de F03. En F05 se dicen "peso más alto" (de una sesión) y "récord personal", con el significado de la tabla de términos del dominio. "Máximo" y "récord" a secas siguen sin usarse. Cambiado el 2026-10-11 (Spec 42, D12). Antes: "Récords y máximos no están en el alcance". |
| Registro, log, bitácora (para las sesiones pasadas) | Historial | "Registro" queda para el registro de fallos del agente. |
| Lista de ejercicios, biblioteca | Catálogo | Un solo nombre para la lista que mantiene el usuario. |

## Requisitos

| ID | Requisito |
| --- | --- |
| S02-R01 | Toda spec usa los términos de este glosario con el significado definido aquí, sin sinónimos. |
| S02-R02 | Los nombres de clases, propiedades y paquetes del dominio siguen la columna "Nombre en código", en inglés. |
| S02-R03 | Los textos de la interfaz usan el término en español del glosario y viven en los recursos de strings. |
| S02-R04 | Ninguna spec, nombre en código ni texto de la interfaz usa un término de la lista "a evitar". |
| S02-R05 | Un término nuevo del dominio se agrega a este glosario, con aprobación del dueño, antes de usarse en una spec o en el código. |
| S02-R06 | Los términos marcados como futuros no aparecen en el código ni en la interfaz hasta que su feature tenga una spec aprobada. |

## Criterios de aceptación

| ID | Criterio | Comprueba | Evidencia |
| --- | --- | --- | --- |
| S02-A01 | Las Specs 10, 40 y F01 a F04 usan solo términos del glosario para los conceptos del dominio. | S02-R01 | Revisión de cada spec |
| S02-A02 | Las clases y propiedades del modelo de dominio de la Spec 10 se llaman como indica la columna "Nombre en código". | S02-R02 | Revisión de la Spec 10 y del paquete de dominio |
| S02-A03 | No existe ninguna clase llamada `Set` ni `Session` en el código de la app. | S02-R02, S02-R04 | `grep -rE "class (Set\|Session)\b" app/src` sin resultados |
| S02-A04 | Los recursos de strings no contienen "máquina" salvo como etiqueta del equipo `MACHINE`, ni "rutina", "workout", "set", "libras" ni "lbs". | S02-R03, S02-R04 | Búsqueda en `app/src/main/res/values*/strings.xml` |
| S02-A05 | Ningún término del dominio aparece en una spec aprobada sin estar en este glosario. | S02-R05 | Revisión de las specs aprobadas |
| S02-A06 | No existen clases, tablas ni pantallas con los nombres `Routine`, `BodyMeasurement` ni `ProgressChart`. | S02-R06 | `grep -rE "Routine\|BodyMeasurement\|ProgressChart" app/src` sin resultados |

## Decisiones abiertas

- [x] **Idioma del código.** Decidido el 2026-10-10 en la constitución: identificadores en inglés y textos de la app en español. La columna "Nombre en código" queda aprobada.
- [x] **Alcance de la Nota.** Decidido el 2026-10-10 en la Spec 10: una nota fija del ejercicio (F01) y una nota de la sesión (F02).
- [x] **Actualizar la Spec 01.** Hecho el 2026-10-10: la Spec 01 agrega S01-R10 y asigna la nota del ejercicio a F01 y la de la sesión a F02.
- [x] **Alcance de "Última vez".** Decidido el 2026-10-10: todas las series del ejercicio en la sesión más reciente que lo contiene. Lo detalla la Spec 10 (S10-R25).
- [x] **Nombre en código del peso.** Decidido el 2026-10-10: `weightGrams` reemplaza a `weightKg`, porque el peso se guarda como entero de gramos. La unidad visible sigue siendo el kilogramo.
- [x] **Términos de F05 y pestaña "Ejercicios".** Aplicado el 2026-10-11 por la Spec 42 (D3 y D12): se agregan "peso más alto" y "récord personal" (S10-R34, S10-R35), y se anota por qué la pestaña se llama "Ejercicios". Decidido el 2026-10-11 (el dueño aceptó de antemano las recomendaciones).
