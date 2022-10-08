package com.anomot.anomotbackend.security

import com.anomot.anomotbackend.security.filters.CustomJsonReaderFilter
import com.anomot.anomotbackend.security.filters.LoginArgumentValidationFilter
import com.anomot.anomotbackend.services.LoginInfoExtractorService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import com.anomot.anomotbackend.utils.Constants
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.servlet.http.HttpServletResponse.*


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true)
class WebSecurityConfig {
    @Value("\${remember-me.key}")
    private val rememberKey: String? = null
    @Value("\${client.domain}")
    private val clientDomain: String? = null

    @Autowired
    private lateinit var customRememberMeTokenRepository: CustomRememberMeTokenRepository
    @Autowired
    private lateinit var loginInfoExtractorService: LoginInfoExtractorService

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeRequests()
                    .antMatchers("/account/new",
                            "/account/email/verify",
                            "/account/mfa/email/methods",
                            "/account/mfa/email/send",
                            "/account/mfa/status",
                            "/account/password/reset/new",
                            "/account/password/reset").permitAll()
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
                    .and()
                .authenticationProvider(authenticationProvider())
                .rememberMe()
                    .useSecureCookie(true)
                    .tokenValiditySeconds(Constants.REMEMBER_ME_VALIDITY_DURATION)
                    .rememberMeCookieDomain(Constants.REMEMBER_ME_COOKIE_DOMAIN)
                    .rememberMeParameter(Constants.REMEMBER_ME_PARAMETER)
                    .rememberMeServices(PersistentTokenBasedRememberMeServices(
                            rememberKey, userDetailsService(), customRememberMeTokenRepository
                    ))
                .and()
                .cors()
                .and()
                .addFilterBefore(CustomJsonReaderFilter(), UsernamePasswordAuthenticationFilter::class.java)
                .addFilterAfter(LoginArgumentValidationFilter(), CustomJsonReaderFilter::class.java)
                .httpBasic()


        return http.build()
    }

    private val loginSuccessHandler = AuthenticationSuccessHandler {
        request, response, authentication ->
        // Get user
        val userDto = (authentication.principal as CustomUserDetails).getAsDto()

        // Store login info
        val successfulLogin = loginInfoExtractorService.getInfo(request.remoteAddr, request.getHeader("User-Agent"))
        loginInfoExtractorService.saveLogin((authentication.principal as CustomUserDetails), successfulLogin)

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

    @Bean
    fun corsMappingConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**")
                        .allowedOrigins(clientDomain)
                        .allowedMethods("POST", "GET", "DELETE", "PUT", "OPTIONS")
                        .allowCredentials(true)
            }
        }
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