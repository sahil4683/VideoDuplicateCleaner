package com.videocleaner.domain

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.videocleaner.domain.model.DuplicateGroup
import com.videocleaner.domain.model.DuplicateType
import com.videocleaner.domain.model.VideoFile
import org.junit.Test

/**
 * Unit tests for DuplicateGroup domain model computations.
 */
class DuplicateGroupTest {

    private fun makeVideo(id: Long, size: Long) = VideoFile(
        id = id,
        uri = Uri.EMPTY,
        fileName = "video_$id.mp4",
        size = size,
        duration = 5000L,
        width = 1920, height = 1080,
        dateCreated = 0L, dateModified = 0L,
        folder = "DCIM/", mimeType = "video/mp4"
    )

    @Test
    fun `totalSize sums all video sizes`() {
        val group = DuplicateGroup(
            groupId = "g1",
            type = DuplicateType.EXACT,
            videos = listOf(
                makeVideo(1, 1_000_000L),
                makeVideo(2, 2_000_000L),
                makeVideo(3, 3_000_000L)
            )
        )
        assertThat(group.totalSize).isEqualTo(6_000_000L)
    }

    @Test
    fun `recoverableSize is total minus largest`() {
        val group = DuplicateGroup(
            groupId = "g1",
            type = DuplicateType.EXACT,
            videos = listOf(
                makeVideo(1, 1_000_000L),
                makeVideo(2, 5_000_000L),
                makeVideo(3, 2_000_000L)
            )
        )
        // Total = 8MB, largest = 5MB, recoverable = 3MB
        assertThat(group.recoverableSize).isEqualTo(3_000_000L)
    }

    @Test
    fun `count returns number of videos`() {
        val group = DuplicateGroup(
            groupId = "g1",
            type = DuplicateType.EXACT,
            videos = listOf(makeVideo(1, 100L), makeVideo(2, 200L))
        )
        assertThat(group.count).isEqualTo(2)
    }

    @Test
    fun `recoverableSize is zero for single video`() {
        val group = DuplicateGroup(
            groupId = "g1",
            type = DuplicateType.EXACT,
            videos = listOf(makeVideo(1, 5_000_000L))
        )
        assertThat(group.recoverableSize).isEqualTo(0L)
    }
}
