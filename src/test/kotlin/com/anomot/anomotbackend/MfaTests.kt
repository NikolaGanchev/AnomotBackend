package com.anomot.anomotbackend

import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.MfaEmailCodeRepository
import com.anomot.anomotbackend.repositories.MfaRecoveryCodeRepository
import com.anomot.anomotbackend.repositories.MfaTotpSecretRepository
import com.anomot.anomotbackend.repositories.UserRepository
import com.anomot.anomotbackend.security.Authorities
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.services.*
import com.bastiaanjansen.otp.TOTPGenerator
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import java.util.*

@SpringBootTest(properties = ["jwt.private-key=1edbc7fb7bae1628f085f6db259b7de40b887157aea732c7c31f18403a562338"])
class MfaTests @Autowired constructor(
        @InjectMockKs
        val mfaEmailTokenService: MfaEmailTokenService,
        @InjectMockKs
        val mfaTotpService: MfaTotpService,
        @InjectMockKs
        val mfaRecoveryService: MfaRecoveryService,

        val passwordEncoder: Argon2PasswordEncoder
){
    @MockkBean
    private lateinit var mfaEmailCodeRepository: MfaEmailCodeRepository
    @MockkBean
    private lateinit var mfaTotpSecretRepository: MfaTotpSecretRepository
    @MockkBean
    private lateinit var mfaRecoveryCodeRepository: MfaRecoveryCodeRepository
    @MockkBean
    private lateinit var userRepository: UserRepository
    @MockkBean
    private lateinit var loginInfoExtractorService: LoginInfoExtractorService
    @MockK
    private lateinit var totp: TOTPGenerator

    @Test
    fun `When email code is valid then return true`() {
        val code = "65abv7"
        val id = 5
        val expectedMfaToken = MfaEmailToken(id = id.toString(), code)

        every { mfaEmailCodeRepository.findById(id.toString()) } returns Optional.of(expectedMfaToken)

        val isValid = mfaEmailTokenService.verifyMfaCode(id.toLong(), code)

        assertThat(isValid).isTrue
    }

    @Test
    fun `When email code doesn't exist then return false`() {
        val code = "65abv7"
        val id = 5

        every { mfaEmailCodeRepository.findById(id.toString()) } returns Optional.ofNullable(null)

        val isValid = mfaEmailTokenService.verifyMfaCode(id.toLong(), code)

        assertThat(isValid).isFalse
    }

    @Test
    fun `When email code is invalid then return false`() {
        val code = "65abv7"
        val id = 5
        val expectedMfaToken = MfaEmailToken(id = id.toString(), code)

        every { mfaEmailCodeRepository.findById(id.toString()) } returns Optional.of(expectedMfaToken)

        val isValid = mfaEmailTokenService.verifyMfaCode(id.toLong(), "54bnOt")

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

    @Test
    fun `When recovery code is valid then return true`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))
        user.id = 5

        every { userRepository.getReferenceById(5) } returns user
        every { mfaRecoveryCodeRepository.saveAll(any<List<MfaRecoveryCode>>()) } returnsArgument 0

        val rawCodes = mfaRecoveryService.generateRecoveryCodes()
        val codes = mfaRecoveryService.saveRecoveryCodes(user.id!!, rawCodes)

        every { mfaRecoveryCodeRepository.getAllByUser(any()) } returns codes
        every { mfaRecoveryCodeRepository.delete(any()) } returns Unit

        val result = mfaRecoveryService.handleVerification(CustomUserDetails(user), rawCodes[0])

        assertThat(result).isTrue
    }

    @Test
    fun `When recovery code is invalid then return false`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))
        user.id = 5

        every { userRepository.getReferenceById(5) } returns user
        every { mfaRecoveryCodeRepository.saveAll(any<List<MfaRecoveryCode>>()) } returnsArgument 0

        val rawCodes = mfaRecoveryService.generateRecoveryCodes()
        val codes = mfaRecoveryService.saveRecoveryCodes(user.id!!, rawCodes)

        every { mfaRecoveryCodeRepository.getAllByUser(any()) } returns codes
        every { mfaRecoveryCodeRepository.delete(any()) } returns Unit

        val result = mfaRecoveryService.handleVerification(CustomUserDetails(user), "_".repeat(6))

        assertThat(result).isFalse
    }
}