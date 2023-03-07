package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.services.ChatService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
internal class ChatController @Autowired constructor(
        private val chatService: ChatService
) {

}