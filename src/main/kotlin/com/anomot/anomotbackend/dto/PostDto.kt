package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.PostType

data class PostDto(
        val type: PostType,
        val text: String?,
        val media: MediaDto?,
        val poster: UserDto?,
        val likes: Long, //TODO likes
        val id: String
)
