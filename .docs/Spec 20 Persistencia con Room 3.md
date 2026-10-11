# Spec 20: Persistencia con Room 3

Oct 10, 2026 · @Juan

| ID | Estado | Fase | Depende de |
| --- | --- | --- | --- |
| 20 | Implementada | 1 Dominio y datos | 03, 10 |

## Propósito

Esta spec diseña cómo se guarda el modelo de dominio de la Spec 10 en una base de datos local con Room 3: tablas, columnas, claves, índices, DAOs, consultas, transacciones, repositorios, migraciones y pruebas.

No cambia el dominio. Las entidades, invariantes y preguntas de la Spec 10 son el contrato, y esta spec solo decide cómo se persisten y cómo se responden con SQL. Los modelos de dominio implementados (`Exercise`, `ExerciseMuscle`, `WorkoutSession`, `ExerciseSet`, `Equipment`, `Muscle`) se toman tal como están en el código.

Las decisiones del dueño que usa esta spec se tomaron el 2026-10-10 y se listan abajo. El dueño cerró todas las decisiones abiertas y aprobó esta spec el 2026-10-10. Implementada el 2026-10-11, con el esquema v1 versionado en Git (S20-A03).

## Decisiones del dueño

| Tema | Decisión | Fecha |
| --- | --- | --- |
| Inyección de dependencias | Manual: un `AppContainer` creado en una clase `Application` propia construye la base de datos y expone los repositorios. Cierra la decisión abierta de la constitución (ver "Cambios que esta spec pide a otras specs"). | 2026-10-10 |
| Entidades y dominio | Las entidades de Room y los modelos de dominio son clases distintas, con mappers entre ellas. | 2026-10-10 |
| Peso | Columna INTEGER de gramos, igual que `weightGrams` del dominio. | 2026-10-10 |
| Músculo | Sigue siendo un enum de código, como dice la Spec 10. No hay tabla de músculos. | 2026-10-10 |
| Enums y booleanos | Enums guardados por `name` como TEXT. Booleanos como INTEGER 0/1. | 2026-10-10 |
| Fecha de la sesión | Solo el día. Formato elegido en S20-R15. | 2026-10-10 |
| Migraciones | Versión 1, `exportSchema` activo, esquemas versionados en Git, migración destructiva prohibida, una prueba por migración desde la v2 y una prueba de validación del esquema v1 desde ya. | 2026-10-10 |
| Configuración de Room 3 | Paquete `androidx.room3`, KSP aplicado en `:app`, plugin de Gradle de Room 3, nombre de archivo fijo y `AppContainer` como único lugar que crea la instancia. | 2026-10-10 |
| Pruebas de base de datos | Opción A: pruebas instrumentadas en `src/androidTest`, en el teléfono o en un emulador. | 2026-10-10 |
| Driver | `AndroidSQLiteDriver` (`sqlite-framework`), en producción y en las pruebas. | 2026-10-10 |
| `Flow` en el dominio | El dominio puede usar `kotlinx.coroutines`, incluido `Flow`. Se enmienda S10-R16. | 2026-10-10 |
| Interfaces de repositorio | `ExerciseRepository`, `SessionRepository` y `ExerciseWriteResult` se aprueban tal como se proponen en S20-R42. | 2026-10-10 |
| Id de un objeto no guardado | `id = 0` significa que el objeto todavía no se guardó. Se registra en la Spec 10. | 2026-10-10 |
| Paquetes | `data/` en la raíz para `WeightrackDatabase` y `Converters`; `AppContainer` y `WeightrackApplication` en el paquete raíz. Se registra en la Spec 03. | 2026-10-10 |

## Ubicación del código

Bajo `com.juancholopes.weightrack`, siguiendo el esqueleto de la Spec 03. Las entidades, DAOs, mappers y repositorios de cada feature viven en su paquete `data/`. La base de datos es compartida, así que necesita un paquete fuera de `feature/` (ver "Cambios que esta spec pide a otras specs").

```
com/juancholopes/weightrack/
  WeightrackApplication.kt          Application propia, crea el AppContainer
  AppContainer.kt                   único lugar que construye la base de datos
  data/
    WeightrackDatabase.kt           @Database
    Converters.kt                   @ColumnTypeConverter de enums y fecha
  feature/catalog/
    domain/ExerciseRepository.kt    interfaz propuesta (S20-R42)
    data/                           ExerciseEntity, ExerciseMuscleEntity, ExerciseDao,
                                    ExerciseMappers, RoomExerciseRepository
  feature/session/
    domain/SessionRepository.kt     interfaz propuesta (S20-R42)
    data/                           WorkoutSessionEntity, ExerciseSetEntity, SessionDao,
                                    filas de consulta, SessionMappers, RoomSessionRepository
```

## Configuración de Room 3

| ID | Requisito |
| --- | --- |
| S20-R01 | Todo el código de persistencia importa de `androidx.room3`. Ningún archivo importa `androidx.room.` (línea 2.x). |
| S20-R02 | El plugin KSP se aplica en `app/build.gradle.kts`, y `room3-compiler` se declara con `ksp(...)`. El catálogo ya tiene KSP 2.3.12 desde la Spec 03. |
| S20-R03 | El plugin de Gradle `androidx.room3` se declara en el `build.gradle.kts` raíz con `apply false` y se aplica en `:app` con `room3 { schemaDirectory("$projectDir/schemas") }`. Los esquemas quedan en `app/schemas/` y se versionan en Git. |
| S20-R04 | La base de datos es `WeightrackDatabase` en `com.juancholopes.weightrack.data`, con `@Database(version = 1, exportSchema = true)`. El esquema de la v1 queda en `app/schemas/com.juancholopes.weightrack.data.WeightrackDatabase/1.json`. |
| S20-R05 | El archivo de la base de datos se llama `weightrack.db`. El builder de producción fija un `SQLiteDriver` con `setDriver(...)` como último paso, porque Room 3 lo exige. El driver es `AndroidSQLiteDriver` (decidido el 2026-10-10). |
| S20-R06 | El builder no llama a `fallbackToDestructiveMigration`, `fallbackToDestructiveMigrationFrom` ni `fallbackToDestructiveMigrationOnDowngrade`. Un cambio de esquema sin migración falla al abrir la base de datos en lugar de borrar datos (S01-R08). |
| S20-R07 | `WeightrackApplication` (declarada en el manifiesto con `android:name`) crea un único `AppContainer`. El `AppContainer` construye la base de datos de forma perezosa una sola vez y expone `exerciseRepository` y `sessionRepository` como interfaces del dominio. Ninguna otra clase llama a `Room.databaseBuilder` ni a `Room.inMemoryDatabaseBuilder` en `src/main`. |
| S20-R08 | Las versiones de Room 3, SQLite y librerías de prueba viven solo en `gradle/libs.versions.toml` (constitución). Las versiones exactas están en "Dependencias". |

## Tablas y columnas

Nombres de tablas y columnas en `snake_case`. Las propiedades de las entidades en Kotlin usan `camelCase` y fijan el nombre de columna con `@ColumnInfo(name = ...)`.

### `exercise` ← `Exercise`

| Columna | Tipo SQLite | Nulo | Clave | Dominio |
| --- | --- | --- | --- | --- |
| `id` | INTEGER | No | PK, `AUTOINCREMENT` | `Exercise.id` |
| `name` | TEXT | No | — | `Exercise.name`, ya normalizado por el dominio (S10-R04) |
| `normalized_name` | TEXT | No | Única | No existe en el dominio. Se calcula en el mapper (S20-R12) |
| `equipment` | TEXT | No | — | `Exercise.equipment`, por `name` |
| `notes` | TEXT | Sí | — | `Exercise.notes` |
| `archived` | INTEGER (0/1) | No | — | `Exercise.archived` |

