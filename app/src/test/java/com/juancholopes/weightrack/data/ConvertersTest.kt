package com.juancholopes.weightrack.data

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class ConvertersTest {

    private val converters = Converters()

    // S20-A10 / S20-R13
    @Test
    fun enums_roundTripByName() {
        Equipment.entries.forEach { equipment ->
            val stored = converters.equipmentToName(equipment)
            assertEquals(equipment.name, stored)
            assertEquals(equipment, converters.nameToEquipment(stored))
        }
        Muscle.entries.forEach { muscle ->
            val stored = converters.muscleToName(muscle)
            assertEquals(muscle.name, stored)
            assertEquals(muscle, converters.nameToMuscle(stored))
        }
    }

    // S20-A10 / S20-R13: an unknown value is never replaced by a default
    @Test
    fun unknownEnumName_throws() {
        assertThrows(IllegalArgumentException::class.java) { converters.nameToEquipment("KETTLEBELL") }
        assertThrows(IllegalArgumentException::class.java) { converters.nameToMuscle("NECK") }
        assertThrows(IllegalArgumentException::class.java) { converters.nameToMuscle("chest") }
    }

    // S20-A10 / S20-R15
    @Test
    fun date_roundTripsAsIsoText() {
        val date = LocalDate.of(2026, 10, 10)
        val stored = converters.dateToIso(date)
        assertEquals("2026-10-10", stored)
        assertEquals(date, converters.isoToDate(stored))
    }

    @Test
    fun nulls_mapToNulls_forOptionalQueryParameters() {
        assertNull(converters.equipmentToName(null))
        assertNull(converters.nameToEquipment(null))
        assertNull(converters.muscleToName(null))
        assertNull(converters.nameToMuscle(null))
        assertNull(converters.dateToIso(null))
        assertNull(converters.isoToDate(null))
    }
}
