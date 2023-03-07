package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.entities.ChatMessage
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.utils.ChatRoles

interface ChatMessageWithUser {
    val chatMessage: ChatMessage
    val roles: List<ChatRoles>
    val user: User?
}