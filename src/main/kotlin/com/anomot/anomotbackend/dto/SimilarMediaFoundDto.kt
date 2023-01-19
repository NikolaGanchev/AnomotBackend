package com.anomot.anomotbackend.dto

data class SimilarMediaFoundDto(
        val media: MediaDto,
        val similarPosts: List<PostDto>?
)