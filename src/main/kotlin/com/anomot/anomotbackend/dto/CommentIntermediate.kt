package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.entities.Comment

data class CommentIntermediate(
        val comment: Comment,
        val responseCount: Long,
        val likes: Long,
        val hasUserLiked: Boolean,
        val followsCommenter: Boolean,
)