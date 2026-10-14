package com.juancholopes.weightrack.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import com.juancholopes.weightrack.R

/**
 * The [DateNames] of the current language, read from the six `format_*` resources of `strings.xml` (S41-R14,
 * S42-R26). `Formats.kt` stays pure and tested on the JVM: this is the only place that reads the resources.
 */
@Composable
fun rememberDateNames(): DateNames {
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current
    return remember(resources, configuration) {
        DateNames(
            weekdaysShort = resources.getStringArray(R.array.format_weekdays_short).toList(),
            weekdaysLong = resources.getStringArray(R.array.format_weekdays_long).toList(),
            monthsShort = resources.getStringArray(R.array.format_months_short).toList(),
            monthsLong = resources.getStringArray(R.array.format_months_long).toList(),
            today = resources.getString(R.string.format_today),
            yesterday = resources.getString(R.string.format_yesterday),
        )
    }
}
