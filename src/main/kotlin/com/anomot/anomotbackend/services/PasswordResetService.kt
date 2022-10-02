package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.entities.PasswordResetToken
import com.anomot.anomotbackend.repositories.PasswordResetTokenRepository
import com.anomot.anomotbackend.repositories.UserRepository
import com.anomot.anomotbackend.utils.Constants
import com.anomot.anomotbackend.utils.SecureRandomStringGenerator
import com.anomot.anomotbackend.utils.TimeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
class PasswordResetService @Autowired constructor(
        private val passwordEncoder: Argon2PasswordEncoder,
        private val userRepository: UserRepository,
        private val passwordResetTokenRepository: PasswordResetTokenRepository
) {

    private val TIMING_ATTACK_MITIGATION_CODE = "a".repeat(Constants.PASSWORD_RESET_CODE_LENGTH)
    private var encryptedTimingAttackMitigationCode: String? = null

    private fun generateResetCode(): String {
        val stringGenerator = SecureRandomStringGenerator(SecureRandomStringGenerator.ALPHANUMERIC)

        return stringGenerator.generate(Constants.PASSWORD_RESET_CODE_LENGTH)
    }

    private fun generateIdentifier(): String {
        return UUID.randomUUID().toString()
    }

    fun generateExpiryDate(tokenLifeDurationMinutes: Int, now: Instant): Date {
        return TimeUtils.generateFutureAfterMinutes(tokenLifeDurationMinutes)
    }

    fun sendPasswordResetTokenEmail(passwordResetToken: PasswordResetToken) {
        // TODO("implement when emails are available")
    }

    fun sendPasswordResetEmail(passwordResetToken: PasswordResetToken) {
        // TODO("implement when emails are available")
    }

    // Use async to prevent timing attacks
    @Async
    fun handlePasswordResetCreation(email: String, expiryDate: Date): CompletableFuture<Boolean> {
        val code = generateResetCode()

        val identifier = generateIdentifier()

        val user = userRepository.findByEmail(email) ?: return CompletableFuture.completedFuture(false)

        val hashedCode = passwordEncoder.encode(code)

        val passwordResetToken = PasswordResetToken(hashedCode, identifier, user, expiryDate)

        passwordResetTokenRepository.save(passwordResetToken)

        sendPasswordResetTokenEmail(passwordResetToken)

        return CompletableFuture.completedFuture(true)
    }

    // Checks if the provided password reset code exists and if it does, resets the user's password
    fun resetPasswordIfValidCode(code: String, identifier: String, newPassword: String, now: Instant): Boolean {
        val passwordResetToken = passwordResetTokenRepository.findByIdentifier(identifier)

        if (passwordResetToken != null) {
            val isValidCode = passwordEncoder.matches(code, passwordResetToken.resetToken)

            if (isValidCode && isNotExpired(passwordResetToken, now)) {
                resetPassword(newPassword, passwordResetToken.user.id)
                sendPasswordResetEmail(passwordResetToken)
                return true
            }
        } else {
            mitigatePasswordResetTimingAttack(code)
        }

        return false
    }

    // Use async to prevent timing attacks
    @Async
    protected fun resetPassword(newPassword: String, id: Long?) {
        if (id == null) {
            throw IllegalArgumentException("User id should not be null")
        }

        userRepository.setPassword(newPassword, id)
    }

    private fun mitigatePasswordResetTimingAttack(code: String) {
        if (encryptedTimingAttackMitigationCode == null) {
            encryptedTimingAttackMitigationCode = passwordEncoder.encode(TIMING_ATTACK_MITIGATION_CODE)
        }

        passwordEncoder.matches(code, encryptedTimingAttackMitigationCode)
    }

    fun isNotExpired(token: PasswordResetToken, now: Instant): Boolean {
        return token.expiryDate.toInstant().isAfter(now)
    }
}