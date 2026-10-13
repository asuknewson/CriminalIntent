package com.example.criminalintent

import android.app.Application
import android.content.Context
import androidx.room.*
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "crimes")
data class Crime(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val date: Long = System.currentTimeMillis(),
    val solved: Boolean = false,
    val notes: String = "",
    val photo: String? = null
)

@Dao
interface CrimeDao {
    @Query("SELECT * FROM crimes ORDER BY date DESC") fun observeAll(): Flow<List<Crime>>
    @Query("SELECT * FROM crimes WHERE id = :id") fun observe(id: String): Flow<Crime?>
    @Insert suspend fun insert(crime: Crime)
    @Query("UPDATE crimes SET title = :value WHERE id = :id") suspend fun title(id: String, value: String)
    @Query("UPDATE crimes SET notes = :value WHERE id = :id") suspend fun notes(id: String, value: String)
    @Query("UPDATE crimes SET date = :value WHERE id = :id") suspend fun date(id: String, value: Long)
    @Query("UPDATE crimes SET solved = :value WHERE id = :id") suspend fun solved(id: String, value: Boolean)
    @Query("UPDATE crimes SET photo = :value WHERE id = :id") suspend fun photo(id: String, value: String)
    @Query("DELETE FROM crimes WHERE id = :id") suspend fun delete(id: String)
}

@Database(entities = [Crime::class], version = 1, exportSchema = false)
abstract class CaseDatabase : RoomDatabase() { abstract fun crimes(): CrimeDao }

class CaseApplication : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database by lazy { Room.databaseBuilder(this, CaseDatabase::class.java, "case-files.db").build() }
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    override fun onCreate() {
        super.onCreate()
        scope.launch { for (write in writes) {
            try { write() } catch (e: Exception) {
                android.util.Log.e("CaseFiles", "Could not save case", e)
                withContext(Dispatchers.Main) { android.widget.Toast.makeText(this@CaseApplication, "Could not save. Check device storage.", android.widget.Toast.LENGTH_LONG).show() }
            }
        } }
    }
    fun write(action: suspend () -> Unit) { writes.trySend(action) }
}

class CaseViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CaseApplication
    val dao = app.database.crimes()
    val crimes = dao.observeAll()
    fun add(onCreated: (String) -> Unit) {
        val crime = Crime()
        app.write { dao.insert(crime); withContext(Dispatchers.Main) { onCreated(crime.id) } }
    }
    fun title(id: String, value: String) = app.write { dao.title(id, value) }
    fun notes(id: String, value: String) = app.write { dao.notes(id, value) }
    fun date(id: String, value: Long) = app.write { dao.date(id, value) }
    fun solved(id: String, value: Boolean) = app.write { dao.solved(id, value) }
    fun photo(id: String, value: String) = app.write { dao.photo(id, value) }
    fun delete(crime: Crime, done: () -> Unit) = app.write {
        dao.delete(crime.id)
        crime.photo?.let { java.io.File(app.filesDir, "evidence/$it").delete() }
        withContext(Dispatchers.Main) { done() }
    }
}
