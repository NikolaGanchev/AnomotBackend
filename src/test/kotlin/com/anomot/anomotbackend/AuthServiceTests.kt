package com.anomot.anomotbackend

import com.anomot.anomotbackend.security.CustomAuthenticationProvider
import com.anomot.anomotbackend.services.AuthenticationService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User

@SpringBootTest
class AuthServiceTests {
    @MockkBean
    @Qualifier("userDetailsService")
    private lateinit var userDetailsService: UserDetailsServiceImpl
    @MockkBean
    @Qualifier("authenticationProvider")
    private lateinit var authenticationProvider: CustomAuthenticationProvider
    @Autowired
    @InjectMockKs
    private lateinit var authenticationService: AuthenticationService

    @Test
    fun `When verifyAuthenticationWithoutMfa and correct credentials then return true`() {
        val authentication = UsernamePasswordAuthenticationToken.authenticated("username",
                "password",
                mutableListOf())

        every { authenticationProvider.authenticate(any()) } returns authentication
        every { authenticationProvider.setProperty("shouldUseMfa").value(false) } propertyType Boolean::class answers { fieldValue = value }

        val result = authenticationService.verifyAuthenticationWithoutMfa(authentication,
                authentication.credentials.toString())

        assertThat(result).isTrue
    }

    @Test
    fun `When verifyAuthenticationWithoutMfa and incorrect credentials then return false`() {
        val authentication = UsernamePasswordAuthenticationToken.authenticated("email",
                "password",
                mutableListOf())

        every { authenticationProvider.authenticate(any()) } throws BadCredentialsException("Bad credentials")
        every { authenticationProvider.setProperty("shouldUseMfa").value(false) } propertyType Boolean::class answers { fieldValue = value }

        val result = authenticationService.verifyAuthenticationWithoutMfa(authentication,
                authentication.credentials.toString())

        assertThat(result).isFalse
    }

    @Test
    @WithMockCustomUser
    fun `When reAuthenticate then set new security context`() {
        val authentication = UsernamePasswordAuthenticationToken.authenticated("email",
                "password",
                mutableListOf())

        val userDetails = User("newEmail", "password", mutableListOf())

        every { userDetailsService.loadUserByUsername(authentication.name) } returns userDetails

        authenticationService.reAuthenticate(authentication)

        val newAuthentication = SecurityContextHolder.getContext().authentication

        assertThat((newAuthentication.principal as User)).isEqualTo(userDetails)
        assertThat(newAuthentication.credentials).isEqualTo(userDetails.password)
    }
}