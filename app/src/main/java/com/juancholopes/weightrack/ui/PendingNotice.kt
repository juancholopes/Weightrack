package com.juancholopes.weightrack.ui

import java.io.Serializable

/**
 * A notice born on one screen and shown on another (S42-R09, D6): archiving or deleting from the editor
 * returns to the home with it. The shell keeps it as saveable state, never as an event (S30-R28), so it
 * survives the death of the process. It is [Serializable] because `rememberSaveable` stores it in a Bundle.
 */
sealed interface PendingNotice : Serializable {
    data class Archived(val exerciseId: Long, val name: String, val wasDeleteRequest: Boolean) : PendingNotice
    data class Deleted(val name: String) : PendingNotice
}
