package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.VoteDto
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.services.VoteService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/vote")
class VoteController(
        private val voteService: VoteService
) {

    @PostMapping
    fun vote(@RequestBody @Valid voteDto: VoteDto, authentication: Authentication) {
        voteService.vote((authentication.principal) as CustomUserDetails, voteDto.jwt, voteDto.forId)
        //TODO
    }
}