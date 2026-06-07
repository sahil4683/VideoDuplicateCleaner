package com.videocleaner.util

import android.net.Uri
import com.videocleaner.data.local.entity.VideoEntity
import com.videocleaner.domain.model.VideoFile

/**
 * Extension functions to map between data layer entities and domain models.
 */
fun VideoEntity.toDomain(): VideoFile = VideoFile(
    id = id,
    uri = Uri.parse(uri),
    fileName = fileName,
    size = size,
    duration = duration,
    width = width,
    height = height,
    dateCreated = dateCreated,
    dateModified = dateModified,
    folder = folder,
    mimeType = mimeType,
    sha256 = sha256,
    partialHash = partialHash
)

fun List<VideoEntity>.toDomain(): List<VideoFile> = map { it.toDomain() }