### `exercise_muscle` ← `ExerciseMuscle`

| Columna | Tipo SQLite | Nulo | Clave | Dominio |
| --- | --- | --- | --- | --- |
| `exercise_id` | INTEGER | No | PK compuesta, FK → `exercise.id` | `ExerciseMuscle.exerciseId` |
| `muscle` | TEXT | No | PK compuesta | `ExerciseMuscle.muscle`, por `name` |
| `is_primary` | INTEGER (0/1) | No | — | `ExerciseMuscle.isPrimary` |

### `workout_session` ← `WorkoutSession`

| Columna | Tipo SQLite | Nulo | Clave | Dominio |
| --- | --- | --- | --- | --- |
| `id` | INTEGER | No | PK, `AUTOINCREMENT` | `WorkoutSession.id` |
| `date` | TEXT `yyyy-MM-dd` | No | — | `WorkoutSession.date` (S20-R15) |
| `notes` | TEXT | Sí | — | `WorkoutSession.notes` |

### `exercise_set` ← `ExerciseSet`

| Columna | Tipo SQLite | Nulo | Clave | Dominio |
| --- | --- | --- | --- | --- |
| `id` | INTEGER | No | PK, `AUTOINCREMENT` | `ExerciseSet.id` |
| `session_id` | INTEGER | No | FK → `workout_session.id` | `ExerciseSet.sessionId` |
| `exercise_id` | INTEGER | No | FK → `exercise.id` | `ExerciseSet.exerciseId` |
| `set_order` | INTEGER | No | Única con `session_id` | `ExerciseSet.order` |
| `weight_grams` | INTEGER | No | — | `ExerciseSet.weightGrams`, sin conversión |
| `reps` | INTEGER | No | — | `ExerciseSet.reps` |

### Requisitos del esquema

| ID | Requisito |
| --- | --- |
| S20-R09 | El esquema v1 tiene exactamente las cuatro tablas de arriba (más las internas de Room y SQLite, como `room_master_table` y `sqlite_sequence`). No hay tabla de músculos ni de equipos. |
| S20-R10 | Cada tabla tiene exactamente las columnas, tipos, nulabilidad y claves de su tabla. |
| S20-R11 | Las entidades (`ExerciseEntity`, `ExerciseMuscleEntity`, `WorkoutSessionEntity`, `ExerciseSetEntity`) son clases distintas de los modelos de dominio y viven en los paquetes `data/`. El dominio no importa ninguna entidad, DAO ni clase de Room (S10-R16). |
| S20-R12 | Cada entidad tiene mappers en los dos sentidos (`toEntity()` y `toDomain()`). `normalized_name` se calcula como `nameKey(normalizeExerciseName(name))`, con las funciones del dominio, no con una copia. Así la clave de unicidad de la base de datos es la misma que usa S10-R05: espacios normalizados, `lowercase(Locale.ROOT)` y tildes distinguidas. |
| S20-R13 | `Equipment` y `Muscle` se guardan con su `name` como TEXT, mediante `@ColumnTypeConverter` explícitos en `Converters` y registrados con `@ColumnTypeConverters` en `WeightrackDatabase`. Al leer, un valor desconocido lanza una excepción, nunca se cambia por un valor por defecto. Nunca se guarda el `ordinal`. |
| S20-R14 | Los booleanos (`archived`, `is_primary`) se guardan como INTEGER 0 o 1 con el mapeo nativo de Room, sin conversor propio. |
| S20-R15 | La fecha de la sesión se guarda como TEXT ISO-8601 `yyyy-MM-dd`, escrita con `LocalDate.toString()` y leída con `LocalDate.parse()` en un `@ColumnTypeConverter`. Nunca se deriva de un `Instant`, de milisegundos ni de una zona horaria. |
| S20-R16 | El peso se guarda en `weight_grams` como el mismo entero de gramos del dominio, sin conversión a kilogramos ni a coma flotante. |
| S20-R17 | Las tres claves `id` usan `@PrimaryKey(autoGenerate = true)`, que en SQLite genera `INTEGER PRIMARY KEY AUTOINCREMENT`. Al insertar, el repositorio pasa `id = 0` y usa el id que devuelve Room. Nunca inserta un `id` explícito. |
| S20-R18 | La columna del orden se llama `set_order`, porque `ORDER` es palabra reservada de SQL. |

Conversores necesarios en `Converters`:

| Conversor | De | A |
| --- | --- | --- |
| `equipmentToName` / `nameToEquipment` | `Equipment?` | `String?` (`name` / `Equipment.valueOf`) |
| `muscleToName` / `nameToMuscle` | `Muscle?` | `String?` (`name` / `Muscle.valueOf`) |
| `dateToIso` / `isoToDate` | `LocalDate?` | `String?` (`toString()` / `LocalDate.parse`) |

Los conversores aceptan tipos anulables para poder usarlos también en parámetros opcionales de consultas (filtros del catálogo y `beforeDate`). Las columnas siguen siendo `NOT NULL`. Booleanos, `Int` y `Long` no necesitan conversor.

**Por qué el músculo como enum no necesita tabla semilla.** Los 14 músculos existen en el código, no en la base de datos. `exercise_muscle.muscle` guarda el `name` del enum, así que no hay filas que precargar al crear la base de datos: ni `RoomDatabase.Callback.onCreate` con `INSERT`, ni `createFromAsset`. Eso evita duplicados en el primer arranque, porque no hay ningún proceso de siembra que pueda ejecutarse dos veces, quedar a medias o competir con la primera lectura, y evita que la tabla y el enum se desalineen. La validez del valor la garantiza el tipo en Kotlin al escribir y el conversor al leer (S20-R13). El costo es que renombrar o quitar un valor del enum exige una migración que reescriba las filas (Spec 10, S20-R44).

**Por qué TEXT `yyyy-MM-dd` y no epoch-day INTEGER.** Las dos opciones ordenan bien y ninguna tiene zona horaria. Se elige TEXT porque:

- **Orden.** Con año de cuatro dígitos, el orden lexicográfico del texto coincide con el cronológico, así que `ORDER BY date` y `date < :beforeDate` funcionan sin conversión. S10-R32 impide fechas futuras, así que no aparecen años de cinco dígitos.
- **Legibilidad.** En el Database Inspector y en el JSON del esquema se lee "2026-10-10", no 20736. Ayuda a depurar y a revisar las pruebas de migración.
- **Sin corrimiento de zona.** `LocalDate` no tiene hora ni zona, y su texto ISO tampoco. No hay conversión a `Instant` que pueda mover el día al cambiar de zona horaria.
- **Coherencia con S10-R13.** La columna representa un día del calendario y nada más, igual que `LocalDate`.

El costo frente a epoch-day es unos bytes más por fila, despreciable con un solo usuario.

## Integridad referencial

