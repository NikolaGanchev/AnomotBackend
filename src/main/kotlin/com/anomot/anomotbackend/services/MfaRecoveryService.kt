package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.entities.MfaRecoveryCode
import com.anomot.anomotbackend.repositories.MfaRecoveryCodeRepository
import com.anomot.anomotbackend.repositories.UserRepository
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.utils.Constants
import com.anomot.anomotbackend.utils.SecureRandomStringGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Service

@Service
class MfaRecoveryService @Autowired constructor(
        private val mfaRecoveryCodeRepository: MfaRecoveryCodeRepository,
        private val userRepository: UserRepository,
        private val passwordEncoder: Argon2PasswordEncoder
) {

    private val TIMING_ATTACK_MITIGATION_CODE = "a".repeat(Constants.MFA_RECOVERY_CODE_LENGTH)
    private var encryptedTimingAttackMitigationCode: String? = null

    private fun generateRecoveryCode(): String {
        val stringGenerator = SecureRandomStringGenerator(SecureRandomStringGenerator.ALPHANUMERIC)

        return stringGenerator.generate(Constants.MFA_RECOVERY_CODE_LENGTH)
    }

    fun updateRecoveryCodes(userId: Long): List<String> {
        deleteRecoveryCodes(userId)
        val codes = generateRecoveryCodes()
        saveRecoveryCodes(userId, codes)

        return codes
    }

    fun generateRecoveryCodes(): List<String> {
        return List(Constants.MFA_RECOVERY_CODE_AMOUNT) {
            generateRecoveryCode()
        }
    }

    fun saveRecoveryCodes(userId: Long, codes: List<String>): List<MfaRecoveryCode> {
        val user = userRepository.getReferenceById(userId)
        val recoveryCodes = codes.map {
            MfaRecoveryCode(passwordEncoder.encode(it), user)
        }

        return mfaRecoveryCodeRepository.saveAll(recoveryCodes)
    }

    fun handleVerification(userDetails: CustomUserDetails, code: String): Boolean {
        val user = userRepository.getReferenceById(userDetails.id!!)
        val codes = mfaRecoveryCodeRepository.getAllByUser(user)

        if (codes == null) {
            mitigateTimingAttack(Constants.MFA_RECOVERY_CODE_AMOUNT)
            return false
        }

        if (codes.size < Constants.MFA_RECOVERY_CODE_AMOUNT) {
            mitigateTimingAttack(Constants.MFA_RECOVERY_CODE_AMOUNT - codes.size)
        }

        var isSuccessful = false
        var successfulIndex = -1

        for (i in codes.indices) {
            if (passwordEncoder.matches(code, codes[i].code)) {
                isSuccessful = true
                successfulIndex = i
            }
        }

        if (isSuccessful) {
            sendRecoveryCodeUsedEmail(userDetails, code)
            mfaRecoveryCodeRepository.delete(codes[successfulIndex])
        }

        return isSuccessful
    }

    fun sendRecoveryCodeUsedEmail(user: CustomUserDetails, code: String,) {
        //TODO("implement when emails are available")
    }

    fun mitigateTimingAttack(timesToRun: Int) {
        if (encryptedTimingAttackMitigationCode == null) {
            encryptedTimingAttackMitigationCode = passwordEncoder.encode(TIMING_ATTACK_MITIGATION_CODE)
        }
        for (i in 0 until timesToRun) {
            passwordEncoder.matches("a".repeat(Constants.MFA_RECOVERY_CODE_LENGTH), encryptedTimingAttackMitigationCode)
        }
    }

    fun deleteRecoveryCodes(userId: Long): Long {
        val user = userRepository.getReferenceById(userId)

        return mfaRecoveryCodeRepository.deleteAllByUser(user)
    }

}