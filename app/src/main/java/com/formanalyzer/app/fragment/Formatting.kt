package com.formanalyzer.app.fragment

import java.util.Locale
import java.text.DateFormat
import java.util.Date
/** Formats a duration as minutes and seconds, e.g. 1:05. */
fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}

/** Date and time in the phone locale*/
fun formatDateTime(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMs))