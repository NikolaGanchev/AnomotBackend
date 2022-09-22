package com.anomot.anomotbackend.security

import com.anomot.anomotbackend.security.filters.CustomJsonReaderFilter
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import com.anomot.anomotbackend.utils.Constants
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.security.authentication.DisabledException
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import javax.servlet.http.HttpServletResponse.*


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true)
class WebSecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeRequests()
                    .antMatchers("/account/new",
                            "/account/email/verify",
                            "/account/mfa/email/methods",
                            "/account/mfa/email/send",
                            "/account/mfa/status").permitAll()
                    .anyRequest().authenticated()
                    .and()
                .formLogin()
                    .loginProcessingUrl("/account/login")
                    .usernameParameter(Constants.USERNAME_PARAMETER)
                    .passwordParameter(Constants.PASSWORD_PARAMETER)
                    .successHandler(loginSuccessHandler)
                    .failureHandler(loginFailureHandler)
                    .authenticationDetailsSource(CustomAuthenticationDetailsSource())
                    .permitAll()
                    .and()
                .logout()
                    .logoutUrl("/account/logout")
                    .permitAll()
                    .invalidateHttpSession(true)
                    .deleteCookies("SESSION")
                    .and()
                .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringAntMatchers(
                            "/account/email/verify")
                    .and()
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(CustomJsonReaderFilter(), UsernamePasswordAuthenticationFilter::class.java)
                .httpBasic()


        return http.build()
    }

    private val loginSuccessHandler = AuthenticationSuccessHandler {
        request, response, authentication ->
        // Get user
        val userDto = (authentication.principal as CustomUserDetails).getAsDto()

        // Set up mapper
        val mapper = ObjectMapper()
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)

        // Fill out response
        response.status = SC_OK
        response.contentType = "application/json"
        response.writer.write(mapper.writeValueAsString(userDto))
    }

    private val loginFailureHandler = AuthenticationFailureHandler {
        request, response, authentication ->
        response.status = SC_UNAUTHORIZED
        response.contentType = "text/plain"

        if (authentication is DisabledException) {
            response.status = SC_FORBIDDEN
        }

        response.writer.write(authentication.message ?: "Login error")
    }

    @Bean()
    @Scope("prototype")
    fun authenticationProvider(): CustomAuthenticationProvider {
        val authProvider = CustomAuthenticationProvider(userDetailsService())
        authProvider.setPasswordEncoder(passwordEncoder())
        return authProvider
    }

    @Bean
    fun passwordEncoder(): Argon2PasswordEncoder {
        return Argon2PasswordEncoder()
    }

    @Bean
    fun userDetailsService(): UserDetailsServiceImpl {
        return UserDetailsServiceImpl()
    }
}