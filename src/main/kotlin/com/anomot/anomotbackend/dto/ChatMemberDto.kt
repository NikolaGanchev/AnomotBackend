package com.anomot.anomotbackend.dto

data class ChatMemberDto(
        val user: UserDto?,
        val chatUsername: String,
        val roles: List<String>,
        val id: String
)
