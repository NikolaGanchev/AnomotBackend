package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.entities.MfaTotpSecret
import com.anomot.anomotbackend.repositories.MfaTotpSecretRepository
import com.anomot.anomotbackend.utils.Constants
import com.bastiaanjansen.otp.SecretGenerator
import com.bastiaanjansen.otp.TOTP
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class MfaTotpService @Autowired constructor(
        val mfaTotpSecretRepository: MfaTotpSecretRepository
) {
    fun generateSecret(): ByteArray {
        return SecretGenerator.generate(160)
    }

    fun saveCode(mfaTotpSecret: MfaTotpSecret) {
        mfaTotpSecretRepository.save(mfaTotpSecret)
    }

    fun verifyMfaCode(email: String, codeToVerify: String, injectedTotp: TOTP? = null): Boolean {
        val secret = mfaTotpSecretRepository.findByEmail(email) ?: return false

        if (injectedTotp != null) return injectedTotp.verify(codeToVerify)

        val totp = TOTP.Builder(secret.secret.toByteArray())
                .withPasswordLength(Constants.TOTP_PASSWORD_LENGTH)
                .withPeriod(Duration.ofSeconds(Constants.TOTP_PERIOD))
                .build()

        return totp.verify(codeToVerify, Constants.TOTP_CODE_ALLOWED_DELAY)
    }

    fun deleteMfaSecret(userId: Long) {
        mfaTotpSecretRepository.deleteByUserId(userId)
    }
}