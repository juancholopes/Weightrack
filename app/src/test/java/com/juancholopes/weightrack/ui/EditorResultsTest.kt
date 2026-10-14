package com.juancholopes.weightrack.ui

import androidx.navigation3.runtime.NavKey
import com.juancholopes.weightrack.feature.catalog.ui.ArchivedExercisesKey
import com.juancholopes.weightrack.feature.catalog.ui.ExerciseEditorKey
import com.juancholopes.weightrack.feature.catalog.ui.editor.ArchivedResult
import com.juancholopes.weightrack.feature.session.ui.HomeKey
import com.juancholopes.weightrack.feature.session.ui.SetLoggerKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/** A destination that sits between the home and the editor in some tests. */
@Serializable
private data object OtherKey : NavKey

/**
 * The rules that turn a result of the exercise editor into an operation on the stack (S42-R08, S42-R09).
 * They are pure functions over a list, so they run on the JVM and write no data (S31-R18).
 */
class EditorResultsTest {

    // SF01-A14 / SF01-R07, S42-R08
    @Test
    fun applyEditorSaved_fromHomeAndEditor_leavesOnlyHome() {
        val stack = mutableListOf<NavKey>(HomeKey, ExerciseEditorKey())

        stack.applyEditorSaved(exerciseId = 7L)

        assertEquals(listOf<NavKey>(HomeKey), stack)
    }

    // SF01-A14 / SF01-R07, S42-R08
    @Test
    fun applyEditorSaved_whenEditorIsNotOnTop_leavesTheStackAlone() {
        val stack = mutableListOf<NavKey>(HomeKey)

        stack.applyEditorSaved(exerciseId = 7L)

        assertEquals(listOf<NavKey>(HomeKey), stack)
    }

    // SF01-A14 / SF01-R07, S42-R08
    @Test
    fun applyEditorSaved_withAnotherEntryBelowTheEditor_removesOnlyTheEditor() {
        val stack = mutableListOf<NavKey>(HomeKey, OtherKey, ExerciseEditorKey(exerciseId = 7L))

        stack.applyEditorSaved(exerciseId = 7L)

        assertEquals(listOf<NavKey>(HomeKey, OtherKey), stack)
    }

    // SF02-A18 / SF02-R02, S42-R08 (D5): saving with `logAfterSave` replaces the editor with the logger of the new exercise
    @Test
    fun applyEditorSaved_withLogAfterSave_opensTheLoggerOfTheExercise() {
        val stack = mutableListOf<NavKey>(HomeKey, ExerciseEditorKey(logAfterSave = true))

        stack.applyEditorSaved(exerciseId = 42L)

        assertEquals(listOf<NavKey>(HomeKey, SetLoggerKey(exerciseId = 42L)), stack)
    }

    // SF02-A18 / SF02-R02, S42-R08: without `logAfterSave` it only removes the editor
    @Test
    fun applyEditorSaved_withoutLogAfterSave_leavesOnlyHome() {
        val stack = mutableListOf<NavKey>(HomeKey, ExerciseEditorKey())

        stack.applyEditorSaved(exerciseId = 42L)

        assertEquals(listOf<NavKey>(HomeKey), stack)
    }

    // SF02-A18 / S42-R08: the editor opened from the logger only removes itself, and the logger stays below it
    @Test
    fun applyEditorSaved_fromTheLogger_returnsToTheLogger() {
        val stack = mutableListOf<NavKey>(HomeKey, SetLoggerKey(exerciseId = 5L), ExerciseEditorKey(exerciseId = 5L))

        stack.applyEditorSaved(exerciseId = 5L)

        assertEquals(listOf<NavKey>(HomeKey, SetLoggerKey(exerciseId = 5L)), stack)
    }

