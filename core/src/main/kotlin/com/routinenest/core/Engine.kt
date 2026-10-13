package com.routinenest.core

import java.time.*
import java.util.Locale

data class Schedule(val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(), val start: LocalDate = LocalDate.now()) {
    fun due(date: LocalDate) = !date.isBefore(start) && date.dayOfWeek in days
}

object Progress {
    fun localDay(instant: Instant, zone: ZoneId): LocalDate = instant.atZone(zone).toLocalDate()
    fun streak(schedule: Schedule, completed: Set<LocalDate>, today: LocalDate): Int {
        return streak(schedule.start, schedule::due, completed, today)
    }
    fun streak(start: LocalDate, due: (LocalDate) -> Boolean, completed: Set<LocalDate>, today: LocalDate): Int {
        var cursor = today
        var count = 0
        // An unfinished current day does not break yesterday's streak.
        if (due(cursor) && cursor !in completed) cursor = cursor.minusDays(1)
        while (!cursor.isBefore(start)) {
            if (due(cursor)) {
                if (cursor !in completed) break
                count++
            }
            cursor = cursor.minusDays(1)
        }
        return count
    }
    fun percentage(done: Int, total: Int) = if (total == 0) 0 else (100.0 * done / total).toInt().coerceIn(0, 100)
    fun nextReminder(time: LocalTime, schedule: Schedule, now: ZonedDateTime): ZonedDateTime? {
        if (schedule.days.isEmpty()) return null
        return (0..14).asSequence().map { now.toLocalDate().plusDays(it.toLong()) }
            .filter { schedule.due(it) }.map { it.atTime(time).atZone(now.zone) }
            .firstOrNull { it.isAfter(now) }
    }
}

sealed interface Command {
    data class Create(val name: String) : Command
    data class Add(val task: String, val routine: String) : Command
    data class Complete(val task: String) : Command
    data class Remind(val time: LocalTime, val tomorrow: Boolean) : Command
    data object Remaining : Command
    data object WeeklyProgress : Command
    data object Read : Command
    data class Unknown(val explanation: String) : Command
}

object CommandParser {
    val supportedLanguages = setOf("en")
    fun parse(raw: String, locale: Locale = Locale.UK): Command {
        if (locale.language !in supportedLanguages) return Command.Unknown("Commands currently support English. Switch the assistant language to English.")
        val text = raw.trim().trimEnd('.', '?', '!').lowercase(locale)
        Regex("create (?:a |an |my )?(.+?)(?: routine)?$").matchEntire(text)?.let {
            return Command.Create(it.groupValues[1].replaceFirstChar(Char::titlecase))
        }
        Regex("add (.+?) to (?:my |the )?(.+?)(?: routine)?$").matchEntire(text)?.let { return Command.Add(it.groupValues[1], it.groupValues[2]) }
        Regex("mark (.+?) (?:as )?(?:complete|done)$").matchEntire(text)?.let { return Command.Complete(it.groupValues[1]) }
        if (text in setOf("what is left on my list", "what's left on my list", "what is left", "show my checklist")) return Command.Remaining
        if (text in setOf("show my weekly progress", "weekly progress")) return Command.WeeklyProgress
        if (text in setOf("read my daily routine", "read my routine")) return Command.Read
        Regex("remind me at (.+?)( tomorrow)?$").matchEntire(text)?.let {
            val words = mapOf("one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12)
            val value = it.groupValues[1]
            words[value]?.let { hour -> return Command.Remind(LocalTime.of(hour, 0), it.groupValues[2].isNotBlank()) }
            Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?").matchEntire(value)?.let { match ->
                var h = match.groupValues[1].toInt()
                val m = match.groupValues[2].toIntOrNull() ?: 0
                val suffix = match.groupValues[3]
                if (suffix.isNotEmpty() && h !in 1..12) return Command.Unknown("Use a time such as 08:00 or 8 pm.")
                if (suffix.isNotEmpty()) h = h % 12 + if (suffix == "pm") 12 else 0
                if (h in 0..23 && m in 0..59) return Command.Remind(LocalTime.of(h, m), it.groupValues[2].isNotBlank())
            }
            return Command.Unknown("Use a time such as 08:00 or 8 pm.")
        }
        return Command.Unknown("I could not understand that command. Try one of the examples.")
    }
}

enum class Feature { MORE_ROUTINES, DUPLICATION, MONTHLY_INSIGHTS }
data class Entitlement(val userId: String, val receiptId: String, val verified: Boolean, val revoked: Boolean = false, val expiresAt: Long = Long.MAX_VALUE) {
    fun permits(feature: Feature, nowMillis: Long = System.currentTimeMillis()): Boolean = verified && !revoked && userId.isNotBlank() && receiptId.isNotBlank() && expiresAt > nowMillis
}
