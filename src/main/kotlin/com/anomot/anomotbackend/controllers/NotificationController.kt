package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.NotificationDto
import com.anomot.anomotbackend.dto.NotificationMarkDto
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.services.NotificationService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

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

    @PostMapping("/account/notifications/mark")
    fun markNotifications(@RequestBody @Valid notificationMarkDto: NotificationMarkDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = if(notificationMarkDto.notificationIds.size == 1) {
            notificationService.toggleReadNotification(user, notificationMarkDto.notificationIds[0], notificationMarkDto.isRead)
        } else {
            notificationService.toggleReadNotifications(user, notificationMarkDto.notificationIds, notificationMarkDto.isRead)
        }

        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }
}