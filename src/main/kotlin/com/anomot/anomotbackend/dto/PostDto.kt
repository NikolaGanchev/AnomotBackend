package com.anomot.anomotbackend.dto

data class PostDto(
        val poster: UserDto,
        val likes: Long,
        val id: String
)
