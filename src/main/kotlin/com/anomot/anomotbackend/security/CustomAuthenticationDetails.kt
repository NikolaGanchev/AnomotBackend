package com.anomot.anomotbackend.security

import com.anomot.anomotbackend.utils.Constants
import org.springframework.security.web.authentication.WebAuthenticationDetails
import javax.servlet.http.HttpServletRequest

class CustomAuthenticationDetails(request: HttpServletRequest): WebAuthenticationDetails(request) {
    private val mfaCode: String?
    private var mfaMethod: MfaMethod? = null

    init {
        mfaCode = request.getParameter(Constants.MFA_CODE_PARAMETER)
        val mfaMethodString: String? = request.getParameter(Constants.MFA_METHOD_PARAMETER)

        if (mfaMethodString != null) {
            mfaMethod = MfaMethod.valueOf(mfaMethodString)
        }
    }

}