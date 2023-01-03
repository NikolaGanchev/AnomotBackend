package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.BattleQueuePost
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BattleQueueRepository: JpaRepository<BattleQueuePost, Long> {}