package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Chat
import com.anomot.anomotbackend.entities.ChatBan
import org.springframework.data.jpa.repository.JpaRepository

interface ChatBanRepository: JpaRepository<ChatBan, Long> {
    fun deleteAllByChatMemberChat(chat: Chat): Int
}