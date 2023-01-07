package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.dto.UserReference
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.services.FollowService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/account")
class FollowController(
        private val followService: FollowService,
        private val userDetailsService: UserDetailsServiceImpl
) {

    @PostMapping("/follow")
    fun follow(@RequestBody @Valid userReference: UserReference, authentication: Authentication): ResponseEntity<String> {
        return try {
            followService.follow(
                    userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails),
                    userDetailsService.getUserReferenceFromId(userReference.id.toLong()))

            ResponseEntity(HttpStatus.OK)
        } catch (numberFormatException: NumberFormatException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @PostMapping("/unfollow")
    fun unfollow(@RequestBody @Valid userReference: UserReference, authentication: Authentication): ResponseEntity<String> {
        return try {
            followService.unfollow(
                    userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails),
                    userDetailsService.getUserReferenceFromId(userReference.id.toLong()))

            ResponseEntity(HttpStatus.OK)
        } catch (numberFormatException: NumberFormatException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @GetMapping("/followers")
    fun getFollowers(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<UserDto>> {
        val result = followService.getFollowers(
                userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails),
                page)

        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/followed")
    fun getFollowed(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<UserDto>> {
        val result = followService.getFollowed(
                userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails),
                page)

        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/followers/count")
    fun getFollowerCount(authentication: Authentication): ResponseEntity<Long> {
        val result = followService.getFollowerCount(
                userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails))

        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/followed/count")
    fun getFollowedCount(authentication: Authentication): ResponseEntity<Long> {
        val result = followService.getFollowedCount(
                userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails))

        return ResponseEntity(result, HttpStatus.OK)
    }

}