package com.juancholopes.weightrack.feature.session.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The home screen, S42-P01: the root of the stack (SF02-R02, S42-R02). Its two tabs are state of the screen, not entries. */
@Serializable
data object HomeKey : NavKey

/**
 * Opens the set logger, S42-P02 (S42-R11, S30-R38). A key carries only ids and simple values.
 *
 * @property exerciseId the exercise whose sets are logged.
 * @property sessionId `0L` when opened from a card: the logger then uses the session of today and the first set
 * creates it if there is none. Any other id is a past session (F04) and the logger never creates another.
 */
@Serializable
data class SetLoggerKey(val exerciseId: Long, val sessionId: Long = 0L) : NavKey

/** Opens the form of the data of a session, S42-P05: its date and its note. */
@Serializable
data class SessionEditorKey(val sessionId: Long) : NavKey
