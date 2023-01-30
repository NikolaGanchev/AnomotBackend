package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.exceptions.UserAlreadyExistsException
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.security.EmailVerified
import com.anomot.anomotbackend.security.MfaMethodValue
import com.anomot.anomotbackend.services.*
import com.anomot.anomotbackend.utils.AppealObjective
import com.anomot.anomotbackend.utils.AppealReason
import com.anomot.anomotbackend.utils.Constants
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import javax.validation.Valid
import javax.validation.constraints.Min

@RestController
@RequestMapping("/account")
class AuthController(private val userDetailsService: UserDetailsServiceImpl,
                    private val emailVerificationService: EmailVerificationService,
                    private val authenticationService: AuthenticationService,
                    private val mfaEmailTokenService: MfaEmailTokenService,
                    private val mfaRecoveryService: MfaRecoveryService,
                    private val passwordResetService: PasswordResetService,
                    private val loginInfoExtractorService: LoginInfoExtractorService,
                    private val userDeletionService: UserDeletionService,
                    private val userModerationService: UserModerationService) {

    @PostMapping("/new")
    fun registerUser(@RequestBody @Valid userRegisterDTO: UserRegisterDto): ResponseEntity<SelfUserDto> {
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

    @PostMapping("/email/verification/new")
    fun requestEmailVerification(authentication: Authentication): ResponseEntity<String> {
        val result = userDetailsService.sendVerificationEmail(
                userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails))
        return ResponseEntity(if (result) HttpStatus.CREATED else HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/password")
    fun changePassword(@RequestBody @Valid passwordChangeDto: PasswordChangeDto): ResponseEntity<String> {
        return try {
            userDetailsService.changePassword(oldPassword = passwordChangeDto.oldPassword,
                    newPassword = passwordChangeDto.newPassword)
            ResponseEntity(HttpStatus.OK)
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
        } catch (exception: BadCredentialsException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        } catch (exception: UserAlreadyExistsException) {
            ResponseEntity(HttpStatus.CONFLICT)
        }
    }

    @PutMapping("/username")
    fun changeUsername(@RequestBody @Valid usernameChangeDto: UsernameChangeDto): ResponseEntity<String> {
        userDetailsService.changeUsername(newUsername = usernameChangeDto.username)
        return ResponseEntity(HttpStatus.OK)
    }

    @PutMapping("/mfa/totp")
    @EmailVerified
    fun updateTotpStatus(@RequestBody @Valid mfaEnabledDto: MfaEnabledDto): ResponseEntity<TotpDto> {
        return if (mfaEnabledDto.isMfaEnabled) {
            val totpDto = userDetailsService.activateTotpMfa()
            ResponseEntity(totpDto, if (totpDto == null) HttpStatus.CONFLICT else HttpStatus.OK)
        } else {
            val success = userDetailsService.deactivateTotpMfa()
            ResponseEntity(if (success) HttpStatus.OK else HttpStatus.CONFLICT)
        }
    }

    @PutMapping("/mfa/email")
    @EmailVerified
    fun updateEmailMfaStatus(@RequestBody @Valid mfaEnabledDto: MfaEnabledDto): ResponseEntity<String> {
        val success = if (mfaEnabledDto.isMfaEnabled) {
            userDetailsService.activateEmailMfa()
        } else {
            userDetailsService.deactivateEmailMfa()
        }

        return ResponseEntity(if (success) HttpStatus.OK else HttpStatus.CONFLICT)
    }

    @DeleteMapping
    fun deleteAccount(@RequestBody @Valid deleteDto: AccountDeleteDto): ResponseEntity<String> {
        return try {
            userDeletionService.deleteUser(deleteDto.password)
            ResponseEntity(HttpStatus.OK)
        } catch (exception: BadCredentialsException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @PostMapping("/mfa/status")
    fun getMfaStatus(@RequestBody @Valid loginDto: LoginDto): ResponseEntity<MfaStatusDto> {
        val authentication = authenticationService.verifyAuthenticationWithoutMfa(loginDto.email!!, loginDto.password!!)

        if (authentication == null || authentication.principal == null) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }

        val user = (authentication.principal) as CustomUserDetails

        return ResponseEntity(MfaStatusDto(user.isMfaEnabled(), user.getMfaMethodsAsList()), HttpStatus.OK)
    }

    @PostMapping("/mfa/email/send")
    fun sendMfaEmail(@RequestBody @Valid loginDto: LoginDto): ResponseEntity<String> {
        val authentication = authenticationService.verifyAuthenticationWithoutMfa(loginDto.email!!, loginDto.password!!)

        if (authentication == null || authentication.principal == null) {
            return ResponseEntity<String>(HttpStatus.UNAUTHORIZED)
        }

        val user = (authentication.principal) as CustomUserDetails

        return if (user.isMfaEnabled() && user.hasMfaMethod(MfaMethodValue.EMAIL)) {
            mfaEmailTokenService.generateAndSendMfaEmail(user.id.toString(), user.username)
            ResponseEntity(HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.CONFLICT)
        }
    }

    @PutMapping("/mfa/recovery/codes")
    fun generateCodes(authentication: Authentication): ResponseEntity<MfaRecoveryCodesDto> {
        val user = (authentication.principal) as CustomUserDetails

        if (!user.isMfaEnabled()) {
            return ResponseEntity(HttpStatus.CONFLICT)
        }

        val codes = mfaRecoveryService.updateRecoveryCodes(user.id!!)

        return ResponseEntity(MfaRecoveryCodesDto(codes), HttpStatus.CREATED)
    }

    // Can also be accessed even if mfa is disabled in case recovery codes fail to be deleted on disabling mfa
    @DeleteMapping("/mfa/recovery/codes")
    fun deleteCodes(authentication: Authentication): ResponseEntity<String> {
        val user = (authentication.principal) as CustomUserDetails

        mfaRecoveryService.deleteRecoveryCodes(user.id!!)

        return ResponseEntity(HttpStatus.OK)
    }

    @GetMapping("/user")
    fun getUser(authentication: Authentication): ResponseEntity<SelfUserDto> {
        val user = ((authentication.principal) as CustomUserDetails)
        val userDto = user.getAsSelfDto().also {
            it.avatarId = userDetailsService.getAvatar(user.id!!)?.name.toString()
        }

        return ResponseEntity(userDto, HttpStatus.OK)
    }

    @PostMapping("/password/reset/new")
    fun requestPasswordReset(@RequestBody @Valid passwordResetRequestDto: PasswordResetRequestDto): ResponseEntity<String> {
        passwordResetService.handlePasswordResetCreation(
                passwordResetRequestDto.email,
                passwordResetService.generateExpiryDate(Constants.PASSWORD_RESET_TOKEN_LIFETIME, Instant.now()))

        return ResponseEntity(HttpStatus.OK)
    }

    @PutMapping("/password/reset")
    fun resetPassword(@RequestBody @Valid passwordResetDto: PasswordResetDto): ResponseEntity<String> {
        val successful = passwordResetService.resetPasswordIfValidCode(
                passwordResetDto.code,
                passwordResetDto.identifier,
                passwordResetDto.newPassword,
                Instant.now()
        )

        return ResponseEntity(if (successful) HttpStatus.OK else HttpStatus.UNAUTHORIZED)
    }

    @GetMapping("/security/logins")
    fun getLogins(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<LoginInfoDto>> {
        val pageRequest = PageRequest.of(page, Constants.LOGINS_PER_PAGE, Sort.by("date").descending())
        val user = (authentication.principal) as CustomUserDetails

        return ResponseEntity(loginInfoExtractorService.getByUser(user, pageRequest), HttpStatus.OK)
    }

    @GetMapping("/security/login")
    fun getLogin(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<LoginInfoDto> {
        val user = (authentication.principal) as CustomUserDetails
        val result = loginInfoExtractorService.getByUserAndId(user, id)

        return if (result != null) {
            ResponseEntity(result, HttpStatus.OK)
        } else ResponseEntity(HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/avatar")
    @EmailVerified
    fun uploadProfilePicture(@RequestParam("file") file: MultipartFile,
                             @RequestParam("left") left: Int,
                             @RequestParam("top") top: Int,
                             @RequestParam("cropSize") @Min(225) cropSize: Int,
                             authentication: Authentication): ResponseEntity<AvatarResultDto> {
        val user = userDetailsService.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = userDetailsService.changeAvatar(file, left, top, cropSize)

        return if (result == null) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } else if (result.hasNsfw) {
            ResponseEntity(AvatarResultDto(
                    result.avatarId,
                    result.hasNsfw,
                    userModerationService.generateAppealJwt(user, result.avatarId, AppealReason.NSFW_FOUND, AppealObjective.AVATAR)
            ), HttpStatus.NOT_ACCEPTABLE)
        } else {
            ResponseEntity(AvatarResultDto(result.avatarId, result.hasNsfw, null), HttpStatus.OK)
        }
    }
}