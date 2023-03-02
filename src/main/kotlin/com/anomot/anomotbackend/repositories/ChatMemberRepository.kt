package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.ChatMember
import org.springframework.data.jpa.repository.JpaRepository

interface ChatMemberRepository: JpaRepository<ChatMember, Long>