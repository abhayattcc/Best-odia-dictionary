package com.abhayattcc.dictionaryreader.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.abhayattcc.dictionaryreader.database.DictionaryDao
import com.abhayattcc.dictionaryreader.models.DictionaryEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DictionaryViewModel(application: Application, private val dictionaryDao: DictionaryDao) : AndroidViewModel(application) {
    private val _results = MutableLiveData<String>()
    val results: LiveData<String> = _results
    private val _suggestions = MutableLiveData<List<String>>()
    val suggestions: LiveData<List<String>> = _suggestions

    fun searchWord(word: String, isExact: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val results = mutableListOf<String>()
            val suggestions = mutableListOf<String>()
            val searchTerm = word.trim().lowercase()
            val pattern = if (isExact) searchTerm else "%$searchTerm%"

            // Search English-Odia
            val enOdResults = if (isExact) {
                dictionaryDao.getEnglishOdiaByEnglish(searchTerm) + dictionaryDao.getEnglishOdiaByOdia(searchTerm)
            } else {
                dictionaryDao.getEnglishOdiaSuggestions(pattern)
            }
            enOdResults.forEach {
                if (it.english.isNotEmpty() && it.odia.isNotEmpty()) {
                    results.add("English: ${it.english}\nOdia: ${it.odia}")
                    if (!isExact) suggestions.add(it.english)
                }
            }

            // Search English-Hindi
            val enHiResults = if (isExact) {
                dictionaryDao.getEnglishHindiByEnglish(searchTerm)
            } else {
                dictionaryDao.getEnglishHindiSuggestions(pattern)
            }
            enHiResults.forEach {
                if (it.english.isNotEmpty() && it.hindi.isNotEmpty()) {
                    results.add("English: ${it.english}\nHindi: ${it.hindi}")
                    if (!isExact) suggestions.add(it.english)
                }
            }

            // Search Odia Meaning
            val odiaResults = if (isExact) {
                dictionaryDao.getOdiaMeaningByOdia(searchTerm)
            } else {
                dictionaryDao.getOdiaMeaningSuggestions(pattern)
            }
            odiaResults.forEach {
                if (it.odia.isNotEmpty() && it.odiaMeaning.isNotEmpty()) {
                    results.add("Odia: ${it.odia}\nSpelling: ${it.odiaSpelling}\nMeaning: ${it.odiaMeaning}")
                    if (!isExact) suggestions.add(it.odia)
                }
            }

            CoroutineScope(Dispatchers.Main).launch {
                _results.value = if (results.isEmpty()) "No results found" else results.joinToString("\n\n")
                _suggestions.value = suggestions.distinct()
            }
        }
    }
}