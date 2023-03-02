package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.ChatMessage
import org.springframework.data.jpa.repository.JpaRepository

interface ChatMessageRepository: JpaRepository<ChatMessage, Long>