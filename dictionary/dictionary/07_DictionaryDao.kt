package com.abhayattcc.dictionaryreader.database

import androidx.room.Dao
import androidx.room.Query
import com.abhayattcc.dictionaryreader.models.DictionaryEntry

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM english_odia WHERE LOWER(english) = :word OR LOWER(odia) = :word")
    suspend fun getEnglishOdia(word: String): List<DictionaryEntry>

    @Query("SELECT * FROM english_hindi WHERE LOWER(english) = :word OR LOWER(hindi) = :word")
    suspend fun getEnglishHindi(word: String): List<DictionaryEntry>

    @Query("SELECT * FROM odia_meaning WHERE LOWER(odia) = :word OR LOWER(odia_spelling) = :word")
    suspend fun getOdiaMeaning(word: String): List<DictionaryEntry>

    @Query("SELECT * FROM english_odia WHERE LOWER(english) LIKE :pattern OR LOWER(odia) LIKE :pattern")
    suspend fun getEnglishOdiaSuggestions(pattern: String): List<DictionaryEntry>

    @Query("SELECT * FROM english_hindi WHERE LOWER(english) LIKE :pattern OR LOWER(hindi) LIKE :pattern")
    suspend fun getEnglishHindiSuggestions(pattern: String): List<DictionaryEntry>

    @Query("SELECT * FROM odia_meaning WHERE LOWER(odia) LIKE :pattern OR LOWER(odia_spelling) LIKE :pattern")
    suspend fun getOdiaMeaningSuggestions(pattern: String): List<DictionaryEntry>

    @Query("SELECT * FROM english_odia WHERE LOWER(english) = :english")
    suspend fun getEnglishOdiaByEnglish(english: String): List<DictionaryEntry>

    @Query("SELECT * FROM english_odia WHERE LOWER(odia) = :odia")
    suspend fun getEnglishOdiaByOdia(odia: String): List<DictionaryEntry>

    @Query("SELECT * FROM english_hindi WHERE LOWER(english) = :english")
    suspend fun getEnglishHindiByEnglish(english: String): List<DictionaryEntry>

    @Query("SELECT * FROM odia_meaning WHERE LOWER(odia) = :odia")
    suspend fun getOdiaMeaningByOdia(odia: String): List<DictionaryEntry>
}