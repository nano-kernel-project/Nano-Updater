package org.nano.updater.database

import androidx.room.*
import org.nano.updater.model.entity.Update

@Database(entities = [Update::class], version = 1)
abstract class UpdaterDatabase: RoomDatabase() {
   abstract fun getUpdaterDao(): UpdaterDao
}

@Dao
interface UpdaterDao {
    @Query("SELECT * FROM `Update`")
    suspend fun getUpdateData(): Update

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpdateData(update: Update)
}
