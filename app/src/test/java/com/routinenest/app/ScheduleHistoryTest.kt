package com.routinenest.app

import com.routinenest.app.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ScheduleHistoryTest {
    private val start = LocalDate.of(2026, 10, 12)
    private val routine = RoutineEntity("r", "Morning", "1", start.toString(), paused = true, archived = true)
    @Test fun editsAndPausesKeepPreviousEligibility() {
        val record = RoutineWithTasks(routine, emptyList(), listOf(RoutineStateEntity("r",start.toString(),"1,2,3,4,5,6,7",false,false),RoutineStateEntity("r",start.plusDays(1).toString(),"1",true,true)))
        assertTrue(record.dueOn(start)); assertFalse(record.dueOn(start.plusDays(1)))
    }
    @Test fun resumeUsesEffectiveWeekdays() {
        val record = RoutineWithTasks(routine.copy(paused = false, archived = false), emptyList(), listOf(RoutineStateEntity("r",start.toString(),"1,2,3,4,5,6,7",false,false),RoutineStateEntity("r",start.plusDays(1).toString(),"1",false,false)))
        assertFalse(record.dueOn(start.plusDays(1))); assertTrue(record.dueOn(start.plusWeeks(1)))
    }
    @Test fun taskRemovalKeepsPriorDays() {
        val task = TaskEntity("t","r","Water",0,start.toString(),start.plusDays(1).toString())
        val record = RoutineWithTasks(routine,listOf(task),emptyList())
        assertEquals(1,record.activeTasks(start).size); assertEquals(0,record.activeTasks(start.plusDays(1)).size)
    }
}
