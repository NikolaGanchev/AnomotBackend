package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.MediaType

data class MediaDto(
        val type: MediaType,
        val id: String
)