package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Chat
import com.anomot.anomotbackend.entities.ChatBan
import com.anomot.anomotbackend.entities.ChatMember
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ChatBanRepository: JpaRepository<ChatBan, Long> {
    fun deleteAllByChatMemberChat(chat: Chat): Int

    fun getByChatMember(chatMember: ChatMember, pageable: Pageable): List<ChatBan>
}