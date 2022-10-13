package com.anomot.anomotbackend

import com.anomot.anomotbackend.entities.Authority
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.EmailVerificationTokenRepository
import com.anomot.anomotbackend.repositories.UserRepository
import com.anomot.anomotbackend.security.Authorities
import com.anomot.anomotbackend.services.EmailVerificationService
import com.anomot.anomotbackend.services.LoginInfoExtractorService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant

@SpringBootTest
class EmailVerificationTests @Autowired constructor(
        @InjectMockKs
        private val emailVerificationService: EmailVerificationService,
) {
    @MockkBean
    private lateinit var emailVerificationTokenRepository: EmailVerificationTokenRepository
    @MockkBean
    private lateinit var userRepository: UserRepository
    @MockkBean
    private lateinit var loginInfoExtractorService: LoginInfoExtractorService

    @Test
    fun `When create token and non expired then verify`() {
        val code = emailVerificationService.generateVerificationCode()
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))

        val expiryDate = emailVerificationService.generateExpiryDate(1, Instant.now())
        val token = emailVerificationService.createEmailVerificationToken(code, user, expiryDate)

        every { emailVerificationTokenRepository.save(any()) } returns token
        every { emailVerificationTokenRepository.delete(any()) } returns Unit
        every { emailVerificationTokenRepository.findByVerificationCode(code) } returns token
        every { userRepository.setIsEmailVerifiedByEmail(true, user.email) } returns 1

        emailVerificationService.saveEmailVerificationToken(token)

        val isVerified = emailVerificationService.verifyEmail(code, Instant.now())

        assertThat(isVerified).isTrue
    }

    @Test
    fun `When create token and expired then don't verify`() {
        val code = emailVerificationService.generateVerificationCode()
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))

        val expiryDate = emailVerificationService.generateExpiryDate(-1, Instant.now())
        val token = emailVerificationService.createEmailVerificationToken(code, user, expiryDate)

        every { emailVerificationTokenRepository.save(any()) } returns token
        every { emailVerificationTokenRepository.delete(any()) } returns Unit
        every { emailVerificationTokenRepository.findByVerificationCode(code) } returns token
        every { userRepository.setIsEmailVerifiedByEmail(true, user.email) } returns 1

        emailVerificationService.saveEmailVerificationToken(token)

        val isVerified = emailVerificationService.verifyEmail(code, Instant.now().minusSeconds(60 * 24))

        assertThat(isVerified).isFalse
    }

}