| ID | Requisito |
| --- | --- |
| S20-R19 | `PRAGMA foreign_keys` está activo en cada conexión. Room lo activa al abrir la base de datos cuando hay entidades con claves foráneas, y una prueba lo comprueba (S20-A06). |
| S20-R20 | `exercise_set.session_id` → `workout_session.id` con `onDelete = CASCADE`. Eliminar una sesión elimina sus series (S10-R23). |
| S20-R21 | `exercise_muscle.exercise_id` → `exercise.id` con `onDelete = CASCADE`. Borrar de forma definitiva un ejercicio borra sus músculos (S10-R20). |
| S20-R22 | `exercise_set.exercise_id` → `exercise.id` con `onDelete = RESTRICT`. El dominio archiva los ejercicios con historial (S10-R17), así que la base de datos bloquea el borrado definitivo de un ejercicio con series como red de seguridad. |
| S20-R23 | Las tres claves foráneas usan `onUpdate = RESTRICT`, porque los `id` no cambian (S10-R14). Cambiar un `id` referenciado falla en lugar de propagarse. Ninguna es `deferred`. |
| S20-R24 | `exercise.normalized_name` tiene un índice único. Dos ejercicios cuyo nombre normalizado coincide no pueden existir, aunque uno esté archivado (S10-R05). |
| S20-R25 | La clave primaria compuesta (`exercise_id`, `muscle`) de `exercise_muscle` es la restricción única que impide repetir un músculo en un ejercicio (S10-R08). |
| S20-R26 | (`session_id`, `set_order`) tiene un índice único en `exercise_set` (S10-R09). |
| S20-R27 | Existen los índices de la tabla de índices de abajo, con esos nombres y columnas. |
| S20-R28 | La base de datos no repite con `CHECK` las invariantes de valor del dominio (S10-R01 a S10-R03, S10-R07, S10-R09 en su parte de `order > 0`, S10-R11, S10-R15, S10-R32). Las valida el dominio antes de escribir. La base de datos garantiza solo existencia de referencias, unicidad y tipos. Un peso de 0 g se guarda como `0`, nunca como `NULL`. |

S10-R10 (toda serie referencia una sesión y un ejercicio existentes) se cumple con S20-R20 y S20-R22: las dos columnas son `NOT NULL` y con clave foránea activa.

S10-R14 (el `id` de sesión crece en orden de creación) se cumple con S20-R17. Con `AUTOINCREMENT`, SQLite asigna un id mayor que cualquiera que haya existido en la tabla, incluso si se borró la última sesión. Sin `AUTOINCREMENT`, un id borrado podría reutilizarse. Es un requisito de esta spec que ningún insert pase un `id` explícito, porque eso movería la secuencia.

### Índices

| Nombre | Tabla | Columnas | Único | Consulta que sirve |
| --- | --- | --- | --- | --- |
| `index_exercise_normalized_name` | `exercise` | `normalized_name` | Sí | Unicidad del nombre (S10-R05), búsqueda por nombre y orden del catálogo (S10-R30, S10-R31) |
| `index_exercise_archived_equipment` | `exercise` | `archived`, `equipment` | No | Catálogo activo y archivados, con filtro opcional por equipo (S10-R30, S10-R31) |
| clave primaria de `exercise_muscle` | `exercise_muscle` | `exercise_id`, `muscle` | Sí | Músculos de un ejercicio, unicidad (S10-R08) y cascada desde `exercise` |
| `index_exercise_muscle_muscle_exercise_id` | `exercise_muscle` | `muscle`, `exercise_id` | No | Filtro del catálogo por músculo (S10-R30) |
| `index_workout_session_date_id` | `workout_session` | `date`, `id` | No | Historial y orden de sesiones (S10-R25 a S10-R28) |
| `index_exercise_set_session_id_set_order` | `exercise_set` | `session_id`, `set_order` | Sí | Series de una sesión (S10-R29), unicidad del orden (S10-R09) y cascada desde `workout_session` |
| `index_exercise_set_exercise_id_session_id` | `exercise_set` | `exercise_id`, `session_id` | No | Series por ejercicio: última vez, evolución, `RESTRICT` y "tiene series" (S10-R17, S10-R25, S10-R27) |

Cada columna de clave foránea es la primera de un índice, como recomienda SQLite para que `CASCADE` y `RESTRICT` no recorran la tabla entera.

Nota sobre el orden por nombre: el dominio ordena con `String.compareTo` (UTF-16) y SQLite con `BINARY` (bytes UTF-8). Coinciden para todo carácter del plano básico; solo difieren entre caracteres fuera del plano básico (por ejemplo, emojis) y los del rango U+E000 a U+FFFF. Se acepta esa diferencia.

## DAOs y consultas

Room 3 exige que toda función de DAO sea `suspend` o devuelva un tipo reactivo. En esta spec, las lecturas que la UI observa devuelven `Flow` y las escrituras y las lecturas internas de una transacción son `suspend`.

Filas de consulta (clases de `data/`, no entidades):

- `SetWithSessionRow`: `@Embedded set: ExerciseSetEntity` y `@Embedded(prefix = "ws_") session: WorkoutSessionEntity`.
- `SetWithExerciseRow`: `@Embedded set: ExerciseSetEntity` y `@Embedded(prefix = "ex_") exercise: ExerciseEntity`.

### Preguntas de la Spec 10

| ID | Requisito |
| --- | --- |
| S20-R29 | **Última vez (S10-R25, S10-R26).** `SessionDao.observeLastPerformance(exerciseId: Long, beforeDate: LocalDate?, beforeId: Long?): Flow<List<SetWithSessionRow>>` devuelve las series del ejercicio en la sesión más reciente que lo contiene. Con `beforeId` nulo considera todas las sesiones. Con `beforeDate` y `beforeId`, solo las sesiones anteriores a esa según el orden de sesiones, lo que excluye la sesión en curso y las posteriores. Lista vacía significa "sin última vez". |
| S20-R30 | **Evolución (S10-R27).** `SessionDao.observeEvolution(exerciseId: Long): Flow<List<SetWithSessionRow>>` devuelve todas las series del ejercicio con su sesión, por fecha, `id` de sesión y `set_order` ascendentes. |
| S20-R31 | **Historial (S10-R28).** `SessionDao.observeHistory(): Flow<List<WorkoutSessionEntity>>` ordena por `date` descendente y desempata por `id` descendente. |
| S20-R32 | **Series de una sesión (S10-R29).** `SessionDao.observeSessionSets(sessionId: Long): Flow<List<SetWithExerciseRow>>` devuelve las series por `set_order` ascendente, cada una con su ejercicio, esté archivado o no. |
| S20-R33 | **Catálogo activo (S10-R30).** `ExerciseDao.observeActive(muscle: Muscle?, equipment: Equipment?): Flow<List<ExerciseEntity>>` devuelve los ejercicios con `archived = 0` ordenados por `normalized_name`, con filtros opcionales que se combinan. El filtro de músculo incluye principales y secundarios. |
| S20-R34 | **Archivados (S10-R31).** `ExerciseDao.observeArchived(): Flow<List<ExerciseEntity>>` devuelve los ejercicios con `archived = 1` ordenados por `normalized_name`. |
| S20-R35 | **Archivar y desarchivar (S10-R17, S10-R19).** `ExerciseDao.setArchived(id: Long, archived: Boolean): Int` es `suspend` y cambia solo `archived`. Devuelve el número de filas cambiadas. |
| S20-R36 | **Borrado definitivo (S10-R20).** `ExerciseDao.deleteIfHasNoSets(id: Long): Int` es `suspend` y borra el ejercicio solo si no tiene series. Devuelve 0 si tiene series o no existe. Sus `exercise_muscle` se borran por cascada. |
| S20-R37 | Las demás lecturas y escrituras que usan los repositorios siguen la misma regla: `Flow` para lo que se observa (`observeExercise`, `observeMuscles`, `observeSession`) y `suspend` para escrituras (`insert`, `update`, `delete`) y para lecturas dentro de una transacción (`countSetsOfExercise`, `findByNormalizedName`, `setsOfSession`). |

