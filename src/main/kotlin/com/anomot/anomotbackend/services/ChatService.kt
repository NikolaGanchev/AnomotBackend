package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.ChatEventType
import com.anomot.anomotbackend.utils.ChatRoles
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Service
import java.util.Date
import jakarta.transaction.Transactional

@Service
class ChatService @Autowired constructor(
        private val chatRepository: ChatRepository,
        private val chatMemberRepository: ChatMemberRepository,
        private val chatBanRepository: ChatBanRepository,
        private val chatMessageRepository: ChatMessageRepository,
        private val chatRoleRepository: ChatRoleRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val passwordEncoder: Argon2PasswordEncoder
) {

    fun createChat(chatCreationDto: ChatCreationDto, creator: User): Chat {
        val chat = chatRepository.save(Chat(chatCreationDto.title,
                chatCreationDto.description,
                chatCreationDto.info,
                if (chatCreationDto.password == null) null else passwordEncoder.encode(chatCreationDto.password)))

        val member = chatMemberRepository.save(ChatMember(
                chat,
                creator,
                chatCreationDto.chatUsername
        ))

        val role = chatRoleRepository.saveAll(listOf(
                ChatRole(member, ChatRoles.USER),
                ChatRole(member, ChatRoles.ADMIN)))

        return chat
    }

    @Transactional
    fun delete(chatId: String, password: String?, user: User): Boolean {
        val (chat, member, roles) = loadInChat(chatId, user) ?: return false

        if (roles.none { it.role == ChatRoles.ADMIN }) return false
        if (chat.password != null && !passwordEncoder.matches(chat.password, password)) return false

        chatBanRepository.deleteAllByChatMemberChat(chat)
        chatMessageRepository.deleteAllByMemberChat(chat)
        chatRoleRepository.deleteAllByChatMemberChat(chat)
        chatMemberRepository.deleteAllByChat(chat)

        return true
    }

    fun join(chatJoinDto: ChatJoinDto, user: User): ChatMember? {
        val chat = getChatReferenceFromIdUnsafe(chatJoinDto.chatId) ?: return null

        if (chatMemberRepository.existsByChatAndUser(chat, user)) return null

        if (chat.password != null && !passwordEncoder.matches(chat.password, chatJoinDto.password)) return null

        val member = chatMemberRepository.save(ChatMember(
                chat,
                user,
                chatJoinDto.username
        ))

        val role = chatRoleRepository.save(ChatRole(member, ChatRoles.USER))
        return member
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

        return if (passwordEncoder.matches(chat.password, oldPassword)) {
            if (newPassword == null) chat.password = null
            else {
                chat.password = passwordEncoder.encode(newPassword)
            }
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
        publishSystemMessage(ChatEventType.BAN, chat, memberToBan)

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
                        if (it.user != null) userDetailsServiceImpl.getAsDto(it.user!!) else null,
                        it.chatMessage.member.chatUsername,
                        it.roles.map { role -> role.name },
                        it.chatMessage.member.id.toString()),
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
                roles.map { it.role.name },
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

    fun addRole(chatId: String,
                roleToChangeTo: ChatRoles,
                chatMemberToPromoteId: String,
                password: String?,
                admin: User): Boolean {
        val memberToPromote = getChatMemberReferenceFromIdUnsafe(chatMemberToPromoteId) ?: return false
        val (chat, member, roles) = loadInChat(chatId, admin) ?: return false

        if (roles.none { it.role == ChatRoles.ADMIN}) return false

        if (chat.password != null && !passwordEncoder.matches(chat.password, password)) return false

        val role = chatRoleRepository.save(ChatRole(member, roleToChangeTo))

        return true
    }

    fun removeRole(chatId: String,
                   roleToRemove: ChatRoles,
                   chatMemberToDemoteId: String,
                   password: String?,
                   admin: User): Boolean {
        val memberToDemote = getChatMemberReferenceFromIdUnsafe(chatMemberToDemoteId) ?: return false
        val (chat, member, roles) = loadInChat(chatId, admin) ?: return false

        if (roles.none { it.role == ChatRoles.ADMIN}) return false

        if (chat.password != null && !passwordEncoder.matches(chat.password, password)) return false

        return chatRoleRepository.deleteByChatMemberAndRole(memberToDemote, roleToRemove) > 0
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