package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.security.EmailVerified
import com.anomot.anomotbackend.services.ChatService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid

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
        return ResponseEntity(ChatDto(chat.title, chat.description, chat.info, chat.creationDate), HttpStatus.OK)
    }

    @DeleteMapping("")
    @EmailVerified
    fun deleteChat() {
    }

    @PostMapping("/join")
    @EmailVerified
    fun joinChat() {
    }

    @PostMapping("/username")
    @EmailVerified
    fun changeChatUsername() {
    }

    @PostMapping("/title")
    @EmailVerified
    fun changeTitle() {
    }

    @PostMapping("/description")
    @EmailVerified
    fun changeDescription() {
    }

    @PostMapping("/info")
    @EmailVerified
    fun changeInfo() {
    }

    @PostMapping("/password")
    @EmailVerified
    fun changePassword() {
    }

    @PostMapping("/ban")
    @EmailVerified
    fun banMember() {
    }

    @GetMapping("/messages")
    @EmailVerified
    fun getChatHistory() {
    }

    @PostMapping("/role/add")
    @EmailVerified
    fun addRole() {
    }

    @PostMapping("/role/remove")
    @EmailVerified
    fun removeRole() {
    }
}