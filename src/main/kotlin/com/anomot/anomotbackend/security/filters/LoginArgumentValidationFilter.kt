package com.anomot.anomotbackend.security.filters

import com.anomot.anomotbackend.dto.LoginDto
import com.anomot.anomotbackend.dto.MfaDto
import com.anomot.anomotbackend.utils.Constants
import org.springframework.web.filter.OncePerRequestFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Validation

class LoginArgumentValidationFilter: OncePerRequestFilter() {
    private var validator = Validation.buildDefaultValidatorFactory().validator

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val isValidUsernameAndPassword = validateUsernameAndPassword(
                request.getParameter(Constants.PASSWORD_PARAMETER),
                request.getParameter(Constants.USERNAME_PARAMETER),
                request.getParameter(Constants.REMEMBER_ME_PARAMETER))

        val isValidMfa = validateMfaArguments(
                request.getParameter(Constants.MFA_CODE_PARAMETER),
                request.getParameter(Constants.MFA_METHOD_PARAMETER),
                request.getParameter(Constants.MFA_RECOVERY_CODE_PARAMETER))

        if (isValidUsernameAndPassword && isValidMfa) filterChain.doFilter(request, response)
        else response.sendError(HttpServletResponse.SC_FORBIDDEN)
    }

    private fun validateUsernameAndPassword(username: String?, password: String?, rememberMe: String?): Boolean {
        val loginDto = LoginDto(username, password, rememberMe?.toBoolean())

        val violations = validator.validate(loginDto)

        return violations.isEmpty()
    }

    private fun validateMfaArguments(code: String?, method: String?, recoveryCode: String?): Boolean {
        val mfaDto = MfaDto(code, method, recoveryCode)

        val violations = validator.validate(mfaDto)

        return violations.isEmpty()
    }
}