package com.anomot.anomotbackend.dto

data class ChatMemberDto(
        val user: UserDto?,
        var chatUsername: String,
        val id: String
)
