package com.routinenest.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.routinenest.app.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    @Test fun createCompleteUndoEditArchiveAndReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "persistence-instrumentation.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, NestDatabase::class.java, name).build()
        var repo = NestRepository(db)
        val id = repo.save(null, "Morning", listOf(null to "Reading", null to "Water"), DayOfWeek.entries.toSet(), "08:00")
        val task = repo.all().single().activeTasks().first()
        repo.complete(task, LocalDate.now(), true)
        db.close()
        // Reopening validates Room's v1 schema identity and durable state.
        db = Room.databaseBuilder(context, NestDatabase::class.java, name).build(); repo = NestRepository(db)
        assertEquals(1, repo.history().size)
        assertEquals("Morning", repo.all().single().routine.name)
        repo.complete(task, LocalDate.now(), false); assertTrue(repo.history().isEmpty())
        repo.complete(task, LocalDate.now(), true)
        repo.save(id, "Morning edited", listOf(task.id to "Read a book"), setOf(LocalDate.now().dayOfWeek), "09:00")
        assertEquals(1, repo.all().single().activeTasks().size)
        assertEquals("Reading", repo.history().single().taskName)
        repo.setState(repo.all().single().routine, paused = true, archived = true)
        assertTrue(repo.all().single().routine.archived)
        repo.duplicate(repo.all().single()); assertEquals(2, repo.all().size)
        repo.deleteAll(); assertTrue(repo.all().isEmpty()); assertTrue(repo.history().isEmpty())
        db.close(); context.deleteDatabase(name)
    }
}
