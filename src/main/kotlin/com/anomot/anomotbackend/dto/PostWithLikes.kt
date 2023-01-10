package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.entities.Post

data class PostWithLikes(
        val post: Post?,
        val likes: Long,
        val hasUserLiked: Boolean
)