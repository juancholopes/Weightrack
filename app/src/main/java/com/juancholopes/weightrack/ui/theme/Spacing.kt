package com.juancholopes.weightrack.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale, read with `WeightrackTheme.spacing` (S41-R09). Each direction has its own scale.
 *
 * [l] is the base padding of a screen and its cards; [xl] separates sections. Values are tunable
 * (S41-R19): change them here and every screen follows.
 */
@Immutable
data class Spacing(
    val xxs: Dp,
    val xs: Dp,
    val s: Dp,
    val m: Dp,
    val l: Dp,
    val xl: Dp,
    val xxl: Dp,
    val xxxl: Dp,
) {
    companion object {
        /** A. Expressive: ample. Base 16 dp, 24 dp between sections. */
        val Expressive = Spacing(
            xxs = 2.dp, xs = 4.dp, s = 8.dp, m = 12.dp, l = 16.dp, xl = 24.dp, xxl = 32.dp, xxxl = 48.dp,
        )

        /** B. Scoreboard: dense. Base 8 dp, 16 dp between sections. */
        val Scoreboard = Spacing(
            xxs = 1.dp, xs = 2.dp, s = 4.dp, m = 6.dp, l = 8.dp, xl = 16.dp, xxl = 24.dp, xxxl = 32.dp,
        )

        /** C. Notebook: very ample. Base 20 dp, 32 dp between sections. */
        val Notebook = Spacing(
            xxs = 4.dp, xs = 8.dp, s = 12.dp, m = 16.dp, l = 20.dp, xl = 32.dp, xxl = 40.dp, xxxl = 56.dp,
        )
    }
}
