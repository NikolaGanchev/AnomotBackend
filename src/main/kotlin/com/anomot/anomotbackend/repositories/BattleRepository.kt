package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Battle
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BattleRepository: JpaRepository<Battle, Long> {}