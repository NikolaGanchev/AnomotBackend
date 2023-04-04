package com.anomot.anomotbackend.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable
import java.util.*

data class ChatMessageDto(
        val member: ChatMemberDto?,
        val message: String,
        @get:JsonProperty("isSystem")
        val isSystem: Boolean,
        val creationDate: Date
): Serializable
