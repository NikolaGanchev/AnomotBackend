package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.security.EmailVerified
import com.anomot.anomotbackend.services.ChatService
import com.anomot.anomotbackend.services.ChatSort
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid
import org.springframework.security.authentication.BadCredentialsException

@RestController
@RequestMapping("/chat")
internal class ChatController @Autowired constructor(
        private val chatService: ChatService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {
    @PostMapping("/new")
    @EmailVerified
    fun createChat(@RequestBody @Valid chatCreationDto: ChatCreationDto, authentication: Authentication): ResponseEntity<ChatDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val chat = chatService.createChat(chatCreationDto, user)
        return ResponseEntity(ChatDto(chat.title,
                chat.description,
                chat.info,
                chat.creationDate,
                chat.password != null,
                chat.id.toString()), HttpStatus.OK)
    }

    @DeleteMapping()
    @EmailVerified
    fun deleteChat(@RequestBody @Valid chatDeleteDto: ChatDeletionDto, authentication: Authentication): ResponseEntity<String> {
        return try {
            if (chatService.deleteChat(chatDeleteDto.chatId,
                            chatDeleteDto.chatPassword,
                            chatDeleteDto.accountPassword,
                            authentication)) {
                ResponseEntity(HttpStatus.OK)
            } else ResponseEntity(HttpStatus.BAD_REQUEST)
        } catch(e: BadCredentialsException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @PostMapping("/join")
    @EmailVerified
    fun joinChat(@RequestBody @Valid chatJoinDto: ChatJoinDto, authentication: Authentication): ResponseEntity<ChatMemberDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = chatService.join(chatJoinDto, user) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PutMapping("/username")
    @EmailVerified
    fun changeChatUsername(@RequestBody @Valid changeChatNameDto: ChangeChatNameDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = chatService.changeChatUsername(changeChatNameDto, user)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/title")
    @EmailVerified
    fun changeTitle(@RequestBody @Valid changeChatTitleDto: ChangeChatTitleDto, authentication: Authentication): ResponseEntity<String>  {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = chatService.changeTitle(changeChatTitleDto, user)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/description")
    @EmailVerified
    fun changeDescription(@RequestBody @Valid changeChatDescriptionDto: ChangeChatDescriptionDto, authentication: Authentication): ResponseEntity<String>  {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = chatService.changeDescription(changeChatDescriptionDto, user)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/info")
    @EmailVerified
    fun changeInfo(@RequestBody @Valid changeChatInfoDto: ChangeChatInfoDto, authentication: Authentication): ResponseEntity<String>  {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = chatService.changeInfo(changeChatInfoDto, user)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/password")
    @EmailVerified
    fun changePassword(@RequestBody @Valid changeChatPasswordDto: ChangeChatPasswordDto, authentication: Authentication): ResponseEntity<String>  {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = chatService.changePassword(changeChatPasswordDto.chatId,
                changeChatPasswordDto.oldPassword,
                changeChatPasswordDto.newPassword,
                user)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/ban")
    fun banMember(@RequestBody @Valid banChatMemberBanDto: ChatMemberBanDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = chatService.banMember(banChatMemberBanDto, user)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/unban")
    fun unbanMember(@RequestBody @Valid banChatMemberUnbanDto: ChatMemberUnbanDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = chatService.unbanMember(banChatMemberUnbanDto, user)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/bans")
    @EmailVerified
    fun getBanHistory(@RequestParam("memberId") memberId: String,
                      @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<ChatBanDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val member = chatService.getChatMemberReferenceFromIdUnsafe(memberId)
                ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val result = chatService.getBanStatus(member, user, page)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/messages")
    @EmailVerified
    fun getChatHistory(@RequestParam("id") chatId: String,
                       @RequestParam("from") from: Date,
                       @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<ChatMessageDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = chatService.getChatHistory(chatId, user, from, page) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PutMapping("/role")
    @EmailVerified
    fun addRole(@RequestBody @Valid chatMemberRoleChangeDto: ChatMemberRoleChangeDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = chatService.editRole(chatMemberRoleChangeDto, user, chatService.ADD_ROLE)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @DeleteMapping("/role")
    @EmailVerified
    fun removeRole(@RequestBody @Valid chatMemberRoleChangeDto: ChatMemberRoleChangeDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = chatService.editRole(chatMemberRoleChangeDto, user, chatService.REMOVE_ROLE)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/ownership/transfer")
    @EmailVerified
    fun transferOwnership(@RequestBody @Valid ownershipTransferDto: OwnershipTransferDto, authentication: Authentication): ResponseEntity<String> {
        return try {
            val result = chatService.transferOwnership(ownershipTransferDto.chatId,
                    ownershipTransferDto.chatMember,
                    ownershipTransferDto.chatPassword,
                    ownershipTransferDto.accountPassword,
                    authentication)
            ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
        } catch (e: BadCredentialsException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @PostMapping("/{chatId}")
    @EmailVerified
    fun publishMessage(@PathVariable chatId: String, @RequestBody @Valid chatMessageDto: ChatMessageSendDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val message = chatService.publishMessage(chatMessageDto.text, chatId, user)
        return ResponseEntity(if (message != null) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/report")
    @EmailVerified
    fun reportChat(@RequestBody @Valid chatReportDto: ChatReportDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = chatService.report(chatReportDto, user)

        return ResponseEntity(if (result) HttpStatus.CREATED else HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/report")
    fun getChatReport(@RequestParam("chatId") chatId: String,
                        authentication: Authentication): ResponseEntity<ReportDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = chatService.getReport(user, chatId)

        return if (result != null) {
            ResponseEntity(result, HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @GetMapping("/ban")
    fun getBanStatus(@RequestParam("chatId") chatId: String,
                     @RequestParam("page") page: Int,
                     authentication: Authentication): ResponseEntity<List<ChatBanDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val (_, member, _) = chatService.loadInChat(chatId, user) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val result = chatService.getBanStatus(member, user, page) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping
    fun getChats(@RequestParam("page") page: Int,
                 @RequestParam("sortBy") sort: ChatSort,
                 authentication: Authentication): ResponseEntity<List<ChatDto>> {
        return ResponseEntity(chatService.getChats(page, sort), HttpStatus.OK)
    }

    @GetMapping("/joined")
    fun getJoinedChats(@RequestParam("page") page: Int,
                       @RequestParam("sortBy") sort: ChatSort,
                       authentication: Authentication): ResponseEntity<List<ChatDto>>  {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        return ResponseEntity(chatService.getChatsJoined(page, sort, user), HttpStatus.OK)
    }

    @GetMapping("/member")
    fun getSelfMember(@RequestParam("chatId") chatId: String, authentication: Authentication): ResponseEntity<ChatMemberDto>  {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = chatService.getChatMember(chatId, user) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        return ResponseEntity(result, HttpStatus.OK)
    }
}