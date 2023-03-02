package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Chat
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRepository: JpaRepository<Chat, Long>