### SQL

Última vez (S20-R29). Se usa el mismo parámetro varias veces, lo que Room admite:

```sql
SELECT es.*, ws.id AS ws_id, ws.date AS ws_date, ws.notes AS ws_notes
FROM exercise_set es
JOIN workout_session ws ON ws.id = es.session_id
WHERE es.exercise_id = :exerciseId
  AND es.session_id = (
    SELECT ws2.id
    FROM workout_session ws2
    WHERE EXISTS (
        SELECT 1 FROM exercise_set es2
        WHERE es2.session_id = ws2.id AND es2.exercise_id = :exerciseId)
      AND (:beforeId IS NULL
           OR ws2.date < :beforeDate
           OR (ws2.date = :beforeDate AND ws2.id < :beforeId))
    ORDER BY ws2.date DESC, ws2.id DESC
    LIMIT 1)
ORDER BY es.set_order ASC
```

Evolución (S20-R30):

```sql
SELECT es.*, ws.id AS ws_id, ws.date AS ws_date, ws.notes AS ws_notes
FROM exercise_set es
JOIN workout_session ws ON ws.id = es.session_id
WHERE es.exercise_id = :exerciseId
ORDER BY ws.date ASC, ws.id ASC, es.set_order ASC
```

Historial (S20-R31):

```sql
SELECT * FROM workout_session ORDER BY date DESC, id DESC
```

Series de una sesión (S20-R32). `JOIN` interno porque la clave foránea garantiza que el ejercicio existe:

```sql
SELECT es.*,
       e.id AS ex_id, e.name AS ex_name, e.normalized_name AS ex_normalized_name,
       e.equipment AS ex_equipment, e.notes AS ex_notes, e.archived AS ex_archived
FROM exercise_set es
JOIN exercise e ON e.id = es.exercise_id
WHERE es.session_id = :sessionId
ORDER BY es.set_order ASC
```

Catálogo activo (S20-R33):

```sql
SELECT e.* FROM exercise e
WHERE e.archived = 0
  AND (:equipment IS NULL OR e.equipment = :equipment)
  AND (:muscle IS NULL OR EXISTS (
        SELECT 1 FROM exercise_muscle m
        WHERE m.exercise_id = e.id AND m.muscle = :muscle))
ORDER BY e.normalized_name ASC
```

Archivados (S20-R34):

```sql
SELECT * FROM exercise WHERE archived = 1 ORDER BY normalized_name ASC
```

Archivar y desarchivar (S20-R35):

```sql
UPDATE exercise SET archived = :archived WHERE id = :id
```

Borrado definitivo solo sin series (S20-R36):

```sql
DELETE FROM exercise
WHERE id = :id
  AND NOT EXISTS (SELECT 1 FROM exercise_set WHERE exercise_id = :id)
```

¿Tiene series? (para la decisión de borrar o archivar):

```sql
SELECT COUNT(*) FROM exercise_set WHERE exercise_id = :exerciseId
```

Búsqueda por nombre normalizado (para informar una colisión, S20-R41):

```sql
SELECT * FROM exercise WHERE normalized_name = :normalizedName
```

`ExerciseDao` consulta `exercise_set` para contar series. Es la única lectura cruzada entre features y es el contrato que fija la Spec 10 para S01-R09: `ExerciseSet` referencia a `Exercise` por `id`.

### Transacciones

| ID | Requisito |
| --- | --- |
| S20-R38 | **Crear o editar un ejercicio con sus músculos** es atómico. `ExerciseDao.insertWithMuscles(exercise, muscles): Long` es una función `@Transaction` que inserta el ejercicio, toma el id devuelto, asigna ese id a cada músculo e inserta los músculos. `ExerciseDao.updateWithMuscles(exercise, muscles)` actualiza la fila, borra los músculos del ejercicio e inserta los nuevos. Si cualquier paso falla, no queda nada escrito. |
| S20-R39 | **Guardar una sesión con todas sus series o ninguna.** `SessionDao.insertSessionWithSets(session, sets): Long` es una función `@Transaction` que inserta la sesión, asigna su id a cada serie e inserta las series. Si una serie falla (por ejemplo por clave foránea o por `set_order` repetido), tampoco queda la sesión. |
| S20-R40 | **Decidir entre borrar y archivar** ocurre en una sola transacción de escritura: el repositorio lee el ejercicio, sus músculos y si tiene series, llama a `deleteExercise(...)` del dominio y aplica el resultado (`setArchived(id, true)` o `deleteIfHasNoSets(id)`). Ninguna serie insertada entre la lectura y la escritura puede dejar un ejercicio con series borrado. Como esta operación llama al dominio, se ejecuta en el repositorio con la API de transacciones de Room 3 (`withWriteTransaction` o `useWriterConnection { it.immediateTransaction { ... } }`), no en una función `@Transaction` del DAO. |
| S20-R41 | **Colisión de nombre.** Si al crear o editar un ejercicio el índice único de `normalized_name` rechaza la escritura, el repositorio no deja escapar la excepción de SQLite: busca el ejercicio que colisiona y devuelve `ExerciseError.NAME_TAKEN_BY_ARCHIVED` si está archivado o `ExerciseError.NAME_TAKEN` si no (S10-R21). La validación del dominio sigue siendo la primera barrera; el índice cubre el caso en que la lista `existing` estaba desactualizada. |

## Repositorios

La Spec 10 dejó las interfaces de repositorio a la Spec 30, que no existía al escribir esta spec. Para que esta spec fuera implementable, define interfaces mínimas en los paquetes de dominio. El dueño las aprobó el 2026-10-10, y la Spec 30 las mantiene como contrato desde el 2026-10-11 (ADR-30-05).

| ID | Requisito |
| --- | --- |
| S20-R42 | Existen dos interfaces en el dominio, con solo tipos del dominio en sus firmas: `ExerciseRepository` en `feature/catalog/domain` y `SessionRepository` en `feature/session/domain`. |
| S20-R43 | `RoomExerciseRepository` (en `feature/catalog/data`) implementa `ExerciseRepository` y `RoomSessionRepository` (en `feature/session/data`) implementa `SessionRepository`. Reciben sus DAOs (y la base de datos para S20-R40) por constructor desde el `AppContainer`. Ninguna entidad ni fila de consulta sale de un repositorio. |

Interfaces aprobadas el 2026-10-10:

```kotlin
// feature/catalog/domain
interface ExerciseRepository {
    fun observeActiveCatalog(muscle: Muscle? = null, equipment: Equipment? = null): Flow<List<Exercise>>
    fun observeArchived(): Flow<List<Exercise>>
    fun observeExercise(id: Long): Flow<Exercise?>
    fun observeMuscles(exerciseId: Long): Flow<List<ExerciseMuscle>>
    suspend fun create(exercise: Exercise, muscles: List<ExerciseMuscle>): ExerciseWriteResult
    suspend fun update(exercise: Exercise, muscles: List<ExerciseMuscle>): ExerciseWriteResult
    suspend fun delete(exerciseId: Long): ExerciseDeletion
    suspend fun unarchive(exerciseId: Long)
}

sealed interface ExerciseWriteResult {
    data class Saved(val id: Long) : ExerciseWriteResult
    data class Rejected(val error: ExerciseError) : ExerciseWriteResult
}

// feature/session/domain
interface SessionRepository {
    fun observeHistory(): Flow<List<WorkoutSession>>
    fun observeSession(id: Long): Flow<WorkoutSession?>
    fun observeSessionSets(sessionId: Long): Flow<List<Pair<ExerciseSet, Exercise>>>
    fun observeLastPerformance(exerciseId: Long, before: WorkoutSession? = null): Flow<LastPerformance?>
    fun observeEvolution(exerciseId: Long): Flow<ExerciseEvolution>
    suspend fun setsOfSession(sessionId: Long): List<ExerciseSet>
    suspend fun createSession(session: WorkoutSession, sets: List<ExerciseSet> = emptyList()): Long
    suspend fun updateSession(session: WorkoutSession)
    suspend fun deleteSession(sessionId: Long)
    suspend fun addSet(set: ExerciseSet): Long
    suspend fun updateSet(set: ExerciseSet)
    suspend fun deleteSet(setId: Long)
}
```

