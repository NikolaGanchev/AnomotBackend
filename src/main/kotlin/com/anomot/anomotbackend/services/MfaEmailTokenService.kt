package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.entities.MfaEmailToken
import com.anomot.anomotbackend.repositories.MfaEmailCodeRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class MfaEmailTokenService @Autowired constructor(
        val mfaEmailCodeRepository: MfaEmailCodeRepository
) {

    private fun generateEmailCode(): String {
        val dictionary = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val length = 6
        val random = SecureRandom()
        val code = StringBuilder(length)

        for (i in 1..length) {
            code.append(dictionary[random.nextInt(dictionary.length)])
        }

        return code.toString()
    }

    fun createMfaEmailToken(email: String): MfaEmailToken {
        return MfaEmailToken(email = email, code = generateEmailCode())
    }

    fun saveEmailToken(mfaEmailToken: MfaEmailToken) {
        mfaEmailCodeRepository.save(mfaEmailToken)
    }

    fun sendMfaEmail() {
        //TODO("implement when emails are available")
    }

    fun verifyMfaCode(email: String, codeToVerify: String): Boolean {
        val foundCode = mfaEmailCodeRepository.findById(email)

        if (foundCode.isEmpty) return false

        if (foundCode.get().code == codeToVerify) return true

        return false
    }
}