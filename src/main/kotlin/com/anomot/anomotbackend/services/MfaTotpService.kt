package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.entities.MfaTotpSecret
import com.anomot.anomotbackend.repositories.MfaTotpSecretRepository
import com.anomot.anomotbackend.utils.Constants
import com.bastiaanjansen.otp.HMACAlgorithm
import com.bastiaanjansen.otp.SecretGenerator
import com.bastiaanjansen.otp.TOTPGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

    fun verifyMfaCode(email: String, codeToVerify: String, injectedTotp: TOTPGenerator? = null): Boolean {
        val secret = mfaTotpSecretRepository.findByEmail(email) ?: return false

        if (injectedTotp != null) return injectedTotp.verify(codeToVerify)

        val totp = TOTPGenerator.Builder(secret.secret.toByteArray())
                .withHOTPGenerator {
                    it.withPasswordLength(Constants.MFA_PASSWORD_LENGTH)
                    it.withAlgorithm(HMACAlgorithm.SHA256)
                }
                .withPeriod(Duration.ofSeconds(Constants.TOTP_PERIOD))
                .build()

        return totp.verify(codeToVerify, Constants.TOTP_CODE_ALLOWED_DELAY)
    }

    @Transactional
    fun deleteMfaSecret(userId: Long) {
        mfaTotpSecretRepository.deleteByUserId(userId)
    }
}