Las firmas reutilizan los tipos que ya existen en el dominio: `ExerciseDeletion`, `ExerciseError`, `LastPerformance`, `ExerciseEvolution` y el par `(ExerciseSet, Exercise)` que devuelve `sessionSets`. El único tipo nuevo es `ExerciseWriteResult`. `observeLastPerformance` recibe la `WorkoutSession` en curso, igual que `lastPerformance(before = ...)` del dominio, y pasa su `date` e `id` al DAO.

Un objeto que todavía no se guardó lleva `id = 0` (y `exerciseId = 0` o `sessionId = 0` en sus hijos). El repositorio ignora esos valores al insertar y usa el id que asigna la base de datos (S20-R17).

## Migraciones

| ID | Requisito |
| --- | --- |
| S20-R44 | Todo cambio del esquema sube `version` en uno, exporta el nuevo JSON a `app/schemas/` en el mismo commit y agrega una `Migration(n, n + 1)` escrita a mano o una `@AutoMigration` declarada, nunca una migración destructiva (S20-R06). Esto incluye renombrar o quitar un valor de `Equipment` o `Muscle`, que exige reescribir las filas guardadas. |
| S20-R45 | Cada migración desde la v2 tiene su propia prueba con `MigrationTestHelper` (`room3-testing`): crea la base de datos en la versión anterior a partir del JSON exportado, inserta datos, migra, valida el esquema y comprueba que las sesiones y series siguen ahí (S01-A07). Desde ya existe una prueba que crea la v1 desde su JSON y la abre con Room sin errores de validación. |

## Pruebas

| ID | Requisito |
| --- | --- |
| S20-R46 | Los DAOs, las restricciones, las transacciones y los repositorios se prueban contra una base de datos en memoria (`Room.inMemoryDatabaseBuilder`) con el mismo driver que producción, una base de datos nueva por prueba y cerrada en `@After`. Viven en `src/androidTest` y corren en el teléfono o en un emulador (opción A, decidida el 2026-10-10). |
| S20-R47 | Los mappers y los conversores tienen pruebas unitarias en `src/test`, sin base de datos ni emulador, que corren con `./gradlew test`. |

### Dónde corren las pruebas de base de datos

Investigado el 2026-10-10 en la documentación de Android Developers:

- La guía "Test and debug your database" dice que con Room KMP las pruebas pueden correr en la máquina de desarrollo con pruebas JVM locales y `Room.inMemoryDatabaseBuilder<T>()` sin `Context`, usando `BundledSQLiteDriver`. Aclara que esa forma sin `Context` es para destinos JVM de escritorio. La misma guía dice: "We don't recommend Android local unit tests with Robolectric. Use local JVM tests using Room KMP instead." Para Android recomienda una prueba JUnit en un dispositivo.
- La guía de migraciones de Room 3 muestra `MigrationTestHelper` con `instrumentation` y `AndroidSQLiteDriver`, es decir, como prueba instrumentada.
- Weightrack es un solo módulo Android (`:app`). Sus pruebas de `src/test` resuelven la variante Android de las librerías, cuyo builder pide un `Context` y cuyo `sqlite-bundled` empaqueta binarios nativos para Android, no para la JVM del equipo. Por eso la vía JVM de la guía exige un módulo JVM o KMP aparte. Esta deducción no la afirma la documentación y queda por verificar con una prueba de humo si se elige la opción C.

| Opción | Cómo | A favor | En contra | Dependencias que agrega |
| --- | --- | --- | --- | --- |
| A. Instrumentadas | `src/androidTest`, en emulador o teléfono, con el mismo driver de producción | Es lo que recomienda Google para Android. SQLite real del dispositivo. `MigrationTestHelper` ya exige instrumentación, así que todo queda en un mismo lugar | Necesita un dispositivo. `./gradlew assembleDebug test` (S03-R14) no las corre; se agrega `./gradlew connectedDebugAndroidTest`. Más lentas | `room3-testing`, `kotlinx-coroutines-test` en `androidTestImplementation` |
| B. Robolectric | `src/test` con Robolectric y `AndroidSQLiteDriver` | Corren con `./gradlew test`, sin dispositivo | Google lo desaconseja para Room. El SQLite de Robolectric no es el del dispositivo. Las pruebas de migración seguirían siendo instrumentadas | `org.robolectric:robolectric` (versión por verificar), `androidx.test:core`, `kotlinx-coroutines-test` en `testImplementation` |
| C. Módulo JVM o KMP | Mover entidades, DAOs y base de datos a un módulo con destino JVM y probar con `BundledSQLiteDriver` | Rápidas, sin dispositivo, camino oficial de Room KMP | Exige modularizar, lo que la constitución pide justificar con un ADR (principio 6). Cambia la estructura de la Spec 03 | `sqlite-bundled`, `kotlinx-coroutines-test`, plugin de Kotlin JVM o KMP |

**Decidido el 2026-10-10: A.** Sigue la guía oficial, prueba el mismo SQLite y driver que usa la app, y no agrega Robolectric ni módulos. El costo es depender de un dispositivo; se puede reducir con un dispositivo administrado por Gradle (Gradle Managed Devices), que también tendría que aprobarse. Los mappers y conversores siguen en `src/test` (S20-R47).

## Dependencias

Versiones verificadas el 2026-10-10. Se agregan a `gradle/libs.versions.toml`.

