package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.exceptions.UserAlreadyExistsException
import com.anomot.anomotbackend.services.EmailVerificationService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
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
        return ResponseEntity(if (isVerified) HttpStatus.CREATED else HttpStatus.UNAUTHORIZED)
    }

    @PutMapping("/password")
    fun changePassword(@RequestBody @Valid passwordChangeDto: PasswordChangeDto): ResponseEntity<String> {
        return try {
            userDetailsService.changePassword(oldPassword = passwordChangeDto.oldPassword,
                    newPassword = passwordChangeDto.newPassword)
            ResponseEntity(HttpStatus.OK)
        } catch (exception: AccessDeniedException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        } catch (exception: BadCredentialsException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @PutMapping("/email")
    fun changeEmail(@RequestBody @Valid emailChangeDto: EmailChangeDto): ResponseEntity<String> {
        return try {
            userDetailsService.changeEmail(password = emailChangeDto.password,
                    newEmail = emailChangeDto.newEmail)
            ResponseEntity(HttpStatus.OK)
        } catch (exception: AccessDeniedException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        } catch (exception: BadCredentialsException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @PutMapping("/username")
    fun changeUsername(@RequestBody @Valid usernameChangeDto: UsernameChangeDto): ResponseEntity<String> {
        return try {
            userDetailsService.changeUsername(newUsername = usernameChangeDto.username)
            ResponseEntity(HttpStatus.OK)
        } catch (exception: AccessDeniedException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @PutMapping("/mfa/totp")
    fun activateTotp(): ResponseEntity<TotpDto> {
        return try {
            val totpDto = userDetailsService.activateTotpMfa()
            ResponseEntity(totpDto, if (totpDto == null) HttpStatus.CONFLICT else HttpStatus.OK)
        } catch (exception: AuthenticationException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @DeleteMapping("/mfa/totp")
    fun disableTotp(): ResponseEntity<String> {
        return try {
            val success = userDetailsService.deactivateTotpMfa()
            ResponseEntity(if (success) HttpStatus.OK else HttpStatus.CONFLICT)
        } catch (exception: AuthenticationException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @PutMapping("/mfa/email")
    fun activateEmailMfa(): ResponseEntity<String> {
        return try {
            val success = userDetailsService.activateEmailMfa()
            ResponseEntity(if (success) HttpStatus.OK else HttpStatus.CONFLICT)
        } catch (exception: AuthenticationException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @DeleteMapping("/mfa/email")
    fun disableEmailMfa(): ResponseEntity<String> {
        return try {
            val success = userDetailsService.deactivateEmailMfa()
            ResponseEntity(if (success) HttpStatus.OK else HttpStatus.CONFLICT)
        } catch (exception: AuthenticationException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @DeleteMapping()
    fun deleteAccount(@RequestBody @Valid deleteDto: AccountDeleteDto): ResponseEntity<String> {
        return try {
            userDetailsService.deleteUser(deleteDto.password)
            ResponseEntity(HttpStatus.OK)
        } catch (exception: AccessDeniedException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        } catch (exception: BadCredentialsException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }
}