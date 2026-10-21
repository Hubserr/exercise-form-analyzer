package com.formanalyzer.app.fragment

import java.util.Locale

/** Formats a duration as minutes and seconds, e.g. 1:05. */
fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}