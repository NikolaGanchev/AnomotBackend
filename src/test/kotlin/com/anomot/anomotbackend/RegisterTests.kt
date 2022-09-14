package com.anomot.anomotbackend

import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.dto.UserRegisterDto
import com.anomot.anomotbackend.entities.Authority
import com.anomot.anomotbackend.entities.EmailVerificationToken
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.exceptions.UserAlreadyExistsException
import com.anomot.anomotbackend.repositories.UserRepository
import com.anomot.anomotbackend.security.Authorities
import com.anomot.anomotbackend.services.EmailVerificationService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import java.time.Instant
import java.util.*

@SpringBootTest
class RegisterTests {
    @MockkBean
    private lateinit var userRepository: UserRepository
    @MockkBean
    private lateinit var passwordEncoder: Argon2PasswordEncoder
    @MockkBean
    private lateinit var emailVerificationService: EmailVerificationService
    @Autowired
    @InjectMockKs
    private lateinit var userDetailsService: UserDetailsServiceImpl

    @BeforeEach
    fun setup() {
        val authority = Authority(Authorities.USER.roleName)
        val emailVerificationToken = EmailVerificationToken(
                "code",
                User("example@test.com", "password12$", "Georgi", mutableListOf(authority)),
                Date.from(Instant.now()))

        every { emailVerificationService.createEmailVerificationToken(any(), any(), any()) } returns emailVerificationToken
        every { emailVerificationService.generateVerificationCode() } returns "code"
        every { emailVerificationService.saveEmailVerificationToken(any()) } returns emailVerificationToken
        every { emailVerificationService.generateExpiryDate(any(), any()) } returns  Date.from(Instant.now())
        every { emailVerificationService.sendVerificationEmail(any(), any()) } returns Unit
    }

    @Test
    fun `When create user then return User`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))
        val expectedResult = UserDto("example@test.com", "Georgi", mutableListOf(authority.authority), false)

        every { passwordEncoder.encode(user.password) } returns user.password
        every { userRepository.save(any()) } returns user
        every { userRepository.findByEmail(any()) } returns null

        val result = userDetailsService.createUser(UserRegisterDto(user.email, user.password, user.username))

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `When user exists then throw exception`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))

        every { passwordEncoder.encode(user.password) } returns user.password
        every { userRepository.save(any()) } returns user
        every { userRepository.findByEmail(any()) } returns user

        assertThrows<UserAlreadyExistsException>("User already exists") {
            userDetailsService.createUser(UserRegisterDto(user.email, user.password, user.username))
        }
    }

}