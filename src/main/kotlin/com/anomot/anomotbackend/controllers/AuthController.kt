package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.exceptions.UserAlreadyExistsException
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.security.MfaMethodValue
import com.anomot.anomotbackend.services.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
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
                    private val emailVerificationService: EmailVerificationService,
                    private val authenticationService: AuthenticationService,
                    private val mfaEmailTokenService: MfaEmailTokenService,
                    private val mfaRecoveryService: MfaRecoveryService) {

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
    fun updateTotpStatus(@RequestBody @Valid mfaEnabledDto: MfaEnabledDto): ResponseEntity<TotpDto> {
        return try {
            if (mfaEnabledDto.isMfaEnabled) {
                val totpDto = userDetailsService.activateTotpMfa()
                ResponseEntity(totpDto, if (totpDto == null) HttpStatus.CONFLICT else HttpStatus.OK)
            } else {
                val success = userDetailsService.deactivateTotpMfa()
                ResponseEntity(if (success) HttpStatus.OK else HttpStatus.CONFLICT)
            }
        } catch (exception: AuthenticationException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @PutMapping("/mfa/email")
    fun updateEmailMfaStatus(@RequestBody @Valid mfaEnabledDto: MfaEnabledDto): ResponseEntity<String> {
        return try {
            val success = if (mfaEnabledDto.isMfaEnabled) {
                userDetailsService.activateEmailMfa()
            } else {
                userDetailsService.deactivateEmailMfa()
            }

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

    @PostMapping("/mfa/email/methods")
    fun getMfaMethods(@RequestBody @Valid loginDto: LoginDto): ResponseEntity<MfaMethodsDto> {
        return try {
            val authentication = authenticationService.verifyAuthenticationWithoutMfa(loginDto.email, loginDto.password)

            if (authentication == null || authentication.principal == null) {
                return ResponseEntity(HttpStatus.UNAUTHORIZED)
            }

            val user = (authentication.principal) as CustomUserDetails

            if (user.isMfaEnabled() && user.getMfaMethodsAsList() != null) {
                ResponseEntity(MfaMethodsDto(user.getMfaMethodsAsList()!!), HttpStatus.OK)
            } else {
                ResponseEntity(HttpStatus.CONFLICT)
            }
        } catch (exception: AuthenticationException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @PostMapping("/mfa/status")
    fun getMfaEnabled(@RequestBody @Valid loginDto: LoginDto): ResponseEntity<MfaEnabledDto> {
        return try {
            val authentication = authenticationService.verifyAuthenticationWithoutMfa(loginDto.email, loginDto.password)

            if (authentication == null || authentication.principal == null) {
                return ResponseEntity(HttpStatus.UNAUTHORIZED)
            }

            val user = (authentication.principal) as CustomUserDetails

            ResponseEntity(MfaEnabledDto(user.isMfaEnabled()), HttpStatus.OK)
        } catch (exception: AuthenticationException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @PostMapping("/mfa/email/send")
    fun sendMfaEmail(@RequestBody @Valid loginDto: LoginDto): ResponseEntity<String> {
        return try {
            val authentication = authenticationService.verifyAuthenticationWithoutMfa(loginDto.email, loginDto.password)

            if (authentication == null || authentication.principal == null) {
                return ResponseEntity<String>(HttpStatus.UNAUTHORIZED)
            }

            val user = (authentication.principal) as CustomUserDetails

            if (user.isMfaEnabled() && user.hasMfaMethod(MfaMethodValue.EMAIL)) {
                mfaEmailTokenService.generateAndSendMfaEmail(user.id.toString())
                ResponseEntity(HttpStatus.OK)
            } else {
                ResponseEntity(HttpStatus.CONFLICT)
            }
        } catch (exception: AuthenticationException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @PutMapping("/mfa/recovery/codes")
    fun generateCodes(authentication: Authentication?): ResponseEntity<MfaRecoveryCodesDto> {
        return if (authentication != null && authentication.principal != null) {
            val user = (authentication.principal) as CustomUserDetails

            if (!user.isMfaEnabled()) {
                return ResponseEntity(HttpStatus.CONFLICT)
            }

            val codes = mfaRecoveryService.updateRecoveryCodes(user.id!!)

            ResponseEntity(MfaRecoveryCodesDto(codes), HttpStatus.CREATED)
        } else {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    // Can also be accessed even if mfa is disabled in case recovery codes fail to be deleted on disabling mfa
    @DeleteMapping("/mfa/recovery/codes")
    fun deleteCodes(authentication: Authentication?): ResponseEntity<String> {
        return if (authentication != null && authentication.principal != null) {
            val user = (authentication.principal) as CustomUserDetails

            mfaRecoveryService.deleteRecoveryCodes(user.id!!)

            ResponseEntity(HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }
}