package com.juancholopes.weightrack.data

import androidx.room3.ColumnTypeConverter
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import java.time.LocalDate

/**
 * Column converters (S20-R13, S20-R15). Enums are stored by `name`, never by `ordinal`, and an unknown
 * name throws instead of falling back to a default. Dates are ISO-8601 `yyyy-MM-dd` text. Nullable
 * types let the same converters bind optional query parameters.
 */
class Converters {
    @ColumnTypeConverter
    fun equipmentToName(equipment: Equipment?): String? = equipment?.name

    @ColumnTypeConverter
    fun nameToEquipment(name: String?): Equipment? = name?.let { Equipment.valueOf(it) }

    @ColumnTypeConverter
    fun muscleToName(muscle: Muscle?): String? = muscle?.name

    @ColumnTypeConverter
    fun nameToMuscle(name: String?): Muscle? = name?.let { Muscle.valueOf(it) }

    @ColumnTypeConverter
    fun dateToIso(date: LocalDate?): String? = date?.toString()

    @ColumnTypeConverter
    fun isoToDate(iso: String?): LocalDate? = iso?.let { LocalDate.parse(it) }
}
