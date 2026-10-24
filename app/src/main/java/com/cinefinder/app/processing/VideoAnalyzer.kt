package com.cinefinder.app.processing

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.cinefinder.app.model.MatchResult
import com.cinefinder.app.model.ReferenceMovie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VideoAnalyzer {
    suspend fun analyzeVideo(
        context: Context,
        videoUri: Uri,
        references: List<ReferenceMovie>,
        onProgress: (Float, String) -> Unit
    ): List<MatchResult> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            context.contentResolver.openFileDescriptor(videoUri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
            } ?: throw IllegalStateException("Não foi possível aceder ao vídeo.")

            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

            if (durationMs <= 0L) throw IllegalStateException("Duração inválida ou formato incompatível.")

            val samplePoints = 10
            val interval = durationMs * 1000L / (samplePoints + 1)
            val extractedHashes = mutableListOf<Long>()

            for (i in 1..samplePoints) {
                val timeUs = i * interval
                val frameBitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frameBitmap != null) {
                    extractedHashes.add(ImageHasher.computeDHash(frameBitmap))
                }
                onProgress((i.toFloat() / samplePoints.toFloat()) * 0.7f, "A analisar fotograma $i de $samplePoints...")
            }

            onProgress(0.85f, "A comparar fotogramas com referências...")
            val results = references.map { ref ->
                val bestScore = extractedHashes.maxOfOrNull { frameHash ->
                    ImageHasher.calculateSimilarity(ref.hash, frameHash)
                } ?: 0.0
                MatchResult(movie = ref, similarityPercentage = bestScore)
            }.sortedByDescending { it.similarityPercentage }

            onProgress(1.0f, "Concluído!")
            results
        } finally {
            retriever.release()
        }
    }
}