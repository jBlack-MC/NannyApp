package com.nannyapp.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val apiDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]")
private val displayDateTime = DateTimeFormatter.ofPattern("EEE, d MMM yyyy · HH:mm", Locale.ENGLISH)

/** Turns the API's "2026-09-17 09:00:00" into "Thu, 17 Sep 2026 · 09:00"; unknown shapes pass through. */
fun formatDateTime(raw: String): String =
    runCatching { LocalDateTime.parse(raw.trim().replace('T', ' '), apiDateTime).format(displayDateTime) }.getOrDefault(raw)

/** 8.0 becomes "8", 2.5 stays "2.5". */
fun formatHours(hours: Double): String = if (hours % 1.0 == 0.0) hours.toInt().toString() else hours.toString()
