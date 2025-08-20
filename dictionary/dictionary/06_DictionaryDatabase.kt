package com.abhayattcc.dictionaryreader.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.abhayattcc.dictionaryreader.models.DictionaryEntry

@Database(entities = [DictionaryEntry::class], version = 1, exportSchema = false)
abstract class DictionaryDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao

    companion object {
        @Volatile
        private var INSTANCE: DictionaryDatabase? = null

        fun getDatabase(context: Context): DictionaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DictionaryDatabase::class.java,
                    "dictionary_database"
                )
                    .createFromAsset("databases/english_odia.db")
                    .createFromAsset("databases/odia_meaning.db")
                    .createFromAsset("databases/english_hindi.db")
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}