package com.anomot.anomotbackend.dto

import java.util.*

data class AdminBattleDto(
        val goldPost: PostDto?,
        val redPost: PostDto?,
        val goldVotes: Long,
        val redVotes: Long,
        val isFinished: Boolean = false,
        val until: Date,
        val id: String
)