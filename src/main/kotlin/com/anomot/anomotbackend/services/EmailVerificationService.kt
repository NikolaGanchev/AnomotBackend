package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.AnomotBackendApplication
import com.anomot.anomotbackend.entities.EmailVerificationToken
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.EmailVerificationTokenRepository
import com.anomot.anomotbackend.repositories.UserRepository
import com.anomot.anomotbackend.utils.Constants
import com.anomot.anomotbackend.utils.SecureRandomStringGenerator
import com.anomot.anomotbackend.utils.TimeUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service
import java.lang.Exception
import java.time.Instant
import java.util.*
import javax.transaction.Transactional

@Service
class EmailVerificationService @Autowired constructor(
        private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
        private val userRepository: UserRepository,
        private val emailService: EmailService,
        @Value("\${email.verification.url}")
        private val emailVerificationUrl: String
) {
    private val logger: Logger = LoggerFactory.getLogger(AnomotBackendApplication::class.java)
    @Value("\${environment.is-local}")
    private val isLocal: String? = null

    fun generateVerificationCode(): String {
        val stringGenerator = SecureRandomStringGenerator(SecureRandomStringGenerator.ALPHANUMERIC)

        return stringGenerator.generate(Constants.EMAIL_VERIFICATION_TOKEN_LENGTH)
    }

    fun createEmailVerificationToken(verificationCode: String, user: User, date: Date): EmailVerificationToken {
        return EmailVerificationToken(
                verificationCode,
                user,
                date)
    }

    fun generateExpiryDate(tokenLifeDurationDays: Int, now: Instant): Date {
        return TimeUtils.generateFutureDate(tokenLifeDurationDays)
    }

    fun saveEmailVerificationToken(emailVerificationToken: EmailVerificationToken): EmailVerificationToken {
        return emailVerificationTokenRepository.save(emailVerificationToken)
    }

    fun sendVerificationEmail(user: User, token: EmailVerificationToken) {
        if (isLocal != null && isLocal.toBoolean()) {
            logger.info("\nEmail verification token \n" +
                    "Code: ${token.verificationCode} \n" +
                    "Expiry date: ${token.expiryDate} \n" +
                    "Link: ${emailVerificationUrl.format(token.verificationCode)} \n" +
                    "User email: ${user.email}")
        }
        emailService.sendEmailVerificationEmail(user.email,
                emailVerificationUrl.format(token.verificationCode),
                LocaleContextHolder.getLocale())
    }

    @Transactional
    fun verifyEmail(verificationCode: String, now: Instant): Boolean {
        val emailVerificationToken = emailVerificationTokenRepository
                .findByVerificationCode(verificationCode) ?: return false

        return if (isNotExpired(emailVerificationToken, now)) {
            val editedRows = userRepository.setIsEmailVerifiedByEmail(true,
                    emailVerificationToken.user.email)

            try {
                emailVerificationTokenRepository.delete(emailVerificationToken)
            } catch (e: Exception) {
                return false
            }

            editedRows != 0
        } else {
            false
        }
    }

    fun isNotExpired(token: EmailVerificationToken, now: Instant): Boolean {
        return token.expiryDate.toInstant().isAfter(now)
    }

}