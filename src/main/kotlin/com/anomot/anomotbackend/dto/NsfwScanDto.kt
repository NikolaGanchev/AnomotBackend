package com.anomot.anomotbackend.dto

data class NsfwScanDto(
        val drawings: Float,
        val hentai: Float,
        val neutral: Float,
        val porn: Float,
        val sexy: Float
)