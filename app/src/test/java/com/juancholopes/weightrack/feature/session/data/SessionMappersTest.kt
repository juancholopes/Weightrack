package com.juancholopes.weightrack.feature.session.data

import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SessionMappersTest {

    // S20-A34
    @Test
    fun session_roundTrip() {
        listOf(
            WorkoutSession(id = 4, date = LocalDate.of(2026, 10, 10), notes = "Me sentí cansado"),
            WorkoutSession(id = 5, date = LocalDate.of(2026, 1, 2), notes = null),
        ).forEach { session ->
            val entity = session.toEntity()
            assertEquals(session.id, entity.id)
            assertEquals(session.date, entity.date)
            assertEquals(session.notes, entity.notes)
            assertEquals(session, entity.toDomain())
        }
    }

    // S20-A34 / S20-R16: grams are kept as the same integer
    @Test
    fun exerciseSet_roundTrip() {
        listOf(0, 62_500, 1_000_000).forEach { grams ->
            val set = ExerciseSet(id = 9, sessionId = 4, exerciseId = 3, order = 2, weightGrams = grams, reps = 10)
            val entity = set.toEntity()
            assertEquals(set.id, entity.id)
            assertEquals(set.sessionId, entity.sessionId)
            assertEquals(set.exerciseId, entity.exerciseId)
            assertEquals(set.order, entity.order)
            assertEquals(grams, entity.weightGrams)
            assertEquals(set.reps, entity.reps)
            assertEquals(set, entity.toDomain())
        }
    }
}
