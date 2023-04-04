package com.anomot.anomotbackend.dto

import java.io.Serializable

data class ChatMemberDto(
        val user: UserDto?,
        val chatUsername: String,
        val roles: List<String>,
        val id: String
): Serializable
