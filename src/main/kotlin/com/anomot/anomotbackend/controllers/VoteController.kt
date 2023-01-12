package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.VoteDto
import com.anomot.anomotbackend.dto.VotedBattleDto
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.security.EmailVerified
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import com.anomot.anomotbackend.services.VoteService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping
class VoteController(
        private val voteService: VoteService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {

    @PostMapping("/vote")
    @EmailVerified
    fun vote(@RequestBody @Valid voteDto: VoteDto, authentication: Authentication): ResponseEntity<VotedBattleDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = voteService.vote(user, voteDto.jwt, voteDto.forId)
        return if (result != null) {
            ResponseEntity(result, HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.FORBIDDEN)
        }
    }

    @GetMapping("/account/votes")
    fun getVoteHistory(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<VotedBattleDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        return ResponseEntity(voteService.getVoteHistory(user, page), HttpStatus.OK)
    }
}