package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.security.CustomAuthenticationProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class AuthenticationService {
    @Autowired
    private lateinit var userDetailsService: UserDetailsServiceImpl
    @Autowired
    private lateinit var authenticationProvider: CustomAuthenticationProvider

    /*
    Only use for credentials checks and never for login
    */
    fun verifyAuthenticationWithoutMfa(user: Authentication, password: String): Boolean {
        authenticationProvider.shouldUseMfa = false

        return try {
            authenticationProvider
                    .authenticate(UsernamePasswordAuthenticationToken.unauthenticated(user.name, password))
            true
        } catch (authenticationException: AuthenticationException) {
            false
        }
    }

    fun reAuthenticate(currentAuthentication: Authentication) {
        val user = userDetailsService.loadUserByUsername(currentAuthentication.name)
        val newAuthentication = UsernamePasswordAuthenticationToken.authenticated(user, user.password, user.authorities)
        newAuthentication.details = currentAuthentication.details

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = newAuthentication
        SecurityContextHolder.setContext(context)
    }
}