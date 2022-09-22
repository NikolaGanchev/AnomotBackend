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

    /**
     * Verifies the user credentials are correct without using Multi-factor authentication.
     * Should not be used for login.
     * @param user The current authentication object
     * @param password The password to check
     * @return Authentication object if credentials are correct, else null
     */
    fun verifyAuthenticationWithoutMfa(user: Authentication, password: String): Authentication? {
        return verifyAuthenticationWithoutMfa(user.name, password)
    }

    /**
     * Verifies the user credentials are correct without using Multi-factor authentication.
     * Should not be used for login.
     * @param email The user email
     * @param password The password to check
     * @return Authentication object if credentials are correct, else null
     */
    fun verifyAuthenticationWithoutMfa(email: String, password: String): Authentication? {
        authenticationProvider.shouldUseMfa = false

        return try {
            authenticationProvider
                    .authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password))
        } catch (authenticationException: AuthenticationException) {
            null
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