package org.nano.updater.di.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import org.nano.updater.database.UpdaterDao
import org.nano.updater.database.UpdaterDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Module
class DatabaseModule @Inject constructor() {

    @Singleton
    @Provides
    fun provideUpdaterDatabase(context: Context): UpdaterDatabase {
        return Room.databaseBuilder(
            context,
            UpdaterDatabase::class.java,
            "updater.db"
        ).build()
    }

    @Singleton
    @Provides
    fun provideUpdaterDao(updaterDatabase: UpdaterDatabase): UpdaterDao {
        return updaterDatabase.getUpdaterDao()
    }
}
