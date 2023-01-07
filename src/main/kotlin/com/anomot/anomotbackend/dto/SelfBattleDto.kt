package com.anomot.anomotbackend.dto

import java.util.Date

data class SelfBattleDto(
        val selfPost: PostDto?,
        val otherPost: PostDto?,
        val selfVotes: Long,
        val otherVotes: Long,
        val isFinished: Boolean = false,
        val until: Date
)
