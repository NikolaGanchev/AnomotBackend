package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.utils.ChatEventType
import com.anomot.anomotbackend.utils.ChatRoles
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Service
import java.util.Date
import javax.transaction.Transactional
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication

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

    val ADD_ROLE: (member: ChatMember, role: ChatRoles) -> Boolean = {member, role ->
        chatRoleRepository.save(ChatRole(member, role))

        true
    }
    val REMOVE_ROLE: (member: ChatMember, role: ChatRoles) -> Boolean = {member, role ->
        chatRoleRepository.deleteByChatMemberAndRole(member, role) > 0
    }

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
                ChatRole(member, ChatRoles.ADMIN),
                ChatRole(member, ChatRoles.OWNER)))

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

    fun join(chatJoinDto: ChatJoinDto, user: User): ChatMemberDto? {
        val chat = getChatReferenceFromIdUnsafe(chatJoinDto.chatId) ?: return null

        if (chatMemberRepository.existsByChatAndUser(chat, user)) return null

        if (chat.password != null && !passwordEncoder.matches(chat.password, chatJoinDto.password)) return null

        val member = chatMemberRepository.save(ChatMember(
                chat,
                user,
                chatJoinDto.username
        ))

        val role = chatRoleRepository.save(ChatRole(member, ChatRoles.USER))
        return ChatMemberDto(
                userDetailsServiceImpl.getAsDto(user),
                member.chatUsername,
                listOf(role.role.name),
                member.id.toString())
    }

    fun report() {
        //TODO
    }

    @Transactional
    fun changeChatUsername(changeChatNameDto: ChangeChatNameDto, user: User): Boolean {
        val chat = getChatReferenceFromIdUnsafe(changeChatNameDto.chatId) ?: return false
        val member = chatMemberRepository.getByChatAndUser(chat, user) ?: return false

        member.chatUsername = changeChatNameDto.username
        publishSystemMessage(ChatEventType.USERNAME_CHANGE, chat, member)

        return true
    }

    @Transactional
    fun changeTitle(changeChatTitleDto: ChangeChatTitleDto, user: User): Boolean {
        val (chat, member, roles) = loadInChat(changeChatTitleDto.chatId, user) ?: return false

        if (roles.none { it.role == ChatRoles.ADMIN }) return false

        if (chat.password == null || passwordEncoder.matches(chat.password, changeChatTitleDto.password)) {
            chat.title = changeChatTitleDto.title
            return true
        }

        return false
    }

    @Transactional
    fun changeDescription(changeChatDescriptionDto: ChangeChatDescriptionDto, user: User): Boolean {
        val (chat, member, roles) = loadInChat(changeChatDescriptionDto.chatId, user) ?: return false

        if (roles.none { it.role == ChatRoles.ADMIN }) return false

        if (chat.password == null || passwordEncoder.matches(chat.password, changeChatDescriptionDto.password)) {
            chat.description = changeChatDescriptionDto.description
            return true
        }

        return false
    }

    @Transactional
    fun changeInfo(changeChatInfoDto: ChangeChatInfoDto, user: User): Boolean {
        val (chat, member, roles) = loadInChat(changeChatInfoDto.chatId, user) ?: return false

        if (roles.none { it.role == ChatRoles.ADMIN }) return false

        if (chat.password == null || passwordEncoder.matches(chat.password, changeChatInfoDto.password)) {
            chat.info = changeChatInfoDto.info
            return true
        }

        return false
    }

    @Transactional
    fun changePassword(chatId: String,
                       oldPassword: String?,
                       newPassword: String?,
                       user: User): Boolean {
        val (chat, member, roles) = loadInChat(chatId, user) ?: return false

        if (roles.none { it.role == ChatRoles.ADMIN }) return false

        return if (chat.password == null || passwordEncoder.matches(chat.password, oldPassword)) {
            if (newPassword == null) chat.password = null
            else {
                chat.password = passwordEncoder.encode(newPassword)
            }
            true
        } else false
    }

    fun banMember(chatMemberBanDto: ChatMemberBanDto, user: User): Boolean {
        val memberToBan = getChatMemberReferenceFromIdUnsafe(chatMemberBanDto.chatMemberToBanId) ?: return false
        val (chat, admin, roles) = loadInChat(chatMemberBanDto.chatId, user) ?: return false
        if (memberToBan.chat.id != chat.id) return false

        val memberToBanRoles = chatRoleRepository.getByChatMember(memberToBan)

        if (roles.none { it.role == ChatRoles.ADMIN || it.role == ChatRoles.MODERATOR }) return false
        if (memberToBanRoles.any { it.role == ChatRoles.ADMIN }) return false

        val ban = chatBanRepository.save(ChatBan(memberToBan, chatMemberBanDto.reason, admin, chatMemberBanDto.until))
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

    fun editRole(chatMemberRoleChangeDto: ChatMemberRoleChangeDto, admin: User, editor: (member: ChatMember, role: ChatRoles) -> Boolean): Boolean {
        val memberToPromote = getChatMemberReferenceFromIdUnsafe(chatMemberRoleChangeDto.chatMember) ?: return false

        if (chatMemberRoleChangeDto.role == ChatRoles.OWNER) return false

        val (chat, member, roles) = loadInChat(chatMemberRoleChangeDto.chatId, admin) ?: return false

        if (roles.none { it.role == ChatRoles.ADMIN}) return false

        if (chatMemberRoleChangeDto.role == ChatRoles.ADMIN && roles.none { it.role == ChatRoles.OWNER }) return false

        if (chat.password != null && !passwordEncoder.matches(chat.password, chatMemberRoleChangeDto.password)) return false

        return editor(memberToPromote, chatMemberRoleChangeDto.role)
    }

    fun transferOwnership(chatId: String,
                          chatMemberToMakeOwner: String,
                          chatPassword: String?,
                          userPassword: String,
                          ownerAuthentication: Authentication): Boolean {
        val owner = userDetailsServiceImpl.getUserReferenceFromDetails((ownerAuthentication.principal) as CustomUserDetails)
        val memberToMakeOwner = getChatMemberReferenceFromIdUnsafe(chatMemberToMakeOwner) ?: return false

        val (chat, member, roles) = loadInChat(chatId, owner) ?: return false

        if (roles.none { it.role == ChatRoles.OWNER}) return false

        if (chat.password != null && !passwordEncoder.matches(chat.password, chatPassword)) return false

        if (userDetailsServiceImpl.verifyAuthenticationWithoutMfa(ownerAuthentication, userPassword) == null) {
            throw BadCredentialsException("Bad credentials")
        }

        val role = chatRoleRepository.save(ChatRole(member, ChatRoles.OWNER))
        chatRoleRepository.deleteByChatMemberAndRole(memberToMakeOwner, ChatRoles.OWNER)

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