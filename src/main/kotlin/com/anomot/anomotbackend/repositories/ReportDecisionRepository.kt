package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.ReportDecision
import org.springframework.data.jpa.repository.JpaRepository

interface ReportDecisionRepository: JpaRepository<ReportDecision, Long>