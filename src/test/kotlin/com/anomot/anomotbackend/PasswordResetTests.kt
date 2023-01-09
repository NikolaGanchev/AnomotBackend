package com.anomot.anomotbackend

import com.anomot.anomotbackend.entities.Authority
import com.anomot.anomotbackend.entities.PasswordResetToken
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.PasswordResetTokenRepository
import com.anomot.anomotbackend.repositories.UserRepository
import com.anomot.anomotbackend.security.Authorities
import com.anomot.anomotbackend.services.LoginInfoExtractorService
import com.anomot.anomotbackend.services.PasswordResetService
import com.anomot.anomotbackend.utils.TimeUtils
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import java.time.Instant
import java.util.*

@SpringBootTest(properties = ["vote.jwt.private-key=1edbc7fb7bae1628f085f6db259b7de40b887157aea732c7c31f18403a562338"])
class PasswordResetTests @Autowired constructor(
        @InjectMockKs
        private val passwordResetService: PasswordResetService
) {
    @MockkBean
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    @MockkBean
    private lateinit var userRepository: UserRepository
    @MockkBean
    private lateinit var passwordEncoder: Argon2PasswordEncoder
    @MockkBean
    private lateinit var loginInfoExtractorService: LoginInfoExtractorService

    @BeforeAll
    fun setup() {
        every { passwordEncoder.encode(any()) } returns ""
    }

    @Test
    fun `When verify and successful then return true`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))
        user.id = 5

        val code = "a".repeat(64)
        val identifier = UUID.randomUUID().toString()

        val token = PasswordResetToken(code, identifier, user, TimeUtils.generateFutureDate(60))

        every { passwordResetTokenRepository.findByIdentifier(any()) } returns token
        every { passwordResetTokenRepository.delete(any()) } returns Unit
        every { passwordEncoder.matches(any(), any()) } returns true
        every { userRepository.setPassword(any(), any()) } returns 1

        val result = passwordResetService.resetPasswordIfValidCode(code, identifier, user.password, Instant.now())

        assertThat(result).isTrue
    }

    @Test
    fun `When verify and missing identifier then return false`() {
        val code = "a".repeat(64)
        val identifier = UUID.randomUUID().toString()

        every { passwordResetTokenRepository.findByIdentifier(any()) } returns null
        every { passwordEncoder.matches(any(), any()) } returns true
        every { passwordEncoder.encode(any()) } returns ""

        val result = passwordResetService.resetPasswordIfValidCode(code, identifier, "password12$", Instant.now())

        assertThat(result).isFalse
    }

    @Test
    fun `When verify and wrong code then return false`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))

        val code = "a".repeat(64)
        val identifier = UUID.randomUUID().toString()

        val token = PasswordResetToken(code, identifier, user, TimeUtils.generateFutureDate(60))

        every { passwordResetTokenRepository.findByIdentifier(any()) } returns token
        every { passwordEncoder.matches(any(), any()) } returns false
        every { userRepository.setPassword(any(), any()) } returns 1

        val result = passwordResetService.resetPasswordIfValidCode(code, identifier, user.password, Instant.now())

        assertThat(result).isFalse
    }

    @Test
    fun `When verify and expired code then return false`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))

        val code = "a".repeat(64)
        val identifier = UUID.randomUUID().toString()

        val token = PasswordResetToken(code, identifier, user, TimeUtils.generatePastMinutesAgo(60))

        every { passwordResetTokenRepository.findByIdentifier(any()) } returns token
        every { passwordEncoder.matches(any(), any()) } returns true
        every { userRepository.setPassword(any(), any()) } returns 1

        val result = passwordResetService.resetPasswordIfValidCode(code, identifier, user.password, Instant.now())

        assertThat(result).isFalse
    }
}