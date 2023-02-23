package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.entities.Comment

data class AdminCommentIntermediate(
        val comment: Comment,
        val responseCount: Long,
        val likes: Long,
        val hasUserLiked: Boolean
)
