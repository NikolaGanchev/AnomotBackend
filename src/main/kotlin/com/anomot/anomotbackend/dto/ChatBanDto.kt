package com.anomot.anomotbackend.dto

import java.util.*

data class ChatBanDto(
        val creationDate: Date,
        val until: Date,
        val bannedBy: ChatMemberDto?,
        val reason: String,
        val id: String
)
