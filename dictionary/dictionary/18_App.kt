package com.abhayattcc.dictionaryreader

import android.app.Application
import com.abhayattcc.dictionaryreader.database.DictionaryDatabase

class App : Application() {
    lateinit var database: DictionaryDatabase

    override fun onCreate() {
        super.onCreate()
        database = DictionaryDatabase.getDatabase(this)
    }
}