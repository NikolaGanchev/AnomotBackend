package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Chat
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ChatRepository: JpaRepository<Chat, Long> {

    @Query("from Chat c")
    fun getAll(pageable: Pageable): List<Chat>

    @Query("select c, (select count(cm) from ChatMember cm where cm.chat = c) as c1 from Chat c order by c1 desc")
    fun getAllByMostMembers(pageable: Pageable): List<Chat>

    @Query("select c, (select count(cm) from ChatMember cm where cm.chat = c) as c1 from Chat c order by c1 asc")
    fun getAllByLeastMembers(pageable: Pageable): List<Chat>

    @Query("select cm.chat from ChatMember cm where cm.user = ?1")
    fun getAllByMember(user: User, pageable: Pageable): List<Chat>

    @Query("select cm.chat, (select count(cm) from ChatMember cm where cm.chat = cm.chat) as c1" +
            " from ChatMember cm where cm.user = ?1 " +
            "order by c1 desc")
    fun getAllByMemberMostMembers(user: User, pageable: Pageable): List<Chat>

    @Query("select cm.chat, (select count(cm) from ChatMember cm where cm.chat = cm.chat) as c1" +
            " from ChatMember cm where cm.user = ?1 " +
            "order by c1 asc")
    fun getAllByMemberLeastMembers(user: User, pageable: Pageable): List<Chat>
}