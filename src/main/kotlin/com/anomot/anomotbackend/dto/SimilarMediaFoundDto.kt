package com.anomot.anomotbackend.dto

data class SimilarMediaFoundDto(
        val appealJwt: String,
        val media: MediaDto,
        val similarPosts: List<PostDto>?
)