| Artefacto | Versión | Uso | Fuente |
| --- | --- | --- | --- |
| `androidx.room3:room3-runtime` | 3.0.3 (9 de septiembre de 2026) | `implementation` | [Room 3 releases](https://developer.android.com/jetpack/androidx/releases/room3) |
| `androidx.room3:room3-compiler` | 3.0.3 | `ksp` | [Room 3 releases](https://developer.android.com/jetpack/androidx/releases/room3) |
| Plugin `androidx.room3` | 3.0.3 | `apply false` en la raíz, aplicado en `:app` | [Room 3 releases](https://developer.android.com/jetpack/androidx/releases/room3), [Migrate your Room database](https://developer.android.com/training/data-storage/room/migrating-db-versions) |
| `androidx.room3:room3-testing` | 3.0.3 | `androidTestImplementation` (opción A) | [Migrate your Room database](https://developer.android.com/training/data-storage/room/migrating-db-versions) |
| `androidx.sqlite:sqlite-framework` (`AndroidSQLiteDriver`) | 2.7.1 (9 de septiembre de 2026) | `implementation`. Driver elegido el 2026-10-10 | [SQLite releases](https://developer.android.com/jetpack/androidx/releases/sqlite) |
| `androidx.sqlite:sqlite-bundled` (`BundledSQLiteDriver`) | 2.7.1 | No se usa: el driver elegido es `AndroidSQLiteDriver` | [SQLite releases](https://developer.android.com/jetpack/androidx/releases/sqlite) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.11.0 (mayo de 2026). Verificado con Kotlin 2.4.21 el 2026-10-11 | `androidTestImplementation` (opción A) | [kotlinx.coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases) |
| `com.google.devtools.ksp` | 2.3.12, ya en el catálogo. Verificado con Kotlin 2.4.21 el 2026-10-11 | Se aplica en `:app` | Spec 03 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.11.0 | `implementation`. Agregada el 2026-10-11: sin ella la app resuelve 1.9.0 y `kotlinx-coroutines-test` 1.11.0 falla en el APK de prueba (`NoSuchMethodError`) | [kotlinx.coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.11.0 | `implementation`. Agregada el 2026-10-11: alineada con `kotlinx-coroutines-core` para no mezclar versiones | [kotlinx.coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases) |
| `org.jetbrains.kotlinx:kotlinx-serialization-core` | 1.11.0 | `runtimeOnly`. Agregada el 2026-10-11: `lifecycle` 2.11.0 fija 1.7.3 y `MigrationTestHelper` de `room3-testing` exige 1.8.1 o más (`AbstractMethodError`). `androidTestImplementation` no basta porque el APK de prueba hereda la versión estricta de la app | [kotlinx.serialization releases](https://github.com/Kotlin/kotlinx.serialization/releases) |

**2026-10-11: dependencias agregadas por compatibilidad, autorizado por el dueño.** `kotlinx-coroutines-core`, `kotlinx-coroutines-android` y `kotlinx-serialization-core` se declaran explícitamente con los motivos de la tabla. Ninguna cambia la línea de Kotlin. El dominio puede usar `Flow` desde el 2026-10-10 (S10-R16).

Para `MigrationTestHelper`, el plugin `androidx.room3` agrega por sí mismo el directorio de esquemas como assets de `androidTest` (verificado el 2026-10-11). No se declara un `assets.srcDir` manual.

## Cómo cumple S01-R06 a S01-R09

Lo pide S01-A05. Completa la tabla de la Spec 10 para la persistencia.

| Requisito de la Spec 01 | Cómo lo cumple la persistencia |
| --- | --- |
| S01-R06 | Cada serie es una fila de `exercise_set` con `exercise_id`, `weight_grams`, `reps` y `session_id`; la fecha sale de `workout_session.date` por `JOIN`. No hay columnas ni tablas de resumen: la última vez y la evolución son consultas (S20-R29, S20-R30). |
| S01-R07 | Renombrar actualiza `name` y `normalized_name` sin tocar `id` ni series (S20-R38). `RESTRICT` impide borrar un ejercicio con series (S20-R22) y el repositorio lo archiva (S20-R40). |
| S01-R08 | `exportSchema` activo, esquemas en Git, una migración y una prueba por versión, y migración destructiva prohibida (S20-R03, S20-R06, S20-R44, S20-R45). |
| S01-R09 | `exercise` y `exercise_muscle` son de F01 (`feature/catalog/data`); `workout_session` y `exercise_set` son de F02 (`feature/session/data`). El único contrato entre ellas es la clave foránea `exercise_set.exercise_id` y la cuenta de series que lee `ExerciseDao`. Una feature futura agrega sus tablas y DAOs y suma sus entidades a `WeightrackDatabase` con una migración. |

## Fuera de alcance de esta spec

- Pantallas, ViewModels, estado de UI y textos: Specs 40 a 42 y specs F.
- Casos de uso y la forma final de los repositorios: Spec 30. Esta spec solo propone interfaces mínimas, y la Spec 30 las mantiene (ADR-30-05).
- Implementación de las features F00 a F04.
- Modularización en varios módulos Gradle, salvo que el dueño elija la opción C con su ADR.
- Copias de seguridad, exportación o importación como funciones de la app (S01).
- Tablas de las features futuras (rutinas, mediciones corporales, gráficas).

## Criterios de aceptación

Las pruebas de base de datos viven en `src/androidTest` (opción A). Las de mappers y conversores viven en `src/test`. Cada prueba de base de datos usa una base de datos en memoria nueva.

| ID | Criterio | Comprueba | Evidencia |
| --- | --- | --- | --- |
| S20-A01 | Ningún archivo importa `androidx.room.`; la persistencia usa `androidx.room3`. | S20-R01 | `grep -rnE "import androidx\.room\." app/src` sin resultados |
| S20-A02 | KSP y el plugin `androidx.room3` se aplican en `app/build.gradle.kts`; la raíz los declara con `apply false`; ninguna versión literal en los `build.gradle.kts`. | S20-R02, S20-R03, S20-R08 | Revisión de los tres archivos y `./gradlew :app:kspDebugKotlin` sin errores |
| S20-A03 | `app/schemas/com.juancholopes.weightrack.data.WeightrackDatabase/1.json` existe y está versionado. | S20-R03, S20-R04 | `git ls-files app/schemas` |
| S20-A04 | No hay migraciones destructivas configuradas. | S20-R06 | `grep -rn "fallbackToDestructiveMigration" app/src` sin resultados |
| S20-A05 | Solo `AppContainer` construye la base de datos en `src/main`, con el nombre `weightrack.db`, y el manifiesto declara `WeightrackApplication`. | S20-R05, S20-R07 | `grep -rn "databaseBuilder" app/src/main` devuelve solo `AppContainer.kt`; `grep -n "android:name=\".WeightrackApplication\"" app/src/main/AndroidManifest.xml` |
| S20-A06 | `PRAGMA foreign_keys` devuelve 1. | S20-R19 | `WeightrackSchemaTest.foreignKeys_areEnabled` |
| S20-A07 | `PRAGMA table_info` de cada tabla coincide en nombres, tipos, `notnull` y `pk` con esta spec, y `sqlite_master` contiene `AUTOINCREMENT` en las tres tablas con id. | S20-R09, S20-R10, S20-R17, S20-R18 | `WeightrackSchemaTest.tables_matchSpec` |
| S20-A08 | `PRAGMA index_list` e `index_info` muestran los índices de la tabla de índices, con su unicidad. `PRAGMA foreign_key_list` muestra las tres claves con su `on_delete` y `on_update`. | S20-R20 a S20-R27 | `WeightrackSchemaTest.indexes_matchSpec`, `WeightrackSchemaTest.foreignKeys_matchSpec` |
| S20-A09 | Una base de datos recién creada no tiene tabla de músculos y sus cuatro tablas están vacías. | S20-R09 | `WeightrackSchemaTest.freshDatabase_hasNoMuscleTable_andNoRows` |
| S20-A10 | Los conversores devuelven `name` para cada valor de `Equipment` y `Muscle` y lo leen de vuelta; un nombre desconocido lanza excepción; `LocalDate.of(2026, 10, 10)` se convierte en `"2026-10-10"` y vuelve igual. | S20-R13, S20-R15 | `ConvertersTest.enums_roundTripByName`, `ConvertersTest.unknownEnumName_throws`, `ConvertersTest.date_roundTripsAsIsoText` |
| S20-A11 | En la base de datos, `equipment` y `muscle` se guardan como TEXT con el `name`, y `archived` e `is_primary` como INTEGER 0 o 1. | S20-R13, S20-R14 | `ExerciseDaoTest.storesEnumsAsNameText_andBooleansAsInteger` (consulta cruda con `typeof()`) |
| S20-A12 | `workout_session.date` se guarda como TEXT `"2026-10-10"` y vuelve como el mismo `LocalDate`. | S20-R15, S10-R13 | `SessionDaoTest.storesDateAsIsoText` |
| S20-A13 | Al crear tres sesiones, borrar la última y crear otra, cada id nuevo es mayor que todos los anteriores, incluido el borrado. | S20-R17, S10-R14 | `SessionDaoTest.sessionIds_growInCreationOrder_evenAfterDeletingLatest` |
| S20-A14 | Insertar una serie con un `session_id` o un `exercise_id` inexistente falla y no deja filas. | S20-R20, S20-R22, S10-R10 | `SessionDaoTest.insertSet_withMissingSessionOrExercise_fails` |
| S20-A15 | Eliminar una sesión elimina sus series y no cambia ningún ejercicio. | S20-R20, S10-R23 | `SessionDaoTest.deleteSession_cascadesToSets_andKeepsExercises` |
| S20-A16 | Borrar un ejercicio sin series borra sus `exercise_muscle`. | S20-R21, S20-R36, S10-R20 | `ExerciseDaoTest.deleteIfHasNoSets_withoutSets_deletesExerciseAndMuscles` |
| S20-A17 | Un `DELETE` directo de un ejercicio con series falla por `RESTRICT` y no cambia nada; `deleteIfHasNoSets` devuelve 0 para ese ejercicio. | S20-R22, S20-R36 | `ExerciseDaoTest.hardDelete_ofExerciseWithSets_isBlockedByRestrict`, `ExerciseDaoTest.deleteIfHasNoSets_withSets_returnsZero` |
| S20-A18 | Cambiar el `id` de una sesión o de un ejercicio referenciado falla por `onUpdate = RESTRICT`. | S20-R23 | `WeightrackSchemaTest.updatingReferencedId_isRestricted` |
| S20-A19 | Con "Press de banca" guardado, insertar una fila con el `normalized_name` de "press  DE banca" falla, también si el existente está archivado. "Prés de banca" se acepta. | S20-R24, S20-R12, S10-R05 | `ExerciseDaoTest.normalizedNameCollision_isRejected_evenWhenArchived`, `ExerciseDaoTest.accentedName_isDistinct` |
| S20-A20 | Crear por el repositorio un ejercicio que colisiona devuelve `Rejected(NAME_TAKEN_BY_ARCHIVED)` si el existente está archivado y `Rejected(NAME_TAKEN)` si no. | S20-R41, S10-R21 | `RoomExerciseRepositoryTest.create_withNameOfArchived_returnsNameTakenByArchived`, `RoomExerciseRepositoryTest.create_withNameOfActive_returnsNameTaken` |
| S20-A21 | Insertar el mismo músculo dos veces para un ejercicio falla. | S20-R25, S10-R08 | `ExerciseDaoTest.duplicateMuscle_isRejected` |
| S20-A22 | Dos series con el mismo `set_order` en una sesión fallan; el mismo `set_order` en otra sesión se acepta. | S20-R26, S10-R09 | `SessionDaoTest.duplicateSetOrder_inSameSession_isRejected` |
| S20-A23 | Series con 0 g, 62 500 g y 1 000 000 g se guardan y se leen con el mismo valor; 0 g no es `NULL`. | S20-R16, S20-R28, S10-R02 | `SessionDaoTest.weightGrams_roundTrip_includingZero` |
| S20-A24 | Con dos sesiones del mismo día que contienen el ejercicio, la última vez devuelve todas las series de la de mayor `id`, por `set_order`. | S20-R29, S10-R25, S10-R14 | `SessionDaoTest.lastPerformance_sameDaySessions_returnsHigherId` |
| S20-A25 | Desde una sesión en curso, la última vez ignora esa sesión y las posteriores (misma fecha con mayor id y fechas posteriores). Sin series del ejercicio, devuelve vacío. Las series de un ejercicio archivado se incluyen. | S20-R29, S10-R26, S10-R18 | `SessionDaoTest.lastPerformance_beforeSession_excludesInProgressAndLater`, `SessionDaoTest.lastPerformance_withoutSets_isEmpty`, `SessionDaoTest.lastPerformance_includesArchivedExercise` |
| S20-A26 | La evolución con series en tres sesiones, dos del mismo día, sale por fecha, id de sesión y `set_order`. | S20-R30, S10-R27 | `SessionDaoTest.evolution_ordersByDateSessionIdAndOrder` |
| S20-A27 | El historial ordena por fecha descendente y, en el mismo día, por id descendente. | S20-R31, S10-R28 | `SessionDaoTest.history_ordersByDateDescThenIdDesc` |
| S20-A28 | Las series de una sesión salen por `set_order`, con su ejercicio, incluidos los archivados, y con huecos en el orden. | S20-R32, S10-R29 | `SessionDaoTest.sessionSets_orderedWithExercise_includingArchived` |
| S20-A29 | El catálogo activo excluye archivados, ordena por nombre sin distinguir mayúsculas y filtra por músculo principal, por músculo secundario, por equipo y por ambos. Los archivados se listan aparte por nombre. | S20-R33, S20-R34, S10-R30, S10-R31 | `ExerciseDaoTest.activeCatalog_filtersAndOrders`, `ExerciseDaoTest.archived_listedByName` |
| S20-A30 | `setArchived` archiva y desarchiva sin tocar series ni músculos. | S20-R35, S10-R17, S10-R19 | `ExerciseDaoTest.setArchived_togglesOnlyArchived` |
| S20-A31 | Borrar por el repositorio un ejercicio con series devuelve `ExerciseDeletion.Archive` y lo deja archivado con sus series; uno sin series devuelve `ExerciseDeletion.Delete` y desaparece con sus músculos. | S20-R40, S10-R17, S10-R20 | `RoomExerciseRepositoryTest.delete_withSets_archives`, `RoomExerciseRepositoryTest.delete_withoutSets_deletesWithMuscles` |
| S20-A32 | Si falla la inserción de un músculo (músculo repetido), no queda el ejercicio. Si falla una serie de `insertSessionWithSets` (ejercicio inexistente), no queda la sesión ni ninguna serie. | S20-R38, S20-R39 | `ExerciseDaoTest.insertWithMuscles_whenMuscleFails_rollsBack`, `SessionDaoTest.insertSessionWithSets_whenOneSetFails_insertsNothing` |
| S20-A33 | Un `Flow` de lectura emite de nuevo tras una escritura en su tabla. | S20-R37 | `SessionDaoTest.observeHistory_emitsAfterInsert` |
| S20-A34 | Los mappers convierten cada entidad en su modelo y de vuelta sin perder datos (`weightGrams`, `date`, `notes` nulas, `archived`, `isPrimary`). | S20-R11, S20-R12, S20-R16 | `ExerciseMappersTest.exercise_roundTrip`, `ExerciseMappersTest.exerciseMuscle_roundTrip`, `SessionMappersTest.session_roundTrip`, `SessionMappersTest.exerciseSet_roundTrip` |
| S20-A35 | `normalized_name` sale de las funciones del dominio: "  Press   DE banca " da `"press de banca"`, también con `Locale.setDefault(Locale("tr"))` (la "I" no se convierte en "ı"). | S20-R12, S10-R05 | `ExerciseMappersTest.normalizedName_usesDomainNormalization`, `ExerciseMappersTest.normalizedName_ignoresDefaultLocale` |
| S20-A36 | La v1 se crea desde su JSON exportado y Room la abre sin errores de validación. | S20-R45 | `WeightrackMigrationTest.version1_createdFromExportedSchema_opensWithRoom` |
| S20-A37 | Los paquetes de dominio siguen sin importar Android, Room ni entidades tras agregar las interfaces. | S20-R11, S20-R42, S10-R16 | `grep -rnE "^import (android\|androidx)\." app/src/main/java/com/juancholopes/weightrack/feature/*/domain` sin resultados |
| S20-A38 | Cada repositorio implementa su interfaz y el `AppContainer` los expone con el tipo de la interfaz. | S20-R07, S20-R43 | Revisión de `AppContainer.kt` y compilación con `./gradlew assembleDebug` |
| S20-A39 | Esta spec incluye la tabla de cumplimiento de S01-R06 a S01-R09. | S01-A05 | Revisión de esta spec |
| S20-A40 | Build, pruebas unitarias y pruebas de base de datos pasan desde un clon limpio. | S03-R14, S20-R46, S20-R47 | `./gradlew assembleDebug test`, más `./gradlew connectedAndroidTest` en un dispositivo |

## Hechos de la implementación

Verificados al implementar el 2026-10-11:

- Room 3 activa `PRAGMA foreign_keys` al abrir (S20-A06 pasa sin callback).
- `withWriteTransaction` y las funciones `suspend` con `@Transaction` del DAO funcionan como pide S20-R38 a S20-R40.
- Las violaciones de restricción llegan como `androidx.sqlite.SQLiteException`, que S20-R41 captura.
- Room 3 usa `Dispatchers.IO` por defecto para las consultas; no se llama a `setQueryCoroutineContext`.
- `room3-runtime` trae `sqlite-framework` de forma transitiva, pero se declara explícitamente porque `AppContainer` usa `AndroidSQLiteDriver` directamente.
- `ExerciseRepository.delete` de un id que no existe lanza `IllegalStateException`.
- Además de las pruebas que nombran los criterios, `RoomSessionRepositoryTest` prueba los mapeos y las escrituras de `RoomSessionRepository` (S20-R43, S20-R46).

## Decisiones abiertas

Todas cerradas por el dueño el 2026-10-10.

- [x] **Dónde corren las pruebas de base de datos.** Decidido el 2026-10-10: opción A, pruebas instrumentadas en `src/androidTest`, en el teléfono o en un emulador. Los mappers y conversores siguen en `src/test`.
- [x] **Driver de producción.** Decidido el 2026-10-10: `AndroidSQLiteDriver` (`sqlite-framework`). Las pruebas usan el mismo driver.
- [x] **`Flow` en el dominio frente a S10-R16.** Decidido el 2026-10-10: el dominio puede usar `kotlinx.coroutines`, incluido `Flow`. S10-R16 queda enmendado.
- [x] **Interfaces de repositorio.** Decidido el 2026-10-10: `ExerciseRepository`, `SessionRepository` y `ExerciseWriteResult` se aprueban tal como se proponen.
- [x] **Id de un objeto no guardado.** Decidido el 2026-10-10: `id = 0` significa que el objeto todavía no se guardó. La Spec 10 registra la convención.
- [x] **Paquete de la base de datos y del contenedor.** Decidido el 2026-10-10: `WeightrackDatabase` y `Converters` en `com.juancholopes.weightrack.data`, y `AppContainer` y `WeightrackApplication` en el paquete raíz. La Spec 03 lo agrega a su esqueleto.

## Cambios que esta spec pide a otras specs

Esta spec no edita ninguna. El dueño aprueba y aplica cada cambio. Las filas de las Specs 00, 03 y 10 se aplicaron el 2026-10-10 con la aprobación del dueño. La de la Spec 30 se aplicó el 2026-10-11, al aprobarse esa spec. La de la Spec 31 queda pendiente hasta que esa spec exista.

| Spec | Cambio propuesto | Motivo |
| --- | --- | --- |
| 00 Constitución | En la tabla de decisiones técnicas, "Inyección de dependencias" pasa a "Manual con un `AppContainer` creado en una `Application` propia. Decidido el 2026-10-10", y se marca la casilla en la lista de decisiones. Anotar la enmienda con fecha. | Decisión del dueño del 2026-10-10. Hoy la constitución dice que se resuelve con un ADR en la Spec 30. |
| 00 Constitución | Valorar pasar "Asincronía: Coroutines y Flow" de Propuesto a Decidido. | Room 3 exige coroutines y esta spec usa `Flow` y `suspend`. |
| 03 Configuración | Ajustar S03-R06 y S03-A04: KSP se aplica en `:app` desde la Spec 20, y el plugin `androidx.room3` se declara en la raíz con `apply false`. | Hoy S03-A04 exige que KSP no aparezca en `app/build.gradle.kts`, lo que fallaría tras implementar esta spec. |
| 03 Configuración | Agregar al esqueleto de paquetes `data/` en la raíz (base de datos y conversores compartidos) y la ubicación de `AppContainer` y `WeightrackApplication`. | La base de datos es de todas las features y no cabe en `feature/<feature>/data`. |
| 10 Modelo de dominio | Aclarar S10-R16 sobre `kotlinx.coroutines` (ver "Decisiones abiertas"). | Las interfaces de repositorio en el dominio devuelven `Flow`. |
| 10 Modelo de dominio | Registrar la convención `id = 0` para objetos no guardados y que `normalized_name` de la Spec 20 depende de `normalizeExerciseName` y `nameKey`. Si la Spec 30 separa módulos, `nameKey` (hoy `internal`) tendría que ser público. | Mantener una sola normalización (S20-R12). |
| 30 Arquitectura | Partir de las interfaces de S20-R42 o reemplazarlas, y registrar la decisión de inyección manual. Aplicado el 2026-10-11: las interfaces se mantienen (ADR-30-05) y la inyección manual queda registrada (ADR-30-04). | La Spec 10 le dejó las interfaces a la Spec 30. |
| 31 Estrategia de pruebas | Recoger la decisión de dónde corren las pruebas de base de datos. | Afecta al comando único de S03-R14. |

## Fuentes

Consultadas el 2026-10-10:

- [Room 3 releases](https://developer.android.com/jetpack/androidx/releases/room3): 3.0.3 estable del 9 de septiembre de 2026, coordenadas `room3-runtime` y `room3-compiler`, plugin `androidx.room3` con `schemaDirectory` obligatorio, DAOs `suspend` o reactivos, `SQLiteDriver` obligatorio, `@ColumnTypeConverter`.
- [Migrate from Room 2.x to Room 3.0](https://developer.android.com/training/data-storage/room/migration-2-to-3): paquete `androidx.room3`, `setDriver` como último paso, `useWriterConnection` e `immediateTransaction`, `withWriteTransaction`, `Migration.migrate` `suspend`.
- [Migrate your Room database](https://developer.android.com/training/data-storage/room/migrating-db-versions): `room3 { schemaDirectory(...) }`, `room3-testing` 3.0.3, `MigrationTestHelper` instrumentado, advertencia sobre `fallbackToDestructiveMigration`.
- [Test and debug your database](https://developer.android.com/training/data-storage/room/testing-db): pruebas en dispositivo o JVM con Room KMP, `BundledSQLiteDriver`, `inMemoryDatabaseBuilder` sin `Context` solo en JVM de escritorio, Robolectric desaconsejado.
- [SQLite releases](https://developer.android.com/jetpack/androidx/releases/sqlite): `androidx.sqlite` 2.7.1 del 9 de septiembre de 2026.
- [kotlinx.coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases): 1.11.0.
- [SQLite Autoincrement](https://www.sqlite.org/autoinc.html): con `AUTOINCREMENT`, el id nuevo es mayor que cualquiera que haya existido en la tabla.
