package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.TotpDto
import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.dto.UserRegisterDto
import com.anomot.anomotbackend.entities.Authority
import com.anomot.anomotbackend.entities.MfaMethod
import com.anomot.anomotbackend.entities.MfaTotpSecret
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.exceptions.UserAlreadyExistsException
import com.anomot.anomotbackend.repositories.AuthorityRepository
import com.anomot.anomotbackend.repositories.MfaMethodRepository
import com.anomot.anomotbackend.repositories.UserRepository
import com.anomot.anomotbackend.security.Authorities
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.security.MfaMethodValue
import com.anomot.anomotbackend.utils.Constants
import com.bastiaanjansen.otp.TOTP
import org.hibernate.Hibernate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import javax.transaction.Transactional

@Service
class UserDetailsServiceImpl: UserDetailsService {

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var passwordEncoder: Argon2PasswordEncoder
    @Autowired
    private lateinit var authorityRepository: AuthorityRepository
    @Autowired
    private lateinit var mfaMethodRepository: MfaMethodRepository
    @Autowired
    private lateinit var emailVerificationService: EmailVerificationService
    @Autowired
    private lateinit var totpService: MfaTotpService
    @Autowired
    @Lazy
    private lateinit var authenticationService: AuthenticationService

    @Transactional
    override fun loadUserByUsername(email: String?): UserDetails {
        if (email == null) {
            throw NullPointerException("Email cannot be null")
        }
        val user = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("Email not found")
        Hibernate.initialize(user.authorities)
        Hibernate.initialize(user.mfaMethods)
        return CustomUserDetails(user)
    }

    fun createUser(userRegisterDto: UserRegisterDto): UserDto {
        val emailExists = userExists(userRegisterDto)

        if (emailExists) {
            throw UserAlreadyExistsException("User already exists")
        }

        val hashedPassword = passwordEncoder.encode(userRegisterDto.password)

        val userAuthority = authorityRepository.findByAuthority(Authorities.USER.roleName)
                ?: authorityRepository.save(Authority(Authorities.USER.roleName))

        val user = User(email = userRegisterDto.email,
                password = hashedPassword,
                username = userRegisterDto.username,
                arrayListOf(userAuthority))

        val savedUser = userRepository.save(user)

        sendVerificationEmail(savedUser)

        return UserDto(email = savedUser.email, username = savedUser.username, listOf(Authorities.USER.roleName), false)
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        val user = SecurityContextHolder.getContext().authentication
                ?: throw AccessDeniedException("No authentication present")

        if (!authenticationService.verifyAuthenticationWithoutMfa(user, oldPassword)) {
            throw BadCredentialsException("Bad credentials")
        }

        val hashedPassword = passwordEncoder.encode(newPassword)

        userRepository.setPassword(hashedPassword, (user.principal as CustomUserDetails).id!!)

        authenticationService.reAuthenticate(user)
    }

    fun changeUsername(newUsername: String) {
        val user = SecurityContextHolder.getContext().authentication
                ?: throw AccessDeniedException("No authentication present")

        userRepository.setUsername(newUsername, (user.principal as CustomUserDetails).id!!)

        authenticationService.reAuthenticate(user)
    }

    fun changeEmail(password: String, newEmail: String) {
        val user = SecurityContextHolder.getContext().authentication
                ?: throw AccessDeniedException("No authentication present")

        if (!authenticationService.verifyAuthenticationWithoutMfa(user, password)) {
            throw BadCredentialsException("Bad credentials")
        }

        userRepository.setEmail(newEmail, (user.principal as CustomUserDetails).id!!)

        authenticationService.reAuthenticate(user)
    }

    fun activateTotpMfa(): TotpDto? {
        val userAuth = SecurityContextHolder.getContext().authentication
                ?: throw AccessDeniedException("No authentication present")

        if (!(userAuth.principal as CustomUserDetails).isMfaEnabled()) {
            val user = userRepository.findByEmail(userAuth.name) ?: throw UsernameNotFoundException("Email not found")

            val mfaMethod = mfaMethodRepository.findByMethod(MfaMethodValue.TOTP.method) ?:
                mfaMethodRepository.save(MfaMethod(MfaMethodValue.TOTP.method))

            activateMfa(user, mfaMethod)

            val secret = totpService.generateSecret()
            val token = MfaTotpSecret(secret.toString(), user)
            totpService.saveCode(token)

            val totp = TOTP.Builder(secret)
                    .withPasswordLength(Constants.TOTP_PASSWORD_LENGTH)
                    .withPeriod(Duration.ofSeconds(Constants.TOTP_PERIOD))
                    .build()

            authenticationService.reAuthenticate(userAuth)

            return TotpDto(totp.secret.toString(), totp.getURI("Anomot%20${user.email}").toString())
        } else {
            return null
        }
    }

