package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.PostType

data class BattlePostDto(
        val type: PostType,
        val text: String?,
        val media: MediaDto?
)
