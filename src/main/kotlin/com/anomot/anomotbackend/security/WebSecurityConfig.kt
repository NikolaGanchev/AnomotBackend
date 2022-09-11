package com.anomot.anomotbackend.security

import com.anomot.anomotbackend.security.filters.CustomJsonReaderFilter
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import com.anomot.anomotbackend.utils.Constants
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import javax.servlet.http.HttpServletResponse.SC_FORBIDDEN
import javax.servlet.http.HttpServletResponse.SC_OK


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
                    .antMatchers("/account/new").permitAll()
                    .anyRequest().authenticated()
                    .and()
                .formLogin()
                    .loginProcessingUrl("/account/login")
                    .usernameParameter(Constants.USERNAME_PARAMETER)
                    .passwordParameter(Constants.PASSWORD_PARAMETER)
                    .successHandler(loginSuccessHandler)
                    .failureHandler(loginFailureHandler)
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
                    .ignoringAntMatchers("/account/2fa/totp",
                            "/account/2fa/email",
                            "/account/new",
                            "/account/login",
                            "/account/logout")
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
        response.status = SC_FORBIDDEN
        response.writer.write("Login error")
    }

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider? {
        val authProvider = DaoAuthenticationProvider()
        authProvider.setUserDetailsService(userDetailsService())
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