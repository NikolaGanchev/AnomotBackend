package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.AppealDecision
import org.springframework.data.jpa.repository.JpaRepository

interface AppealDecisionRepository: JpaRepository<AppealDecision, Long>