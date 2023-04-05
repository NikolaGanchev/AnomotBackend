package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.utils.*
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageType
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.user.SimpUserRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.transaction.Transactional

@Service
class ChatService @Autowired constructor(
        private val chatRepository: ChatRepository,
        private val chatMemberRepository: ChatMemberRepository,
        private val chatBanRepository: ChatBanRepository,
        private val chatMessageRepository: ChatMessageRepository,
        private val chatRoleRepository: ChatRoleRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val passwordEncoder: Argon2PasswordEncoder,
        private val redisContainer: RedisMessageListenerContainer,
        private val redisTemplate: StringRedisTemplate,
        private val followService: FollowService,
        private val userModerationService: UserModerationService,
        private val reportRepository: ReportRepository,
        @Lazy
        private val simpUserRegistry: SimpUserRegistry,
        @Lazy
        private val simpMessagingTemplate: SimpMessagingTemplate
) {

    private val destinations: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    private val subscriptions: ConcurrentHashMap<String,  ConcurrentHashMap.KeySetView<String, Boolean>> = ConcurrentHashMap()
    private val listeners: ConcurrentHashMap<String, MessageListener> = ConcurrentHashMap()
    private val mapper = ObjectMapper().also {
        it.setSerializationInclusion(JsonInclude.Include.ALWAYS)
        it.registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
    }

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

    fun report(chatReportDto: ChatReportDto, user: User): Boolean {
        val chat = getChatReferenceFromIdUnsafe(chatReportDto.chatId)

        val reportReason = ReportReason.from(chatReportDto.reason)

        return userModerationService.report(reportReason,
                ReportType.POST,
                chatReportDto.other,
                user, null, null, null, null, chat,
                Constants.CHAT_REPORT_COOLDOWN)
    }

    fun getReport(user: User, chatId: String): ReportDto? {
        val chat = getChatReferenceFromIdUnsafe(chatId) ?: return null

        val reports = reportRepository.getAllByReporterAndReportTicketChat(user, chat)

        val singleReportedDtos = reports.map {
            SingleReportDto(it.reportReason, it.other)
        }.toTypedArray()

        return ReportDto(singleReportedDtos, ReportType.POST)
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
    fun changeChatUsername(changeChatNameDto: ChangeChatNameDto, member: ChatMember): Boolean {
        member.chatUsername = changeChatNameDto.username
        publishSystemMessage(ChatEventType.USERNAME_CHANGE, member.chat, member)

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

    fun unbanMember(chatMemberUnbanDto: ChatMemberUnbanDto, user: User): Boolean {
        val memberToUnban = getChatMemberReferenceFromIdUnsafe(chatMemberUnbanDto.chatMemberToUnbanId) ?: return false
        val (chat, admin, roles) = loadInChat(chatMemberUnbanDto.chatId, user) ?: return false
        if (memberToUnban.chat.id != chat.id) return false

        val memberToUnbanRoles = chatRoleRepository.getByChatMember(memberToUnban)

        if (roles.none { it.role == ChatRoles.ADMIN || it.role == ChatRoles.MODERATOR }) return false
        if (memberToUnbanRoles.any { it.role == ChatRoles.ADMIN }) return false

        val banIdLong = chatMemberUnbanDto.banToRemoveId.toLongOrNull() ?: return false
        if (!chatBanRepository.existsById(banIdLong)) return false
        val ban = chatBanRepository.getReferenceById(banIdLong)
        if (ban.chatMember.id != memberToUnban.id) return false

        chatBanRepository.delete(ban)

        publishSystemMessage(ChatEventType.UNBAN, chat, memberToUnban)

        return true
    }

    fun getBanStatus(member: ChatMember, fromUser: User, page: Int): List<ChatBanDto>? {
        val (chat, admin, roles) = loadInChat(member.chat.id.toString(), fromUser) ?: return null

        if (roles.none { it.role == ChatRoles.ADMIN || it.role == ChatRoles.MODERATOR }) return null

        val bans = chatBanRepository.getByChatMember(member, PageRequest.of(page, 10, Sort.by("creationDate").descending()))

        return bans.map {
            val rolesBannedBy = chatRoleRepository.getByChatMember(it.bannedBy)
            return@map ChatBanDto(
                    it.creationDate,
                    it.until,
                    ChatMemberDto(
                            if (it.bannedBy.user != null && followService.canSeeOtherUser(fromUser, it.bannedBy.user!!))
                                userDetailsServiceImpl.getAsDto(it.bannedBy.user!!) else null,
                            it.bannedBy.chatUsername,
                            rolesBannedBy.map { role -> role.role.name },
                            it.bannedBy.id.toString()
                    ),
                    it.reason,
                    it.id.toString()
            )
        }
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
                        if (it.chatMessage.member != null) it.chatMessage.member!!.chatUsername else "",
                        it.roles.map { role -> role.name },
                        if (it.chatMessage.member != null) it.chatMessage.member!!.id.toString() else ""),
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

        val dto = ChatMessageDto(ChatMemberDto(
                userDetailsServiceImpl.getAsDto(user),
                member.chatUsername,
                roles.map { it.role.name },
                member.id.toString()),
                savedMessage.message,
                false,
                savedMessage.creationDate)

        sendMessageToUsers(dto, chat)

        return dto
    }

    fun publishSystemMessage(message: ChatEventType, chat: Chat, chatMember: ChatMember?): ChatMessageDto {
        val savedMessage = chatMessageRepository.save(ChatMessage(
                chatMember,
                message.name,
                true
        ))

        val chatMemberDto = if (chatMember != null) ChatMemberDto(
                if (chatMember.user != null) userDetailsServiceImpl.getAsDto(chatMember.user!!) else null,
                chatMember.chatUsername,
                chatRoleRepository.getByChatMember(chatMember).map { it.role.name },
                chatMember.id.toString()) else null

        val dto = ChatMessageDto(chatMemberDto, message.name, true, savedMessage.creationDate)

        sendMessageToUsers(dto, chat)

        return dto
    }

    fun publishSystemMessageLiteral(message: String, chat: Chat, chatMember: ChatMember?): ChatMessageDto {
        val savedMessage = chatMessageRepository.save(ChatMessage(
                chatMember,
                message,
                true
        ))

        val chatMemberDto = if (chatMember != null) ChatMemberDto(
                if (chatMember.user != null) userDetailsServiceImpl.getAsDto(chatMember.user!!) else null,
                chatMember.chatUsername,
                chatRoleRepository.getByChatMember(chatMember).map { it.role.name },
                chatMember.id.toString()) else null

        val dto = ChatMessageDto(chatMemberDto, message, true, savedMessage.creationDate)

        sendMessageToUsers(dto, chat)

        return dto
    }

    fun sendMessageToUsers(message: ChatMessageDto, chat: Chat) {
        redisTemplate.convertAndSend("/user/chat/${chat.id.toString()}", mapper.writeValueAsString(message))
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


    fun deleteChat(chatId: String,
                   chatPassword: String?,
                   userPassword: String,
                   ownerAuthentication: Authentication): Boolean {
        val owner = userDetailsServiceImpl.getUserReferenceFromDetails((ownerAuthentication.principal) as CustomUserDetails)

        val (chat, member, roles) = loadInChat(chatId, owner) ?: return false

        if (roles.none { it.role == ChatRoles.OWNER}) return false

        if (chat.password != null && !passwordEncoder.matches(chat.password, chatPassword)) return false

        if (userDetailsServiceImpl.verifyAuthenticationWithoutMfa(ownerAuthentication, userPassword) == null) {
            throw BadCredentialsException("Bad credentials")
        }

        return deleteChat(chat)
    }

    @Transactional
    fun deleteChat(chat: Chat): Boolean {
        chatBanRepository.deleteAllByChatMemberChat(chat)
        chatMessageRepository.deleteAllByMemberChat(chat)
        chatRoleRepository.deleteAllByChatMemberChat(chat)
        chatMemberRepository.deleteAllByChat(chat)
        chatRepository.delete(chat)

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

    fun canSeeMessagesInChat(authentication: Authentication, chatId: String): Boolean {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val chat = getChatReferenceFromIdUnsafe(chatId) ?: return false
        val member = chatMemberRepository.getByChatAndUser(chat, user) ?: return false

        return true
    }

    fun stripTopic(topic: String): String {
        if (topic.startsWith("/user")) {
            return topic.substring(5)
        }
        return topic
    }

    @Synchronized fun subscribeToRedisTopic(topic: String) {
        if (listeners.containsKey(topic)) return
        val realTopic = stripTopic(topic)
        val messageListener = MessageListenerAdapter(object : MessageListener {
            override fun onMessage(message: Message, pattern: ByteArray?) {

                val chatMessageDto = mapper.readValue(message.toString(), ChatMessageDto::class.java)
                val chatMessageAnonymized = chatMessageDto.copy(member = chatMessageDto.member?.copy(user = null))

                simpUserRegistry.findSubscriptions { it.destination == topic }.forEach {
                    val headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE)
                    headerAccessor.sessionId = it.session.id
                    headerAccessor.setLeaveMutable(true)

                    if (!chatMessageDto.isSystem && chatMessageDto.member != null) {
                        val receiver = userDetailsServiceImpl.getUserReferenceFromDetails(
                                (it.session.user.principal as Authentication).principal as CustomUserDetails)
                        val sender = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(chatMessageDto.member.user?.id.toString())
                        if (sender != null && followService.canSeeOtherUser(receiver, sender)) {
                            simpMessagingTemplate.convertAndSendToUser(it.session.id, realTopic, chatMessageDto, headerAccessor.messageHeaders)
                        } else {
                            simpMessagingTemplate.convertAndSendToUser(it.session.id, realTopic, chatMessageAnonymized, headerAccessor.messageHeaders)
                        }
                    }
                }
            }
        })

        listeners[topic] = messageListener
        redisContainer.addMessageListener(messageListener, ChannelTopic(topic))
    }

    @Scheduled(fixedRate = 1000 * 30)
    fun unsubscribeFromRedisTopics() {
        subscriptions.forEach {
            if (it.value.size == 0) {
                if (listeners.containsKey(it.key)) {
                    redisContainer.removeMessageListener(listeners[it.key]!!)
                    listeners.remove(it.key)
                }
            }
        }
    }

    @EventListener
    public fun onSubscribeEvent(event: SessionSubscribeEvent) {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails(
                (event.user as Authentication).principal as CustomUserDetails)
        val destination = event.message.headers["simpDestination"]
        val userId = user.id.toString()

        if (subscriptions.containsKey(destination)) {
            subscriptions[destination]!!.add(userId)
        } else {
            subscriptions[destination.toString()] = ConcurrentHashMap.newKeySet()
        }

        subscribeToRedisTopic(destination.toString())
        destinations[event.message.headers["simpSessionId"].toString()] = destination.toString()

        val chatId = if (destination.toString().startsWith("/user/chat/")) destination.toString().substring(11) else return
        val (chat, member, roles) = loadInChat(chatId, user) ?: return
        publishSystemMessage(ChatEventType.JOIN, chat, member)
    }

    @EventListener
    public fun onUnsubscribeEvent(event: SessionUnsubscribeEvent) {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((event.user as Authentication).principal as CustomUserDetails)
        val simpSession = event.message.headers["simpSessionId"]
        val destination = destinations[simpSession]
        subscriptions[destination]?.remove(user.id.toString())
        destinations.remove(simpSession)

        val chatId = if (destination.toString().startsWith("/user/chat/")) destination.toString().substring(11) else return
        val (chat, member, roles) = loadInChat(chatId, user) ?: return
        publishSystemMessage(ChatEventType.LEAVE, chat, member)
    }

    @EventListener
    public fun onConnectionEndEvent(event: SessionDisconnectEvent) {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((event.user as Authentication).principal as CustomUserDetails)

        subscriptions.forEach {
            val userId = user.id.toString()
            if (it.value.contains(userId)) {
                it.value.remove(userId)
            }
        }

        val destination = destinations[event.message.headers["simpSessionId"]]
        destinations.remove(event.message.headers["simpSessionId"])

        val chatId = if (destination.toString().startsWith("/user/chat/")) destination.toString().substring(11) else return
        val (chat, member, roles) = loadInChat(chatId, user) ?: return
        publishSystemMessage(ChatEventType.LEAVE, chat, member)
    }
}