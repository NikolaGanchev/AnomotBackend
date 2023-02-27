package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.entities.Post

data class PostWithLikeNumber(
        val post: Post,
        val likes: Long,
        val hasUserLiked: Boolean
)