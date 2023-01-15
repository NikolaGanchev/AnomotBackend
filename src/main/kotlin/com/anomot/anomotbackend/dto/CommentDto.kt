package com.anomot.anomotbackend.dto

data class CommentDto(
        val text: String?,
        val commenter: UserDto?,
        val isEdited: Boolean
)