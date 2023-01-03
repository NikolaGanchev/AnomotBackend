package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Vote
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VoteRepository: JpaRepository<Vote, Long> {
}