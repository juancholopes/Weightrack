package com.juancholopes.weightrack

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.juancholopes.weightrack.data.WeightrackDatabase
import com.juancholopes.weightrack.feature.catalog.data.RoomExerciseRepository
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseRepository
import com.juancholopes.weightrack.feature.session.data.RoomSessionRepository
import com.juancholopes.weightrack.feature.session.domain.AddSetUseCase
import com.juancholopes.weightrack.feature.session.domain.CorrectSetUseCase
import com.juancholopes.weightrack.feature.session.domain.CreateSessionUseCase
import com.juancholopes.weightrack.feature.session.domain.SessionRepository
import com.juancholopes.weightrack.feature.session.domain.UpdateSessionUseCase
import java.time.Clock

/**
 * Manual dependency injection (S20-R07). The only place that builds the database: lazily, once,
 * without destructive migrations (S20-R06), with the driver set as the last builder step (S20-R05).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: WeightrackDatabase by lazy {
        Room.databaseBuilder(appContext, WeightrackDatabase::class.java, WeightrackDatabase.NAME)
            .setDriver(AndroidSQLiteDriver())
            .build()
    }

    val exerciseRepository: ExerciseRepository by lazy {
        RoomExerciseRepository(database, database.exerciseDao())
    }

    val sessionRepository: SessionRepository by lazy {
        RoomSessionRepository(database.sessionDao())
    }

    /** The only clock of the app (ADR-30-09, S30-R36): every "today" comes from `LocalDate.now(clock)`. */
    val clock: Clock = Clock.systemDefaultZone()

    // Validated session writes (SF02-R04, S30-R20): no ViewModel writes sessions or sets directly.
    val createSessionUseCase: CreateSessionUseCase by lazy {
        CreateSessionUseCase(sessionRepository, clock)
    }

    val updateSessionUseCase: UpdateSessionUseCase by lazy {
        UpdateSessionUseCase(sessionRepository, clock)
    }

    val addSetUseCase: AddSetUseCase by lazy {
        AddSetUseCase(exerciseRepository, sessionRepository, clock)
    }

    val correctSetUseCase: CorrectSetUseCase by lazy {
        CorrectSetUseCase(exerciseRepository, sessionRepository)
    }
}
