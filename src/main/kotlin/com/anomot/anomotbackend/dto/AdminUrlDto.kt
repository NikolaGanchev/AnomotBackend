package com.anomot.anomotbackend.dto

data class AdminUrlDto(
        val publisher: UserDto,
        val url: String,
        val threats: List<String>
)