    fun deactivateTotpMfa(): Boolean {
        val userAuth = SecurityContextHolder.getContext().authentication
                ?: throw AccessDeniedException("No authentication present")

        if ((userAuth.principal as CustomUserDetails).isMfaEnabled()) {
            val user = userRepository.findByEmail(userAuth.name) ?: throw UsernameNotFoundException("Email not found")

            val mfaMethod = mfaMethodRepository.findByMethod(MfaMethodValue.TOTP.method) ?:
                mfaMethodRepository.save(MfaMethod(MfaMethodValue.TOTP.method))

            deactivateMfa(user, mfaMethod)
            authenticationService.reAuthenticate(userAuth)
            return true
        }
        return false
    }

    fun activateEmailMfa(): Boolean {
        val userAuth = SecurityContextHolder.getContext().authentication
                ?: throw AccessDeniedException("No authentication present")

        if (!(userAuth.principal as CustomUserDetails).isMfaEnabled()) {
            val user = userRepository.findByEmail(userAuth.name) ?: throw UsernameNotFoundException("Email not found")

            val mfaMethod = mfaMethodRepository.findByMethod(MfaMethodValue.EMAIL.method) ?:
                mfaMethodRepository.save(MfaMethod(MfaMethodValue.EMAIL.method))

            activateMfa(user, mfaMethod)
            authenticationService.reAuthenticate(userAuth)
            return true
        }
        return false
    }

    fun deactivateEmailMfa(): Boolean {
        val userAuth = SecurityContextHolder.getContext().authentication
                ?: throw AccessDeniedException("No authentication present")

        if ((userAuth.principal as CustomUserDetails).isMfaEnabled()) {
            val user = userRepository.findByEmail(userAuth.name) ?: throw UsernameNotFoundException("Email not found")

            val mfaMethod = mfaMethodRepository.findByMethod(MfaMethodValue.EMAIL.method) ?:
                mfaMethodRepository.save(MfaMethod(MfaMethodValue.EMAIL.method))

            deactivateMfa(user, mfaMethod)
            authenticationService.reAuthenticate(userAuth)
            return true
        }
        return false
    }

    fun deactivateMfa(user: User, mfaMethod: MfaMethod) {
        if (user.mfaMethods == null) {
            user.isMfaActive = false
            throw Exception("User has active Multi-factor authentication without any methods")
        }

        user.mfaMethods!!.remove(mfaMethod)

        if (user.mfaMethods!!.isEmpty()) {
            user.isMfaActive = false
        }
    }

    fun activateMfa(user: User, mfaMethod: MfaMethod) {
        user.isMfaActive = true
        if (user.mfaMethods == null) {
            user.mfaMethods = mutableListOf()
        }

        user.mfaMethods!!.add(mfaMethod)
    }

    fun deleteUser(password: String) {
        val user = SecurityContextHolder.getContext().authentication
                ?: throw AccessDeniedException("No authentication present")

        if (!authenticationService.verifyAuthenticationWithoutMfa(user, password)) {
            throw BadCredentialsException("Bad credentials")
        }

        userRepository.deleteById((user.principal as CustomUserDetails).id!!)
        totpService.deleteMfaSecret((user.principal as CustomUserDetails).id!!)
    }

    fun requestData(password: String) {
        //TODO("implement when other systems are done")
    }

    fun sendVerificationEmail(user: User) {
        val code = emailVerificationService.generateVerificationCode()
        val expiryDate = emailVerificationService.generateExpiryDate(
                Constants.EMAIL_VERIFICATION_TOKEN_LIFETIME,
                Instant.now())
        val token = emailVerificationService.createEmailVerificationToken(code, user, expiryDate)
        val savedToken = emailVerificationService.saveEmailVerificationToken(token)
        emailVerificationService.sendVerificationEmail(user, savedToken)
    }

    private fun userExists(userRegisterDto: UserRegisterDto): Boolean {
        return userRepository.findByEmail(userRegisterDto.email) != null
    }
}


