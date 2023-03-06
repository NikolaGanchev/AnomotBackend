package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.ChatEventType
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

    fun banMember(chatId: String, chatMemberToBan: String, user: User, until: Date, reason: String): Boolean {
        val memberToBan = getChatMemberReferenceFromIdUnsafe(chatMemberToBan) ?: return false
        val (chat, admin, roles) = loadInChat(chatId, user) ?: return false
        if (memberToBan.chat.id != chat.id) return false

        val memberToBanRoles = chatRoleRepository.getByChatMember(memberToBan)

        if (roles.none { it.role == ChatRoles.ADMIN || it.role == ChatRoles.MODERATOR }) return false
        if (memberToBanRoles.any { it.role == ChatRoles.ADMIN }) return false

        val ban = chatBanRepository.save(ChatBan(memberToBan, reason, admin, until))
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
                        it.chatMessage.member.chatUsername, it.chatMessage.member.id.toString()),
                        it.chatMessage.message,
                        it.chatMessage.isSystem,
                        it.chatMessage.creationDate
                    )
        }
    }

    fun publishMessage(message: String, chatId: String, user: User): ChatMessageDto? {
        val (chat, member, roles) = loadInChat(chatId, user) ?: return null

        val bans = chatBanRepository.getByChatMember(member, PageRequest.of(0, 1, Sort.by("until").descending()))
        if (bans.isNotEmpty() && bans[0].until.after(Date())) return null

        val savedMessage = chatMessageRepository.save(ChatMessage(member, message))

        return ChatMessageDto(ChatMemberDto(
                userDetailsServiceImpl.getAsDto(user),
                member.chatUsername,
                member.id.toString()),
                savedMessage.message,
                false,
                savedMessage.creationDate)
    }

    fun publishSystemMessage(message: ChatEventType, chat: Chat, chatMember: ChatMember): ChatMessage {
        return chatMessageRepository.save(ChatMessage(
                chatMember,
                message.name,
                true
        ))
    }

    fun changeRole(chatId: String,
                roleToChangeTo: ChatRoles,
                chatMemberToPromoteId: String,
                admin: User): Boolean {
        val (chat, member, roles) = loadInChat(chatId, admin) ?: return false

        if (roles.none { it.role == ChatRoles.ADMIN}) return false


        //TODO
        return true
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

    fun getChatMemberReferenceFromIdUnsafe(chatMemberId: String): ChatMember? {
        val id = chatMemberId.toLongOrNull() ?: return null
        return if (chatMemberRepository.existsById(id)) chatMemberRepository.getReferenceById(id) else null
    }
}