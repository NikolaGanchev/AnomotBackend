package com.anomot.anomotbackend.security

import com.anomot.anomotbackend.utils.Constants
import org.springframework.security.web.authentication.WebAuthenticationDetails
import java.util.*
import javax.servlet.http.HttpServletRequest

class CustomAuthenticationDetails(request: HttpServletRequest): WebAuthenticationDetails(request) {
    var mfaCode: String?
        private set
    var mfaMethodValue: MfaMethodValue? = null
        private set
    var shouldSendMfaEmail = false
        private set

    init {
        mfaCode = request.getParameter(Constants.MFA_CODE_PARAMETER)
        val mfaMethodString: String? = request.getParameter(Constants.MFA_METHOD_PARAMETER)
        val shouldSendMfaEmailString: String? = request.getParameter(Constants.MFA_SHOULD_SEND_MFA_EMAIL)
        shouldSendMfaEmail = shouldSendMfaEmailString != null && shouldSendMfaEmailString == "true"

        if (mfaMethodString != null) {
            mfaMethodValue = MfaMethodValue.valueOf(mfaMethodString.uppercase(Locale.ENGLISH))
        }
    }

}