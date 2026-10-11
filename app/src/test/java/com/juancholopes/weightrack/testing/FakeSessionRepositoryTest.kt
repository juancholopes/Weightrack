package com.juancholopes.weightrack.testing

/** S31-A02 / S31-R06, S31-R07: the fake honors the same contract as Room. */
class FakeSessionRepositoryTest : SessionRepositoryContract() {

    override fun createRepositories(): Repositories {
        val store = InMemoryStore()
        return Repositories(FakeExerciseRepository(store), FakeSessionRepository(store))
    }
}
