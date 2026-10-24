package com.cinefinder.app.processing

import android.graphics.Bitmap
import android.graphics.Color

object ImageHasher {
    fun computeDHash(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        var hash = 0L
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val leftPixel = scaled.getPixel(x, y)
                val rightPixel = scaled.getPixel(x + 1, y)
                val leftBrightness = getLuminance(leftPixel)
                val rightBrightness = getLuminance(rightPixel)
                if (leftBrightness > rightBrightness) {
                    hash = hash or (1L shl (y * 8 + x))
                }
            }
        }
        return hash
    }

    private fun getLuminance(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    fun calculateSimilarity(hash1: Long, hash2: Long): Double {
        val xor = hash1 xor hash2
        val hammingDistance = java.lang.Long.bitCount(xor)
        return ((64.0 - hammingDistance) / 64.0 * 100.0).coerceIn(0.0, 100.0)
    }
}