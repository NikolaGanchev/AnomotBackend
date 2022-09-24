package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.entities.MfaRecoveryCode
import com.anomot.anomotbackend.repositories.MfaRecoveryCodeRepository
import com.anomot.anomotbackend.repositories.UserRepository
import com.anomot.anomotbackend.utils.Constants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class MfaRecoveryService @Autowired constructor(
        private val mfaRecoveryCodeRepository: MfaRecoveryCodeRepository,
        private val userRepository: UserRepository
) {
    private fun generateRecoveryCode(): String {
        val dictionary = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val length = Constants.MFA_RECOVERY_CODE_LENGTH
        val random = SecureRandom()
        val code = StringBuilder(length)

        for (i in 1..length) {
            code.append(dictionary[random.nextInt(dictionary.length)])
        }

        return code.toString()
    }

    fun updateRecoveryCodes(userId: Long): List<String> {
        deleteRecoveryCodes(userId)
        val codes = generateRecoveryCodes()
        saveRecoveryCodes(userId, codes)

        return codes
    }

    private fun generateRecoveryCodes(): List<String> {
        return List(Constants.MFA_RECOVERY_CODE_AMOUNT) {
            generateRecoveryCode()
        }
    }

    fun saveRecoveryCodes(userId: Long, codes: List<String>) {
        val user = userRepository.getReferenceById(userId)
        val recoveryCodes = codes.map {
            MfaRecoveryCode(it, user)
        }

        mfaRecoveryCodeRepository.saveAll(recoveryCodes)
    }

    fun handleVerification(userId: Long, code: String): Boolean {
        return if (verifyRecoveryCode(userId, code)) {
            deleteRecoveryCode(userId, code)
            true
        } else {
            false
        }
    }

    fun verifyRecoveryCode(userId: Long, code: String): Boolean {
        val user = userRepository.getReferenceById(userId)

        return mfaRecoveryCodeRepository.existsByUserAndCode(user, code)
    }

    fun deleteRecoveryCode(userId: Long, code: String): Long {
        val user = userRepository.getReferenceById(userId)

        return mfaRecoveryCodeRepository.deleteByUserAndCode(user, code)
    }

    fun deleteRecoveryCodes(userId: Long): Long {
        val user = userRepository.getReferenceById(userId)

        return mfaRecoveryCodeRepository.deleteAllByUser(user)
    }

}