    // SF02-A18 / SF02-R02, S42-R09: archiving from the editor above the logger removes both and keeps the home
    @Test
    fun applyEditorArchived_fromTheLogger_leavesOnlyHome() {
        val stack = mutableListOf<NavKey>(HomeKey, SetLoggerKey(exerciseId = 5L), ExerciseEditorKey(exerciseId = 5L))

        val notice = stack.applyEditorArchived(ArchivedResult(5L, "Press de banca", wasDeleteRequest = false))

        assertEquals(listOf<NavKey>(HomeKey), stack)
        assertEquals(PendingNotice.Archived(5L, "Press de banca", wasDeleteRequest = false), notice)
    }

    // SF02-A18 / SF02-R02, S42-R09: deleting from the editor above the logger removes both and keeps the home
    @Test
    fun applyEditorDeleted_fromTheLogger_leavesOnlyHome() {
        val stack = mutableListOf<NavKey>(HomeKey, SetLoggerKey(exerciseId = 5L), ExerciseEditorKey(exerciseId = 5L))

        stack.applyEditorDeleted("Press de banca")

        assertEquals(listOf<NavKey>(HomeKey), stack)
    }

    // SF02-A18 / S42-R11: a second tap on a card while the logger is on top must not open it twice
    @Test
    fun pushWhenTopIs_withTheSourceOnTop_addsTheKey_andOnlyOnce() {
        val stack = mutableListOf<NavKey>(HomeKey)

        stack.pushWhenTopIs<HomeKey>(SetLoggerKey(exerciseId = 3L))
        stack.pushWhenTopIs<HomeKey>(SetLoggerKey(exerciseId = 3L))

        assertEquals(listOf<NavKey>(HomeKey, SetLoggerKey(exerciseId = 3L)), stack)
    }

    // SF02-A18 / S42-R07: the editor opens from the logger only while the logger is on top
    @Test
    fun pushWhenTopIs_fromTheLogger_opensTheEditorOnTopOfIt() {
        val stack = mutableListOf<NavKey>(HomeKey, SetLoggerKey(exerciseId = 3L))

        stack.pushWhenTopIs<SetLoggerKey>(ExerciseEditorKey(exerciseId = 3L))

        assertEquals(listOf<NavKey>(HomeKey, SetLoggerKey(exerciseId = 3L), ExerciseEditorKey(exerciseId = 3L)), stack)
    }

    // SF01-A14 / SF01-R08, S42-R06 (closing without saving writes nothing and removes only the editor)
    @Test
    fun applyEditorClosed_fromHomeAndEditor_leavesOnlyHome() {
        val stack = mutableListOf<NavKey>(HomeKey, ExerciseEditorKey(prefillName = "Press"))

        stack.applyEditorClosed()

        assertEquals(listOf<NavKey>(HomeKey), stack)
    }

    // SF01-A14 / SF01-R08, S42-R06 (a second tap on "close" must never remove the root)
    @Test
    fun applyEditorClosed_whenEditorIsNotOnTop_leavesTheStackAlone() {
        val stack = mutableListOf<NavKey>(HomeKey)

        stack.applyEditorClosed()

        assertEquals(listOf<NavKey>(HomeKey), stack)
    }

    // SF01-A14 / SF01-R07, S42-R09
    @Test
    fun applyEditorArchived_fromHomeAndEditor_leavesHomeAndReturnsTheNotice() {
        val stack = mutableListOf<NavKey>(HomeKey, ExerciseEditorKey(exerciseId = 5L))

        val notice = stack.applyEditorArchived(ArchivedResult(5L, "Press de banca", wasDeleteRequest = false))

        assertEquals(listOf<NavKey>(HomeKey), stack)
        assertEquals(PendingNotice.Archived(5L, "Press de banca", wasDeleteRequest = false), notice)
    }

