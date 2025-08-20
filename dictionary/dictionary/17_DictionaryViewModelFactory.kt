package com.abhayattcc.dictionaryreader.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.abhayattcc.dictionaryreader.database.DictionaryDao

class DictionaryViewModelFactory(
    private val application: Application,
    private val dictionaryDao: DictionaryDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DictionaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DictionaryViewModel(application, dictionaryDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}