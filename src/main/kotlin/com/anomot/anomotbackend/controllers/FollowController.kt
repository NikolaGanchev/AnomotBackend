package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.security.EmailVerified
import com.anomot.anomotbackend.services.FollowService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping
class FollowController(
        private val followService: FollowService,
        private val userDetailsService: UserDetailsServiceImpl
) {

    @PostMapping("/account/follow")
    @EmailVerified
    fun follow(@RequestParam("id") userId: String, authentication: Authentication): ResponseEntity<String> {
        val result = followService.follow(
                userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails),
                userDetailsService.getUserReferenceFromIdUnsafe(userId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST))

        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/account/unfollow")
    @EmailVerified
    fun unfollow(@RequestParam("id") userId: String, authentication: Authentication): ResponseEntity<String> {
        val result = followService.unfollow(
                userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails),
                userDetailsService.getUserReferenceFromIdUnsafe(userId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST))

        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/account/followers")
    fun getFollowers(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<UserDto>> {
        val result = followService.getFollowers(
                userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails),
                page)

        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/account/followed")
    fun getFollowed(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<UserDto>> {
        val result = followService.getFollowed(
                userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails),
                page)

        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/account/followers/count")
    fun getSelfFollowerCount(authentication: Authentication): ResponseEntity<Long> {
        val result = followService.getFollowerCount(
                userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails))

        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/account/followed/count")
    fun getSelfFollowedCount(authentication: Authentication): ResponseEntity<Long> {
        val result = followService.getFollowedCount(
                userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails))

        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/followers/count")
    fun getFollowerCount(@RequestParam("id") userId: String, authentication: Authentication): ResponseEntity<Long> {
        val user = userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val otherUser = userDetailsService.getUserReferenceFromIdUnsafe(userId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        if (!followService.follows(user, otherUser)) return ResponseEntity(HttpStatus.FORBIDDEN)

        val result = followService.getFollowerCount(otherUser)

        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/followed/count")
    fun getFollowedCount(@RequestParam("id") userId: String, authentication: Authentication): ResponseEntity<Long> {
        val user = userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val otherUser = userDetailsService.getUserReferenceFromIdUnsafe(userId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        if (!followService.follows(user, otherUser)) return ResponseEntity(HttpStatus.FORBIDDEN)

        val result = followService.getFollowedCount(otherUser)

        return ResponseEntity(result, HttpStatus.OK)
    }

}