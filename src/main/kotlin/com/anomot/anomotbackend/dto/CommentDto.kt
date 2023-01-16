package com.anomot.anomotbackend.dto

import java.util.*

data class CommentDto(
        val text: String?,
        val commenter: UserDto?,
        val isEdited: Boolean,
        val responseCount: Int?,
        val likes: Long,
        val hasUserLiked: Boolean,
        val lastChangeDate: Date?,
        val id: String
)