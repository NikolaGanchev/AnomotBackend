package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.dto.ChatMessageWithUser
import com.anomot.anomotbackend.entities.Chat
import com.anomot.anomotbackend.entities.ChatMessage
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Date

interface ChatMessageRepository: JpaRepository<ChatMessage, Long> {
    fun deleteAllByMemberChat(chat: Chat): Int

    @Query("select new com.anomot.anomotbackend.dto.ChatMessageWithUser(cm, " +
            "(select r.role from ChatRole r where r.chatMember=cm.member), " +
            "(select f.followed from Follow f where f.followed = cm.member.user and f.follower = ?3)) " +
            "from ChatMessage cm where cm.creationDate > ?2 and cm.member.chat = ?1")
    fun getMessagesByChatAndFromDate(chat: Chat, from: Date, fromUser: User, pageable: Pageable): List<ChatMessageWithUser>
}
