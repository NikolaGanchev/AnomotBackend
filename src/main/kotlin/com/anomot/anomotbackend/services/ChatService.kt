package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.ChatCreationDto
import com.anomot.anomotbackend.dto.ChatJoinDto
import com.anomot.anomotbackend.dto.ChatMemberDto
import com.anomot.anomotbackend.dto.ChatMessageDto
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.ChatRoles
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.util.Date
import javax.transaction.Transactional

@Service
class ChatService @Autowired constructor(
        private val chatRepository: ChatRepository,
        private val chatMemberRepository: ChatMemberRepository,
        private val chatBanRepository: ChatBanRepository,
        private val chatMessageRepository: ChatMessageRepository,
        private val chatRoleRepository: ChatRoleRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {

    fun createChat(chatCreationDto: ChatCreationDto, creator: User): Chat {
        val chat = chatRepository.save(Chat(chatCreationDto.title,
                chatCreationDto.description,
                chatCreationDto.info,
                chatCreationDto.password))

        val member = chatMemberRepository.save(ChatMember(
                chat,
                creator,
                chatCreationDto.chatUsername
        ))

        return chat
    }

    @Transactional
    fun delete(chatId: String, user: User): Boolean {
        val (chat, member, roles) = loadInChat(chatId, user) ?: return false

        if (roles.none { it.role == ChatRoles.ADMIN }) return false

        chatBanRepository.deleteAllByChatMemberChat(chat)
        chatMessageRepository.deleteAllByMemberChat(chat)
        chatRoleRepository.deleteAllByChatMemberChat(chat)
        chatMemberRepository.deleteAllByChat(chat)

        return true
    }

    fun join(chatJoinDto: ChatJoinDto, user: User): ChatMember? {
        val chat = getChatReferenceFromIdUnsafe(chatJoinDto.chatId) ?: return null

        if (chatMemberRepository.existsByChatAndUser(chat, user)) return null

        return chatMemberRepository.save(ChatMember(
                chat,
                user,
                chatJoinDto.username
        ))
    }

    fun report() {
        //TODO
    }

    @Transactional
    fun changeChatUsername(chatJoinDto: ChatJoinDto, user: User): Boolean {
        val chat = getChatReferenceFromIdUnsafe(chatJoinDto.chatId) ?: return false
        val member = chatMemberRepository.getByChatAndUser(chat, user) ?: return false

        // TODO Send system message about username change
        member.chatUsername = chatJoinDto.username
        return true
    }

    @Transactional
    fun changeTitle(chatId: String, newTitle: String, user: User): Boolean {
        val (chat, member, roles) = loadInChat(chatId, user) ?: return false

        if (roles.none { it.role == ChatRoles.ADMIN }) return false

        chat.title = newTitle

        return true
    }

    @Transactional
    fun changeDescription(chatId: String, newDescription: String, user: User): Boolean {
        val (chat, member, roles) = loadInChat(chatId, user) ?: return false

        if (roles.none { it.role == ChatRoles.ADMIN }) return false

        chat.description = newDescription

        return true
    }

    @Transactional
    fun changeInfo(chatId: String, newInfo: String, user: User): Boolean {
        val (chat, member, roles) = loadInChat(chatId, user) ?: return false

        if (roles.none { it.role == ChatRoles.ADMIN }) return false

        chat.info = newInfo

        return true
    }

    @Transactional
    fun changePassword(chatId: String,
                       oldPassword: String?,
                       newPassword: String?,
                       user: User): Boolean {
        val (chat, member, roles) = loadInChat(chatId, user) ?: return false

        if (roles.none { it.role == ChatRoles.ADMIN }) return false

        return if (chat.password == oldPassword) {
            chat.password = newPassword
            true
        } else false
    }

    fun banMember(chatId: String, user: User, userToBan: User, until: Date, reason: String): Boolean {
        val (chat, member, roles) = loadInChat(chatId, user) ?: return false

        val memberToBan = chatMemberRepository.getByChatAndUser(chat, userToBan) ?: return false
        val memberToBanRoles = chatRoleRepository.getByChatMember(memberToBan)

        if (roles.none { it.role == ChatRoles.ADMIN }) return false
        if (memberToBanRoles.any { it.role == ChatRoles.ADMIN }) return false

        val ban = ChatBan(memberToBan, reason, member, until)
        return true
    }

    fun getChatHistory(chatId: String, fromUser: User, from: Date, page: Int): List<ChatMessageDto>? {
        val chat = getChatReferenceFromIdUnsafe(chatId) ?: return null
        return chatMessageRepository.
            getMessagesByChatAndFromDate(
                    chat,
                    from,
                    fromUser,
                    PageRequest.of(page, 15,
                            Sort.by("creationDate").descending()))
                .map {
                    ChatMessageDto(ChatMemberDto(
                        if (it.user != null) userDetailsServiceImpl.getAsDto(it.user) else null,
                        it.chatMessage.member.chatUsername),
                        it.chatMessage.message,
                        it.chatMessage.creationDate
                    )
        }
    }

    fun publishMessage() {
        //TODO
    }

    fun publishSystemMessage() {
        //TODO
    }

    fun promote() {
        //TODO
    }

    fun loadInChat(chatId: String, user: User): Triple<Chat, ChatMember, List<ChatRole>>? {
        val chat = getChatReferenceFromIdUnsafe(chatId) ?: return null
        val member = chatMemberRepository.getByChatAndUser(chat, user) ?: return null
        val roles = chatRoleRepository.getByChatMember(member)

        return Triple(chat, member, roles)
    }


    fun getChatReferenceFromIdUnsafe(chatId: String): Chat? {
        val id = chatId.toLongOrNull() ?: return null
        return if (chatRepository.existsById(id)) chatRepository.getReferenceById(id) else null
    }
}