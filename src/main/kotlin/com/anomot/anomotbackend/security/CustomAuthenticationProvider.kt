package com.anomot.anomotbackend.security

import com.anomot.anomotbackend.exceptions.MfaEmailSentException
import com.anomot.anomotbackend.exceptions.MfaRequiredException
import com.anomot.anomotbackend.exceptions.BadMfaCodeException
import com.anomot.anomotbackend.services.MfaEmailTokenService
import com.anomot.anomotbackend.services.MfaTotpService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component


@Component
class CustomAuthenticationProvider(userDetailsService: UserDetailsServiceImpl): DaoAuthenticationProvider() {

    init {
        super.setUserDetailsService(userDetailsService)
    }

    @Autowired
    private lateinit var mfaEmailTokenService: MfaEmailTokenService
    @Autowired
    private lateinit var mfaTotpService: MfaTotpService

    /*
    Only set to false for internal checks that require credentials and not for login
    Also onyl use if the bean scope is prototype
     */
    var shouldUseMfa: Boolean = true

    override fun additionalAuthenticationChecks(userDetails: UserDetails, authentication: UsernamePasswordAuthenticationToken) {
        super.additionalAuthenticationChecks(userDetails, authentication)

        if (!shouldUseMfa) return

        val authenticationDetails = authentication.details as CustomAuthenticationDetails
        val user: CustomUserDetails = userDetails as CustomUserDetails

        if (user.isMfaEnabled()) {
            val mfaCode = authenticationDetails.mfaCode
            val mfaMethod = authenticationDetails.mfaMethodValue
            val shouldSendMfaEmail = authenticationDetails.shouldSendMfaEmail
            val userSupportedMfaMethods = user.getMfaMethodsAsList()

            if (mfaCode == null || mfaMethod == null || !userSupportedMfaMethods!!.contains(mfaMethod.method)) {
                /*
                If the execution reaches this point, that means that the user credentials are correct,
                but they haven't provided Multi-factor authentication parameters despite them being enabled
                on their account.
                Thus, it is safe to send them a JSON response containing the enabled Mfa methods for their account.
                Java Spring however doesn't allow accessing the user details inside of a loginFailureHandler.
                Instead, they can be sent as a comma separated string inside of the exception message
                 */
                val allowedMethodsString = userDetails.getMfaMethodsAsList()!!.toTypedArray().joinToString(",")
                throw MfaRequiredException(allowedMethodsString)
            }

            if (shouldSendMfaEmail) {
                val mfaEmailToken = mfaEmailTokenService.createMfaEmailToken(user.id.toString())
                mfaEmailTokenService.saveEmailToken(mfaEmailToken)
                mfaEmailTokenService.sendMfaEmail(mfaEmailToken)
                throw MfaEmailSentException("Sent mfa email")
            }

            val isVerified = when(mfaMethod) {
                MfaMethodValue.EMAIL -> {
                    mfaEmailTokenService.verifyMfaCode(user.username, mfaCode).also {
                        if (it) mfaEmailTokenService.deleteMfaCode(user.username, mfaCode)
                    }
                }
                MfaMethodValue.TOTP -> {
                    mfaTotpService.verifyMfaCode(user.username, mfaCode)
                }
            }

            if (!isVerified) {
                throw BadMfaCodeException("Bad Multi-factor authentication code")
            }
        }
   }
}