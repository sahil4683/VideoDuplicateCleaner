package com.videocleaner.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Utility object for computing file hashes.
 *
 * Uses a multi-stage approach:
 * 1. Partial hash (first + middle + last 1 MB) for fast screening
 * 2. Full SHA-256 only when partial hashes match
 */
object HashUtils {
    private const val CHUNK_SIZE = 1_048_576L // 1 MB

    /**
     * Computes a partial hash by sampling three 1 MB chunks:
     * - First 1 MB
     * - Middle 1 MB
     * - Last 1 MB
     *
     * This is approximately 3x faster than full SHA-256 for large files.
     */
    suspend fun computePartialHash(
        context: Context,
        uri: Uri,
    ): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val available = stream.available().toLong()
                    val digest = MessageDigest.getInstance("SHA-256")
                    val buffer = ByteArray(CHUNK_SIZE.toInt())

                    // Read first 1 MB
                    var bytesRead = stream.read(buffer)
                    if (bytesRead > 0) digest.update(buffer, 0, bytesRead)

                    // Seek to middle and read 1 MB
                    if (available > CHUNK_SIZE * 2) {
                        val middlePos = (available / 2) - (CHUNK_SIZE / 2)
                        stream.skip(middlePos - bytesRead)
                        bytesRead = stream.read(buffer)
                        if (bytesRead > 0) digest.update(buffer, 0, bytesRead)
                    }

                    // Seek to last 1 MB
                    if (available > CHUNK_SIZE) {
                        val lastPos = available - CHUNK_SIZE
                        stream.skip(lastPos - stream.available())
                        bytesRead = stream.read(buffer)
                        if (bytesRead > 0) digest.update(buffer, 0, bytesRead)
                    }

                    digest.digest().toHexString()
                }
            }.getOrNull()
        }

    /**
     * Computes a full SHA-256 hash of the entire file.
     * Call this only after partial hashes match to confirm exact duplicates.
     */
    suspend fun computeFullSha256(
        context: Context,
        uri: Uri,
    ): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    val buffer = ByteArray(8_192) // 8 KB read buffer
                    var bytesRead: Int
                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                    digest.digest().toHexString()
                }
            }.getOrNull()
        }

    private fun ByteArray.toHexString(): String = joinToString("") { byte -> "%02x".format(byte) }
}
