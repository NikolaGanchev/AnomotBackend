package com.anomot.anomotbackend

import com.anomot.anomotbackend.entities.Authority
import com.anomot.anomotbackend.entities.MfaEmailToken
import com.anomot.anomotbackend.entities.MfaTotpSecret
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.MfaEmailCodeRepository
import com.anomot.anomotbackend.repositories.MfaTotpSecretRepository
import com.anomot.anomotbackend.security.Authorities
import com.anomot.anomotbackend.services.MfaEmailTokenService
import com.anomot.anomotbackend.services.MfaTotpService
import com.bastiaanjansen.otp.TOTP
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest
class MfaTests @Autowired constructor(
        @InjectMockKs
        val mfaEmailTokenService: MfaEmailTokenService,
        @InjectMockKs
        val mfaTotpService: MfaTotpService
){
    @MockkBean
    private lateinit var mfaEmailCodeRepository: MfaEmailCodeRepository
    @MockkBean
    private lateinit var mfaTotpSecretRepository: MfaTotpSecretRepository
    @MockK
    private lateinit var totp: TOTP

    @Test
    fun `When email code is valid then return true`() {
        val code = "65abv7"
        val id = 5
        val expectedMfaToken = MfaEmailToken(id = id.toString(), code)

        every { mfaEmailCodeRepository.findById(id.toString()) } returns Optional.of(expectedMfaToken)

        val isValid = mfaEmailTokenService.verifyMfaCode(id.toString(), code)

        assertThat(isValid).isTrue
    }

    @Test
    fun `When email code doesn't exist then return false`() {
        val code = "65abv7"
        val id = 5

        every { mfaEmailCodeRepository.findById(id.toString()) } returns Optional.ofNullable(null)

        val isValid = mfaEmailTokenService.verifyMfaCode(id.toString(), code)

        assertThat(isValid).isFalse
    }

    @Test
    fun `When email code is invalid then return false`() {
        val code = "65abv7"
        val id = 5
        val expectedMfaToken = MfaEmailToken(id = id.toString(), code)

        every { mfaEmailCodeRepository.findById(id.toString()) } returns Optional.of(expectedMfaToken)

        val isValid = mfaEmailTokenService.verifyMfaCode(id.toString(), "54bnOt")

        assertThat(isValid).isFalse
    }

    @Test
    fun `When totp code is valid then return true`() {
        val secret = mfaTotpService.generateSecret()
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))
        val mfaTotpSecret = MfaTotpSecret(secret.toString(), user)

        every { mfaTotpSecretRepository.findByEmail(user.email) } returns mfaTotpSecret
        every { totp.verify(any()) } returns true

        val isValid = mfaTotpService.verifyMfaCode(user.email, "906748", totp)

        assertThat(isValid).isTrue
    }

    @Test
    fun `When totp code doesn't exist then return false`() {
        val email = "example@example.com"

        every { mfaTotpSecretRepository.findByEmail(email) } returns null

        val isValid = mfaTotpService.verifyMfaCode(email, "906748")

        assertThat(isValid).isFalse
    }

    @Test
    fun `When totp code is invalid then return false`() {
        val secret = mfaTotpService.generateSecret()
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))
        val mfaTotpSecret = MfaTotpSecret(secret.toString(), user)

        every { mfaTotpSecretRepository.findByEmail(user.email) } returns mfaTotpSecret
        every { totp.verify(any()) } returns false

        val isValid = mfaTotpService.verifyMfaCode(user.email, "906748", totp)

        assertThat(isValid).isFalse
    }

}