package com.routinenest.core

import org.junit.Assert.*
import org.junit.Test
import java.time.*
import java.util.Locale

class EngineTest {
    private val monday = LocalDate.of(2026, 10, 12)
    @Test fun dailyRecurrenceRespectsStart() { val s = Schedule(start = monday); assertTrue(s.due(monday)); assertFalse(s.due(monday.minusDays(1))) }
    @Test fun weeklyRecurrence() { val s = Schedule(setOf(DayOfWeek.MONDAY), monday); assertTrue(s.due(monday.plusWeeks(1))); assertFalse(s.due(monday.plusDays(1))) }
    @Test fun weeklyStreakSkipsOffDays() { assertEquals(2, Progress.streak(Schedule(setOf(DayOfWeek.MONDAY), monday), setOf(monday, monday.plusWeeks(1)), monday.plusWeeks(1))) }
    @Test fun unfinishedTodayPreservesYesterday() { assertEquals(1, Progress.streak(Schedule(start = monday), setOf(monday), monday.plusDays(1))) }
    @Test fun missedScheduledDayBreaksStreak() { assertEquals(0, Progress.streak(Schedule(start = monday), setOf(monday), monday.plusDays(2))) }
    @Test fun completedTodayCounts() { assertEquals(2, Progress.streak(Schedule(start = monday), setOf(monday, monday.plusDays(1)), monday.plusDays(1))) }
    @Test fun noDivisionByZero() { assertEquals(0, Progress.percentage(0, 0)); assertEquals(50, Progress.percentage(1, 2)) }
    @Test fun timezoneChangesLocalDay() { val instant = Instant.parse("2026-10-12T23:30:00Z"); assertEquals(monday.plusDays(1), Progress.localDay(instant, ZoneId.of("Europe/London"))); assertEquals(monday, Progress.localDay(instant, ZoneId.of("America/New_York"))) }
    @Test fun dstGapReminderMovesForward() { val now = ZonedDateTime.of(2026, 3, 29, 0, 0, 0, 0, ZoneId.of("Europe/London")); val next = Progress.nextReminder(LocalTime.of(1, 30), Schedule(start = now.toLocalDate()), now)!!; assertEquals(2, next.hour); assertTrue(next.isAfter(now)) }
    @Test fun reminderSkipsPastTime() { val now = monday.atTime(9, 0).atZone(ZoneId.of("Europe/London")); assertEquals(monday.plusWeeks(1), Progress.nextReminder(LocalTime.of(8, 0), Schedule(setOf(DayOfWeek.MONDAY), monday), now)!!.toLocalDate()) }
    @Test fun createVoiceCommand() { assertEquals(Command.Create("Morning"), CommandParser.parse("Create a morning routine.")) }
    @Test fun addVoiceCommand() { assertEquals(Command.Add("reading", "evening"), CommandParser.parse("Add reading to my evening routine.")) }
    @Test fun completeVoiceCommand() { assertEquals(Command.Complete("reading"), CommandParser.parse("Mark reading as complete.")) }
    @Test fun readCommands() { assertEquals(Command.Remaining, CommandParser.parse("What is left on my list?")); assertEquals(Command.WeeklyProgress, CommandParser.parse("Show my weekly progress.")); assertEquals(Command.Read, CommandParser.parse("Read my daily routine.")) }
    @Test fun reminderVoiceCommand() { assertEquals(Command.Remind(LocalTime.of(8, 0), true), CommandParser.parse("Remind me at eight tomorrow.")); assertEquals(Command.Remind(LocalTime.of(20, 0), false), CommandParser.parse("Remind me at 8 pm")) }
    @Test fun invalidTimesNeverThrow() { assertTrue(CommandParser.parse("remind me at 25:90") is Command.Unknown); assertTrue(CommandParser.parse("remind me at 15 pm") is Command.Unknown) }
    @Test fun unsupportedLanguageExplicit() { assertTrue(CommandParser.parse("create a routine", Locale.FRANCE) is Command.Unknown) }
    @Test fun ambiguousCommandNotGuessed() { assertTrue(CommandParser.parse("do it") is Command.Unknown) }
    @Test fun unverifiedReceiptCannotUnlock() { assertFalse(Entitlement("u", "r", false).permits(Feature.MORE_ROUTINES)); assertFalse(Entitlement("u", "r", true, true).permits(Feature.DUPLICATION)); assertTrue(Entitlement("u", "r", true).permits(Feature.MONTHLY_INSIGHTS)) }
    @Test fun emptyReceiptCannotUnlock() { assertFalse(Entitlement("u", "", true).permits(Feature.MORE_ROUTINES)) }
}
