package com.anomot.anomotbackend.dto

data class AvatarResultDto(
        val avatarId: String,
        val hasNsfw: Boolean,
        val appealJwt: String?
)
