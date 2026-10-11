package com.juancholopes.weightrack.feature.catalog.data

import com.juancholopes.weightrack.data.WeightrackDatabase
import com.juancholopes.weightrack.data.newInMemoryDatabase
import com.juancholopes.weightrack.feature.session.data.RoomSessionRepository
import com.juancholopes.weightrack.testing.ExerciseRepositoryContract
import com.juancholopes.weightrack.testing.Repositories
import org.junit.After

/** S31-A03 / S31-R06, S31-R07: Room honors the contract the fakes are tested against. */
class RoomExerciseRepositoryContractTest : ExerciseRepositoryContract() {

    private lateinit var db: WeightrackDatabase

    override fun createRepositories(): Repositories {
        db = newInMemoryDatabase()
        return Repositories(RoomExerciseRepository(db, db.exerciseDao()), RoomSessionRepository(db.sessionDao()))
    }

    @After
    fun closeDatabase() {
        db.close()
    }
}
