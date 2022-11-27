package com.anomot.anomotbackend.dto

data class MediaSaveDto(
        val type: String,
        val phash: String?,
        val id: String,
        val avgNsfw: NsfwScanDto?,
        val maxNsfw: NsfwScanDto?,
        val duration: Float?
)


