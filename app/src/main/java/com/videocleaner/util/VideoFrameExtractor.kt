package com.videocleaner.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts frames from a video at specified percentage positions.
 * Used for perceptual hash computation in similar video detection.
 */
object VideoFrameExtractor {

    /**
     * Extracts frames at 0%, 25%, 50%, 75%, and 100% of the video duration.
     *
     * @param context Application context
     * @param uri     URI of the video file
     * @param duration Duration of the video in milliseconds
     * @return List of bitmaps at requested positions (may be smaller if extraction fails)
     */
    suspend fun extractKeyFrames(
        context: Context,
        uri: Uri,
        duration: Long
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()

        try {
            retriever.setDataSource(context, uri)

            // Extract at 0%, 25%, 50%, 75%, 100%
            val positions = listOf(0f, 0.25f, 0.50f, 0.75f, 1.0f)

            positions.forEach { percentage ->
                val timeUs = (duration * percentage * 1000).toLong()  // µs
                val frame = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                if (frame != null) frames.add(frame)
            }
        } catch (e: Exception) {
            // Log error and return whatever frames were collected
        } finally {
            retriever.release()
        }

        frames
    }

    /**
     * Computes a combined perceptual hash from multiple frames.
     * Each frame hash is stored in a LongArray for comparison.
     */
    suspend fun computeVideoHash(
        context: Context,
        uri: Uri,
        duration: Long
    ): LongArray? = withContext(Dispatchers.IO) {
        val frames = extractKeyFrames(context, uri, duration)
        if (frames.isEmpty()) return@withContext null

        val hashes = LongArray(frames.size) { i ->
            PerceptualHash.compute(frames[i]).also { frames[i].recycle() }
        }
        hashes
    }

    /**
     * Computes similarity between two video hash arrays.
     * Returns average similarity across all frame pairs.
     */
    fun computeSimilarity(hashes1: LongArray, hashes2: LongArray): Float {
        if (hashes1.isEmpty() || hashes2.isEmpty()) return 0f

        val minLength = minOf(hashes1.size, hashes2.size)
        var totalSimilarity = 0f

        for (i in 0 until minLength) {
            totalSimilarity += PerceptualHash.similarityPercent(hashes1[i], hashes2[i])
        }

        return totalSimilarity / minLength
    }
}
