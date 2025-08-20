package com.abhayattcc.dictionaryreader.models

import androidx.room.Entity

@Entity
data class DictionaryEntry(
    val english: String = "",
    val hindi: String = "",
    val odia: String = "",
    val odiaSpelling: String = "",
    val odiaMeaning: String = ""
)