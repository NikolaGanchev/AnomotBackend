package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.NotificationDto
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.services.NotificationService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class NotificationController  @Autowired constructor(
        private val notificationService: NotificationService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {
    @GetMapping("/account/notifications")
    fun getNotifications(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<NotificationDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        return ResponseEntity(notificationService.getNotifications(user, page), HttpStatus.OK)
    }
}