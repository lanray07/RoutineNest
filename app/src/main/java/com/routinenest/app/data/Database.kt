package com.routinenest.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "routines")
data class RoutineEntity(@PrimaryKey val id: String, val name: String, val days: String, val start: String, val paused: Boolean = false, val archived: Boolean = false, val reminder: String = "")
@Entity(tableName = "tasks", indices = [Index("routineId")], foreignKeys = [ForeignKey(entity = RoutineEntity::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE)])
data class TaskEntity(@PrimaryKey val id: String, val routineId: String, val name: String, val position: Int, val created: String, val removed: String? = null)
@Entity(tableName = "completions", primaryKeys = ["taskId", "day"], indices = [Index("routineId")])
data class CompletionEntity(val taskId: String, val routineId: String, val day: String, val taskName: String)
@Entity(tableName = "routine_states", primaryKeys = ["routineId", "effectiveDay"], foreignKeys = [ForeignKey(entity = RoutineEntity::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE)])
data class RoutineStateEntity(val routineId: String, val effectiveDay: String, val days: String, val paused: Boolean, val archived: Boolean)
data class RoutineWithTasks(@Embedded val routine: RoutineEntity, @Relation(parentColumn = "id", entityColumn = "routineId") val tasks: List<TaskEntity>, @Relation(parentColumn = "id", entityColumn = "routineId") val states: List<RoutineStateEntity>)

@Dao
interface NestDao {
    @Transaction @Query("SELECT * FROM routines ORDER BY start, name") fun observeRoutines(): Flow<List<RoutineWithTasks>>
    @Query("SELECT * FROM completions ORDER BY day DESC") fun observeCompletions(): Flow<List<CompletionEntity>>
    @Transaction @Query("SELECT * FROM routines WHERE id = :id") suspend fun routine(id: String): RoutineWithTasks?
    @Transaction @Query("SELECT * FROM routines") suspend fun all(): List<RoutineWithTasks>
    @Query("SELECT * FROM completions ORDER BY day DESC") suspend fun completions(): List<CompletionEntity>
    @Upsert suspend fun upsertRoutine(routine: RoutineEntity)
    @Upsert suspend fun upsertTasks(tasks: List<TaskEntity>)
    @Upsert suspend fun upsertState(state: RoutineStateEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun complete(completion: CompletionEntity)
    @Query("DELETE FROM completions WHERE taskId = :taskId AND day = :day") suspend fun undo(taskId: String, day: String)
    @Query("DELETE FROM completions") suspend fun clearCompletions()
    @Query("DELETE FROM routines") suspend fun clearRoutines()
}

@Database(entities = [RoutineEntity::class, TaskEntity::class, CompletionEntity::class, RoutineStateEntity::class], version = 1, exportSchema = true)
abstract class NestDatabase : RoomDatabase() { abstract fun dao(): NestDao }
