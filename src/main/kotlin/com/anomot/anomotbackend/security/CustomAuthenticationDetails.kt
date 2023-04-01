package com.anomot.anomotbackend.security

import com.anomot.anomotbackend.utils.Constants
import org.springframework.security.web.authentication.WebAuthenticationDetails
import java.util.*
import javax.servlet.http.HttpServletRequest

class CustomAuthenticationDetails(request: HttpServletRequest): WebAuthenticationDetails(request) {
    val mfaCode: String?
    val mfaMethodValue: MfaMethodValue?
    val recoveryCode: String?

    init {
        mfaCode = request.getParameter(Constants.MFA_CODE_PARAMETER)
        val mfaMethodString: String? = request.getParameter(Constants.MFA_METHOD_PARAMETER)

        if (mfaMethodString != null) {
            mfaMethodValue = MfaMethodValue.valueOf(mfaMethodString.uppercase(Locale.ENGLISH))
        } else {
            mfaMethodValue = null
        }

        recoveryCode = request.getParameter(Constants.MFA_RECOVERY_CODE_PARAMETER)
    }

}