    // SF01-A14 / SF01-R07, S42-R09, S40-F06 ("tiene series, así que se archivó")
    @Test
    fun applyEditorArchived_afterADeleteRequest_carriesTheVariantInTheNotice() {
        val stack = mutableListOf<NavKey>(HomeKey, ExerciseEditorKey(exerciseId = 5L))

        val notice = stack.applyEditorArchived(ArchivedResult(5L, "Press de banca", wasDeleteRequest = true))

        assertEquals(listOf<NavKey>(HomeKey), stack)
        assertEquals(PendingNotice.Archived(5L, "Press de banca", wasDeleteRequest = true), notice)
    }

    // SF01-A14 / SF01-R07, S42-R09 (D6: the editor and whatever sits above the root leave the stack)
    @Test
    fun applyEditorArchived_withEntriesAboveTheRoot_leavesOnlyTheRoot() {
        val stack = mutableListOf<NavKey>(HomeKey, OtherKey, ExerciseEditorKey(exerciseId = 5L))

        stack.applyEditorArchived(ArchivedResult(5L, "Press de banca", wasDeleteRequest = false))

        assertEquals(listOf<NavKey>(HomeKey), stack)
    }

    // SF01-A14 / SF01-R07, S42-R09
    @Test
    fun applyEditorArchived_whenNothingSitsAboveTheRoot_keepsTheRootAndStillReturnsTheNotice() {
        val stack = mutableListOf<NavKey>(HomeKey)

        val notice = stack.applyEditorArchived(ArchivedResult(5L, "Press de banca", wasDeleteRequest = false))

        assertEquals(listOf<NavKey>(HomeKey), stack)
        assertEquals(PendingNotice.Archived(5L, "Press de banca", wasDeleteRequest = false), notice)
    }

    // SF01-A14 / SF01-R07, S42-R09
    @Test
    fun applyEditorDeleted_fromHomeAndEditor_leavesHomeAndReturnsTheNotice() {
        val stack = mutableListOf<NavKey>(HomeKey, ExerciseEditorKey(exerciseId = 5L))

        val notice = stack.applyEditorDeleted("Press de banca")

        assertEquals(listOf<NavKey>(HomeKey), stack)
        assertEquals(PendingNotice.Deleted("Press de banca"), notice)
    }

    // SF01-A14 / SF01-R07, S42-R09
    @Test
    fun applyEditorDeleted_withEntriesAboveTheRoot_leavesOnlyTheRoot() {
        val stack = mutableListOf<NavKey>(HomeKey, OtherKey, ExerciseEditorKey(exerciseId = 5L))

        stack.applyEditorDeleted("Press de banca")

        assertEquals(listOf<NavKey>(HomeKey), stack)
    }

    // SF01-A14 / SF01-R10 (the archived list goes back the same way, and never removes the root)
    @Test
    fun popTop_withTheArchivedListOnTop_removesIt() {
        val stack = mutableListOf<NavKey>(HomeKey, ArchivedExercisesKey)

        stack.popTop<ArchivedExercisesKey>()

        assertEquals(listOf<NavKey>(HomeKey), stack)
    }

    // SF01-A14 / SF01-R10
    @Test
    fun popTop_withAnotherEntryOnTop_leavesTheStackAlone() {
        val stack = mutableListOf<NavKey>(HomeKey)

        stack.popTop<ArchivedExercisesKey>()

        assertEquals(listOf<NavKey>(HomeKey), stack)
    }

    // SF01-A14 / S42-R09: the notice lives in `rememberSaveable`, so it must survive being saved to a Bundle
    // (a Bundle stores a `Serializable` as it is) and a process death.
    @Test
    fun pendingNotice_survivesBeingSerialized() {
        val notices = listOf(
            PendingNotice.Archived(exerciseId = 5L, name = "Press de banca", wasDeleteRequest = true),
            PendingNotice.Deleted(name = "Remo de peña"),
        )

        notices.forEach { notice ->
            val restored = roundTrip(notice)

            assertNotNull(restored)
            assertEquals(notice, restored)
        }
    }

    private fun roundTrip(value: Any): Any? {
        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).use { out -> out.writeObject(value) } }
        return ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
    }
}
