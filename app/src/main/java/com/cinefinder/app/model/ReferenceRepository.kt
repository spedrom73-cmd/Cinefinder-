package com.cinefinder.app.model

import android.content.Context
import android.graphics.Bitmap
import com.cinefinder.app.processing.ImageHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ReferenceRepository(private val context: Context) {
    private val indexFile = File(context.filesDir, "references_index.txt")

    suspend fun getReferences(): List<ReferenceMovie> = withContext(Dispatchers.IO) {
        if (!indexFile.exists()) return@withContext emptyList()
        val list = mutableListOf<ReferenceMovie>()
        indexFile.readLines().forEach { line ->
            val parts = line.split("|")
            if (parts.size >= 4) {
                list.add(
                    ReferenceMovie(
                        id = parts[0],
                        title = parts[1],
                        hash = parts[2].toLongOrNull() ?: 0L,
                        imagePath = parts[3]
                    )
                )
            }
        }
        list
    }

    suspend fun addReference(title: String, bitmap: Bitmap): ReferenceMovie = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val hash = ImageHasher.computeDHash(bitmap)
        val file = File(context.filesDir, "ref_$id.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        val item = ReferenceMovie(id, title, hash, file.absolutePath)
        indexFile.appendText("${item.id}|${item.title}|${item.hash}|${item.imagePath}\n")
        item
    }

    suspend fun removeReference(id: String) = withContext(Dispatchers.IO) {
        val current = getReferences()
        val remaining = current.filter { it.id != id }
        val toDelete = current.find { it.id == id }
        toDelete?.let { File(it.imagePath).delete() }
        indexFile.writeText("")
        remaining.forEach { item ->
            indexFile.appendText("${item.id}|${item.title}|${item.hash}|${item.imagePath}\n")
        }
    }
}