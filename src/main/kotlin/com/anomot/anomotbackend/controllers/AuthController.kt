package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.EmailVerifyDto
import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.dto.UserRegisterDto
import com.anomot.anomotbackend.exceptions.UserAlreadyExistsException
import com.anomot.anomotbackend.services.EmailVerificationService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import javax.validation.Valid

@RestController
@RequestMapping("/account")
class AuthController(private val userDetailsService: UserDetailsServiceImpl,
                    private val emailVerificationService: EmailVerificationService) {

    @PostMapping("/new")
    fun registerUser(@RequestBody @Valid userRegisterDTO: UserRegisterDto): ResponseEntity<UserDto> {
        return try {
            val user = userDetailsService.createUser(userRegisterDTO)
            ResponseEntity(user, HttpStatus.CREATED)
        } catch (userAlreadyExistsException: UserAlreadyExistsException) {
            ResponseEntity(HttpStatus.CONFLICT)
        }
    }

    @PostMapping("/email/verify")
    fun verifyEmail(@RequestBody @Valid emailVerifyDto: EmailVerifyDto): ResponseEntity<String> {
        val isVerified = emailVerificationService.verifyEmail(emailVerifyDto.verificationCode, Instant.now())
        return ResponseEntity(if (isVerified) HttpStatus.CREATED else HttpStatus.FORBIDDEN)
    }
}