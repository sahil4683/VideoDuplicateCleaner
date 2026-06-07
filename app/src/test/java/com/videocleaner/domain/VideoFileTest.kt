package com.videocleaner.domain

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.videocleaner.domain.model.VideoFile
import org.junit.Test

/**
 * Unit tests for VideoFile domain model formatting helpers.
 */
class VideoFileTest {
    private fun makeVideo(
        size: Long = 1_048_576L,
        // 1:30
        duration: Long = 90_000L,
        width: Int = 1920,
        height: Int = 1080,
    ) = VideoFile(
        id = 1L,
        uri = Uri.EMPTY,
        fileName = "test.mp4",
        size = size,
        duration = duration,
        width = width,
        height = height,
        dateCreated = 0L,
        dateModified = 0L,
        folder = "DCIM/Camera/",
        mimeType = "video/mp4",
    )

    @Test
    fun `formattedSize shows MB for megabyte range`() {
        val video = makeVideo(size = 10_485_760L) // 10 MB
        assertThat(video.formattedSize).isEqualTo("10.0 MB")
    }

    @Test
    fun `formattedSize shows GB for gigabyte range`() {
        val video = makeVideo(size = 1_073_741_824L) // 1 GB
        assertThat(video.formattedSize).isEqualTo("1.0 GB")
    }

    @Test
    fun `formattedSize shows KB for kilobyte range`() {
        val video = makeVideo(size = 2_048L) // 2 KB
        assertThat(video.formattedSize).isEqualTo("2.0 KB")
    }

    @Test
    fun `formattedDuration shows mm_ss format for under 1 hour`() {
        val video = makeVideo(duration = 90_000L) // 1 minute 30 seconds
        assertThat(video.formattedDuration).isEqualTo("01:30")
    }

    @Test
    fun `formattedDuration shows hh_mm_ss format for over 1 hour`() {
        val video = makeVideo(duration = 3_661_000L) // 1h 1m 1s
        assertThat(video.formattedDuration).isEqualTo("01:01:01")
    }

    @Test
    fun `resolution formats correctly`() {
        val video = makeVideo(width = 3840, height = 2160)
        assertThat(video.resolution).isEqualTo("3840×2160")
    }
}
