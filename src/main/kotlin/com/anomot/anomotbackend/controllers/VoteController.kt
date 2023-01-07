package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.VoteDto
import com.anomot.anomotbackend.dto.VotedBattleDto
import com.anomot.anomotbackend.security.CustomUserDetails
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
    fun vote(@RequestBody @Valid voteDto: VoteDto, authentication: Authentication) {
        voteService.vote((authentication.principal) as CustomUserDetails, voteDto.jwt, voteDto.forId)
        //TODO
    }

    @GetMapping("/votes")
    fun getVoteHistory(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<VotedBattleDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        return ResponseEntity(voteService.getVoteHistory(user, page), HttpStatus.OK)
    }
}