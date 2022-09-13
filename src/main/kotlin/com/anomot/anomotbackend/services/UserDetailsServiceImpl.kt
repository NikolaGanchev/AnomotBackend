package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.dto.UserRegisterDto
import com.anomot.anomotbackend.entities.Authority
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.exceptions.UserAlreadyExistsException
import com.anomot.anomotbackend.repositories.AuthorityRepository
import com.anomot.anomotbackend.repositories.UserRepository
import com.anomot.anomotbackend.security.Authorities
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.utils.Constants
import org.hibernate.Hibernate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Service
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
    private lateinit var emailVerificationService: EmailVerificationService

    @Transactional
    override fun loadUserByUsername(email: String?): UserDetails {
        if (email == null) {
            throw NullPointerException("Email cannot be null")
        }
        val user = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("Email not found")
        Hibernate.initialize(user.authorities)
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

        return UserDto(email = savedUser.email, username = savedUser.username, listOf(Authorities.USER.roleName))
    }

    private fun sendVerificationEmail(user: User) {
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


