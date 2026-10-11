package com.juancholopes.weightrack.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 shapes of each direction, read with `MaterialTheme.shapes` (S41-R09).
 *
 * Cards use `extraLarge`. Material 3 buttons are already pills (`ButtonDefaults.shape`), so they need
 * no token. Corner radii are tunable (S41-R19).
 */
object WeightrackShapes {
    /** A. Expressive: very rounded, 32 dp cards. */
    val Expressive: Shapes = roundedShapes(extraSmall = 8, small = 12, medium = 16, large = 24, extraLarge = 32)

    /** B. Scoreboard: medium corners, 12 dp cards. */
    val Scoreboard: Shapes = roundedShapes(extraSmall = 4, small = 6, medium = 8, large = 10, extraLarge = 12)

    /** C. Notebook: 20 dp cards. */
    val Notebook: Shapes = roundedShapes(extraSmall = 6, small = 10, medium = 14, large = 18, extraLarge = 20)

    private fun roundedShapes(extraSmall: Int, small: Int, medium: Int, large: Int, extraLarge: Int) = Shapes(
        extraSmall = RoundedCornerShape(extraSmall.dp),
        small = RoundedCornerShape(small.dp),
        medium = RoundedCornerShape(medium.dp),
        large = RoundedCornerShape(large.dp),
        extraLarge = RoundedCornerShape(extraLarge.dp),
    )
}
