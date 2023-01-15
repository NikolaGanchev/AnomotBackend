package com.anomot.anomotbackend.dto

import java.util.Date

data class CommentEditDto(
        val text: String,
        val changedAt: Date,
        val id: String
)
