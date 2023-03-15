package com.anomot.anomotbackend.security

import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import jakarta.servlet.http.HttpServletRequest

class CustomAuthenticationDetailsSource: WebAuthenticationDetailsSource() {

    override fun buildDetails(context: HttpServletRequest): WebAuthenticationDetails {
        return CustomAuthenticationDetails(context)
    }
}