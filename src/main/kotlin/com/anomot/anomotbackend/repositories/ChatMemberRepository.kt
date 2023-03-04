package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Chat
import com.anomot.anomotbackend.entities.ChatMember
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository

interface ChatMemberRepository: JpaRepository<ChatMember, Long> {
    fun getByChatAndUser(chat: Chat, user: User): ChatMember?

    fun existsByChatAndUser(chat: Chat, user: User): Boolean

    fun deleteAllByChat(chat: Chat): Int
}