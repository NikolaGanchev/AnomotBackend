package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class CommentUploadDto(
        @Max(Constants.MAX_COMMENT_SIZE.toLong())
        @Min(Constants.MIN_COMMENT_SIZE.toLong())
        val text: String,
        val id: String
)
