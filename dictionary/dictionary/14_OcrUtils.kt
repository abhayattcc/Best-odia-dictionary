package com.abhayattcc.dictionaryreader.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

object OcrUtils {
    suspend fun processOcr(context: Context, file: File): String {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.path)
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            "OCR Error: ${e.message}"
        }
    }
}