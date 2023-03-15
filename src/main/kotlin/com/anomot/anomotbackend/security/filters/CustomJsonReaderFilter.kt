package com.anomot.anomotbackend.security.filters

import com.anomot.anomotbackend.utils.Constants
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.filter.OncePerRequestFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse


class CustomJsonReaderFilter:
        OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        if (request.contentType != "application/json" || !request.requestURL.endsWith("account/login")) {
            chain.doFilter(request, response)
        } else {

            val requestMap: MutableMap<String, String>? = ObjectMapper()
                    .readValue(request.inputStream, MutableMap::class.java)
                    as? MutableMap<String, String>

            if (requestMap == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST)
                return
            }

            val email: String? = requestMap[Constants.USERNAME_PARAMETER]

            val password: String? = requestMap[Constants.PASSWORD_PARAMETER]

            if (email == null || password == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST)
                return
            }

            val mutableRequestWrapper = CustomHttpServletRequestWrapper(request)

            mutableRequestWrapper.setParameter(Constants.USERNAME_PARAMETER, email)
            mutableRequestWrapper.setParameter(Constants.PASSWORD_PARAMETER, password)

            // Multi-factor authentication parameters
            if (requestMap.contains(Constants.MFA_CODE_PARAMETER)) {
                val mfaCode = requestMap[Constants.MFA_CODE_PARAMETER]
                mutableRequestWrapper.setParameter(Constants.MFA_CODE_PARAMETER, mfaCode!!)
            }

            if (requestMap.contains(Constants.MFA_METHOD_PARAMETER)) {
                val mfaMethod = requestMap[Constants.MFA_METHOD_PARAMETER]
                mutableRequestWrapper.setParameter(Constants.MFA_METHOD_PARAMETER, mfaMethod!!)
            }

            if (requestMap.contains(Constants.MFA_RECOVERY_CODE_PARAMETER)) {
                val mfaRecoveryCode = requestMap[Constants.MFA_RECOVERY_CODE_PARAMETER]
                mutableRequestWrapper.setParameter(Constants.MFA_RECOVERY_CODE_PARAMETER, mfaRecoveryCode!!)
            }

            // Remember me parameter
            if (requestMap.contains(Constants.REMEMBER_ME_PARAMETER)) {
                val rememberMe = requestMap[Constants.REMEMBER_ME_PARAMETER].toString()
                mutableRequestWrapper.setParameter(Constants.REMEMBER_ME_PARAMETER, rememberMe)
            }

            chain.doFilter(mutableRequestWrapper, response)
        }
    }
}