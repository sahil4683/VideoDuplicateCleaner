package com.videocleaner.data

import com.google.common.truth.Truth.assertThat
import com.videocleaner.util.PerceptualHash
import org.junit.Test

/**
 * Unit tests for perceptual hashing utility.
 * Tests hash properties (determinism, distance, similarity scoring).
 */
class HashUtilsTest {
    @Test
    fun `hamming distance of identical hashes is zero`() {
        val hash = 0b1010101010101010L
        assertThat(PerceptualHash.hammingDistance(hash, hash)).isEqualTo(0)
    }

    @Test
    fun `hamming distance of completely different hashes is 64`() {
        val hash1 = 0L
        val hash2 = -1L // All bits set
        assertThat(PerceptualHash.hammingDistance(hash1, hash2)).isEqualTo(64)
    }

    @Test
    fun `hamming distance with one bit difference is 1`() {
        val hash1 = 0L
        val hash2 = 1L
        assertThat(PerceptualHash.hammingDistance(hash1, hash2)).isEqualTo(1)
    }

    @Test
    fun `similarity of identical hashes is 100 percent`() {
        val hash = 0xDEADBEEFCAFEBABEL
        val similarity = PerceptualHash.similarityPercent(hash, hash)
        assertThat(similarity).isEqualTo(100f)
    }

    @Test
    fun `similarity of completely different hashes is 0 percent`() {
        val hash1 = 0L
        val hash2 = -1L
        val similarity = PerceptualHash.similarityPercent(hash1, hash2)
        assertThat(similarity).isEqualTo(0f)
    }

    @Test
    fun `similarity is symmetric`() {
        val hash1 = 0b1111000011110000L
        val hash2 = 0b0000111100001111L
        val sim1 = PerceptualHash.similarityPercent(hash1, hash2)
        val sim2 = PerceptualHash.similarityPercent(hash2, hash1)
        assertThat(sim1).isEqualTo(sim2)
    }

    @Test
    fun `hamming distance is symmetric`() {
        val hash1 = 0x1234567890ABCDEFL
        val hash2 = 0xFEDCBA9876543210L
        assertThat(PerceptualHash.hammingDistance(hash1, hash2))
            .isEqualTo(PerceptualHash.hammingDistance(hash2, hash1))
    }

    @Test
    fun `similarity within 90 percent threshold for near identical hashes`() {
        // Hashes differing by at most 6 bits should be >= 90% similar
        val base = 0b1111111111111111111111111111111111111111111111111111111111111111L
        val nearIdentical = base xor 0b111111L // 6 bits different
        val similarity = PerceptualHash.similarityPercent(base, nearIdentical)
        assertThat(similarity).isAtLeast(90f)
    }
}
