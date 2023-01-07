package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.repositories.VoteRepository
import com.anomot.anomotbackend.security.CustomUserDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class VoteService @Autowired constructor(
        private val voteRepository: VoteRepository
) {

    fun vote(user: CustomUserDetails, voteJWT: String, forId: String) {
        //TODO
    }
}