package com.videocleaner.util

import android.graphics.Bitmap
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Perceptual hashing implementation for video frame similarity detection.
 *
 * Implements DCT-based pHash (Perceptual Hash):
 * 1. Resize image to 32×32
 * 2. Convert to grayscale
 * 3. Apply DCT (Discrete Cosine Transform)
 * 4. Take top-left 8×8 of DCT coefficients
 * 5. Compute average of those 64 values
 * 6. Set bit = 1 if value > average, 0 otherwise
 * Result: 64-bit hash stored as Long
 */
object PerceptualHash {

    private const val HASH_SIZE = 8     // 8×8 = 64-bit hash
    private const val DCT_SIZE = 32     // Resize to 32×32 before DCT

    /**
     * Computes the pHash of a bitmap.
     * Returns a 64-bit Long representing the hash.
     */
    fun compute(bitmap: Bitmap): Long {
        // Step 1: Resize to DCT_SIZE × DCT_SIZE
        val resized = Bitmap.createScaledBitmap(bitmap, DCT_SIZE, DCT_SIZE, true)

        // Step 2: Convert to grayscale values
        val pixels = Array(DCT_SIZE) { row ->
            DoubleArray(DCT_SIZE) { col ->
                val pixel = resized.getPixel(col, row)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                // Luminance formula (BT.709)
                0.2126 * r + 0.7152 * g + 0.0722 * b
            }
        }

        if (resized != bitmap) resized.recycle()

        // Step 3: Apply 2D DCT
        val dct = applyDct(pixels)

        // Step 4: Extract top-left HASH_SIZE × HASH_SIZE
        val topLeft = DoubleArray(HASH_SIZE * HASH_SIZE)
        var idx = 0
        for (row in 0 until HASH_SIZE) {
            for (col in 0 until HASH_SIZE) {
                topLeft[idx++] = dct[row][col]
            }
        }

        // Step 5: Compute average (excluding DC component at [0][0])
        val avg = topLeft.drop(1).average()

        // Step 6: Build 64-bit hash
        var hash = 0L
        topLeft.forEachIndexed { i, value ->
            if (value > avg) hash = hash or (1L shl i)
        }
        return hash
    }

    /**
     * Computes Hamming distance between two hashes.
     * Returns number of differing bits (0 = identical, 64 = completely different).
     */
    fun hammingDistance(hash1: Long, hash2: Long): Int =
        java.lang.Long.bitCount(hash1 xor hash2)

    /**
     * Converts Hamming distance to similarity percentage.
     * 0 bits different = 100% similar, 64 bits different = 0% similar.
     */
    fun similarityPercent(hash1: Long, hash2: Long): Float {
        val distance = hammingDistance(hash1, hash2)
        return ((64 - distance) / 64.0f) * 100f
    }

    /**
     * Applies a 2D DCT to the pixel array.
     * Uses the separable property to apply 1D DCT on rows then columns.
     */
    private fun applyDct(pixels: Array<DoubleArray>): Array<DoubleArray> {
        val size = pixels.size
        val result = Array(size) { DoubleArray(size) }

        for (u in 0 until size) {
            for (v in 0 until size) {
                var sum = 0.0
                for (i in 0 until size) {
                    for (j in 0 until size) {
                        sum += cos((2 * i + 1) * u * Math.PI / (2 * size)) *
                               cos((2 * j + 1) * v * Math.PI / (2 * size)) *
                               pixels[i][j]
                    }
                }
                val cu = if (u == 0) 1.0 / sqrt(2.0) else 1.0
                val cv = if (v == 0) 1.0 / sqrt(2.0) else 1.0
                result[u][v] = (2.0 / size) * cu * cv * sum
            }
        }
        return result
    }
}
