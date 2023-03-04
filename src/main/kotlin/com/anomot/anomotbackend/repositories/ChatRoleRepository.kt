package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Chat
import com.anomot.anomotbackend.entities.ChatMember
import com.anomot.anomotbackend.entities.ChatRole
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoleRepository: JpaRepository<ChatRole, Long> {
    fun getByChatMember(chatMember: ChatMember): List<ChatRole>

    fun deleteAllByChatMemberChat(chat: Chat): Int
}