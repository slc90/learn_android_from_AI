package com.example.learnandroidfromai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class StudyNoteFileStore(
    private val context: Context
) {
    private val file = File(
        context.filesDir,
        "study_note.txt"
    )

    suspend fun save(text: String) {
        withContext(Dispatchers.IO) {
            file.writeText(text)
        }
    }

    suspend fun read(): String {
        return withContext(Dispatchers.IO) {
            if (file.exists()) {
                file.readText()
            } else {
                ""
            }
        }
    }
}