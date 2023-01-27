package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import com.anomot.anomotbackend.services.UserModerationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class UserModerationController(
        private val userModerationService: UserModerationService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {

    @PostMapping("/user/report")
    fun reportUser(@RequestBody @Valid userReportDto: UserReportDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = userModerationService.report(userReportDto, user)

        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/appeal")
    fun appeal(@RequestBody @Valid appealUploadDto: AppealUploadDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = userModerationService.appeal(appealUploadDto, user)

        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/appeals")
    fun getAppeals(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<AppealDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        return ResponseEntity(userModerationService.getAppeals(user, page), HttpStatus.OK)
    }

    @GetMapping("/appeals")
    fun getAppeal(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<AppealDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = userModerationService.getAppeal(user, id)

        return if (result != null) {
            ResponseEntity(result, HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @GetMapping("/user/report")
    fun getUserReport(@RequestParam("id") userId: String, authentication: Authentication): ResponseEntity<ReportDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = userModerationService.getReport(user, userId)

        return if (result != null) {
            ResponseEntity(result, HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

}