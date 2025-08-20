package com.abhayattcc.dictionaryreader.utils

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

object FileUtils {
    private val dbUrls = mapOf(
        "english_odia" to "https://raw.githubusercontent.com/abhayattcc/odia_dictionary_purnachandra_bhasakosa_by_abhayattcc/refs/heads/odia_dictionary/main/english_odia.db.gz",
        "odia_meaning" to "https://raw.githubusercontent.com/abhayattcc/odia_dictionary_purnachandra_bhasakosa_by_abhayattcc/refs/heads/odia_dictionary/main/odia_meaning.db.gz",
        "english_hindi" to "https://raw.githubusercontent.com/abhayattcc/odia_dictionary_purnachandra_bhasakosa_by_abhayattcc/refs/heads/odia_dictionary/main/english_hindi.db.gz"
    )

    fun checkAndRequestPermissions(activity: Activity, onPermissionsGranted: () -> Unit) {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.POST_NOTIFICATIONS
        )
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isEmpty()) {
            onPermissionsGranted()
        } else {
            ActivityCompat.requestPermissions(activity, notGranted.toTypedArray(), 100)
        }
    }

    fun showPermissionRationale(activity: Activity, onRetry: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle("Permissions Required")
            .setMessage("This app requires storage access to save dictionary databases, system alert window for clipboard popups, and notifications for background clipboard monitoring.")
            .setPositiveButton("Grant") { _, _ -> onRetry() }
            .setNegativeButton("Cancel") { _, _ -> activity.finish() }
            .show()
    }

    suspend fun downloadResources(activity: Activity, onComplete: () -> Unit) {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val dbDir = File(activity.filesDir, "databases")
            if (!dbDir.exists()) dbDir.mkdirs()

            val totalFiles = dbUrls.size
            var processedFiles = 0

            dbUrls.forEach { (name, url) ->
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val gzFile = File(dbDir, "$name.db.gz")
                        val dbFile = File(dbDir, "$name.db")
                        response.body?.byteStream()?.use { input ->
                            FileOutputStream(gzFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        GZIPInputStream(gzFile.inputStream()).use { gzip ->
                            FileOutputStream(dbFile).use { output ->
                                gzip.copyTo(output)
                            }
                        }
                        gzFile.delete()
                    }
                }
                processedFiles++
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Downloading: ${processedFiles * 100 / totalFiles}%", Toast.LENGTH_SHORT).show()
                }
            }

            // Download ML Kit Odia model
            val modelManager = RemoteModelManager.getInstance()
            val conditions = DownloadConditions.Builder().build()
            modelManager.download(
                com.google.mlkit.common.model.RemoteModel("odia"),
                conditions
            ).addOnSuccessListener {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "ML Kit Odia model downloaded", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            }.addOnFailureListener {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Failed to download ML Kit model", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun uriToFile(context: Context, uri: Uri): File? {
        val contentResolver: ContentResolver = context.contentResolver
        val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            cursor.getString(cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
        } ?: "temp_file"
        val file = File(context.cacheDir, fileName)
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun loadPdf(file: File): String {
        return withContext(Dispatchers.IO) {
            val pdfView = com.github.barteksc.pdfviewer.PDFView(file.context, null)
            pdfView.fromFile(file).load()
            val text = StringBuilder()
            for (page in 0 until pdfView.pageCount) {
                text.append(OcrUtils.processOcr(file.context, file)).append("\n\n")
            }
            text.toString()
        }
    }

    fun exportText(context: Context, text: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "DictionaryReaderExport_${System.currentTimeMillis()}.txt")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }
        val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { output ->
                output.write(text.toByteArray())
            }
            Toast.makeText(context, "Text exported to Documents", Toast.LENGTH_SHORT).show()
        }
    }
}