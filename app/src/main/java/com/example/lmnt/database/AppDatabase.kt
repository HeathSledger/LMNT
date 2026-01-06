package com.example.lmnt.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Wir behalten PlaybackHistory und SongMetadata bei und fügen die Playlist-Funktionen hinzu.
// Version 5 löscht durch fallbackToDestructiveMigration() die alten Tabellen und erstellt sie neu.
@Database(
    entities = [
        PlaybackHistory::class,
        SongMetadata::class,
        Playlist::class,
        PlaylistSongCrossRef::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Wichtig: Room.databaseBuilder ist der korrekte Befehl
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lmnt_database"
                )
                    // Behält die App stabil, wenn wir Entities hinzufügen oder ändern
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}