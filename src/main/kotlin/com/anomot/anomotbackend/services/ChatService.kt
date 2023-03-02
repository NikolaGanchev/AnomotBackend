package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.ChatCreationDto
import com.anomot.anomotbackend.entities.Chat
import com.anomot.anomotbackend.entities.ChatMember
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.ChatRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

@Service
class ChatService @Autowired constructor(
        private val chatRepository: ChatRepository,
        private val chatMemberRepository: ChatMemberRepository,
        private val chatBanRepository: ChatBanRepository,
        private val chatMessageRepository: ChatMessageRepository
) {

    fun createChat(chatCreationDto: ChatCreationDto, creator: User): Chat {
        val chat = chatRepository.save(Chat(chatCreationDto.title,
                chatCreationDto.description,
                chatCreationDto.info,
                chatCreationDto.password))

        val member = chatMemberRepository.save(ChatMember(
                chat,
                creator,
                ChatRole.ADMIN,
                chatCreationDto.chatUsername
        ))

        return chat
    }

    fun delete() {
        //TODO
    }

    fun report() {
        //TODO
    }

    fun changeChatUsername() {
        //TODO
    }

    fun changeTitle() {
        //TODO
    }

    fun changeDescription() {
        //TODO
    }

    fun changeInfo() {
        //TODO
    }

    fun changePassword() {
        //TODO
    }

    fun banMember() {
        //TODO
    }

    fun getChatHistory(from: Date) {
        //TODO
    }

    fun publishMessage() {
        //TODO
    }
}