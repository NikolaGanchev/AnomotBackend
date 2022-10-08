package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.AnomotBackendApplication
import com.anomot.anomotbackend.entities.MfaEmailToken
import com.anomot.anomotbackend.repositories.MfaEmailCodeRepository
import com.anomot.anomotbackend.utils.Constants
import com.anomot.anomotbackend.utils.SecureRandomStringGenerator
import com.anomot.anomotbackend.utils.secureEquals
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MfaEmailTokenService @Autowired constructor(
        val mfaEmailCodeRepository: MfaEmailCodeRepository
) {

    private val logger: Logger = LoggerFactory.getLogger(AnomotBackendApplication::class.java)
    @Value("\${environment.is-local}")
    private val isLocal: String? = null

    private fun generateEmailCode(): String {
        val stringGenerator = SecureRandomStringGenerator(SecureRandomStringGenerator.NUMERIC)

        return stringGenerator.generate(Constants.MFA_PASSWORD_LENGTH)
    }

    fun createMfaEmailToken(id: String): MfaEmailToken {
        return MfaEmailToken(id = id, code = generateEmailCode())
    }

    fun saveEmailToken(mfaEmailToken: MfaEmailToken) {
        mfaEmailCodeRepository.save(mfaEmailToken)
    }

    fun generateAndSendMfaEmail(userId: String) {
        val mfaEmailToken = createMfaEmailToken(userId)
        saveEmailToken(mfaEmailToken)
        sendMfaEmail(mfaEmailToken)
    }

    fun sendMfaEmail(mfaEmailToken: MfaEmailToken) {
        if (isLocal != null && isLocal.toBoolean()) {
        logger.info("\nMulti-factor authentication email token \n" +
                "Code: ${mfaEmailToken.code} \n" +
                "User id: ${mfaEmailToken.id}")
        }
        //TODO("implement when emails are available")
    }

    fun verifyMfaCode(id: String, codeToVerify: String): Boolean {
        val foundCode = mfaEmailCodeRepository.findById(id)

        if (foundCode.isEmpty) return false

        if (foundCode.get().code.secureEquals(codeToVerify)) return true

        return false
    }

    fun deleteMfaCode(id: String, codeToDelete: String) {
        mfaEmailCodeRepository.deleteById(id)
    }
}