package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.PostType
import java.util.Date

data class PostDto(
        val type: PostType,
        val text: String?,
        val media: MediaDto?,
        val poster: UserDto?,
        val likes: Long?,
        val hasUserLiked: Boolean?,
        val creationDate: Date,
        val id: String
)
