package com.anomot.anomotbackend.security

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

            if (mfaCode == null || mfaMethod == null || !user.hasMfaMethod(mfaMethod)) {
                throw MfaRequiredException("Multi-factor authentication required")
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