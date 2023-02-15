package com.anomot.anomotbackend.security

import com.anomot.anomotbackend.exceptions.UserBannedException
import com.anomot.anomotbackend.security.filters.CustomJsonReaderFilter
import com.anomot.anomotbackend.security.filters.LoginArgumentValidationFilter
import com.anomot.anomotbackend.services.LoginInfoExtractorService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import com.anomot.anomotbackend.utils.Constants
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.resolver.DefaultAddressResolverGroup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.core.io.Resource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.thymeleaf.TemplateEngine
import org.thymeleaf.spring5.SpringTemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.ITemplateResolver
import reactor.netty.http.client.HttpClient
import java.util.*
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
    @Value("\${mail.host}")
    private val host: String? = null
    @Value("\${mail.port}")
    private val port: String? = null
    @Value("\${mail.protocol}")
    private val protocol: String? = null
    @Value("\${mail.username}")
    private val username: String? = null
    @Value("\${mail.password}")
    private val password: String? = null
    @Value("\${cookie.domain}")
    private val cookieDomain: String? = null
    @Value("classpath:mail/mail.properties")
    private val resourceFile: Resource? = null

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
                            "/account/password/reset",
                            "/media/{id}",
                            "/url/{url}").permitAll()
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
                    .and()
                .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse().also {
                        it.setCookieDomain(cookieDomain)
                    })
                    .and()
                .authenticationProvider(authenticationProvider())
                .rememberMe()
                    .useSecureCookie(true)
                    .tokenValiditySeconds(Constants.REMEMBER_ME_VALIDITY_DURATION)
                    .rememberMeCookieDomain(Constants.REMEMBER_ME_COOKIE_DOMAIN)
                    .rememberMeParameter(Constants.REMEMBER_ME_PARAMETER)
                    .rememberMeCookieName(Constants.REMEMBER_ME_COOKIE_NAME)
                    .rememberMeServices(PersistentTokenBasedRememberMeServices(
                            rememberKey, userDetailsService(), customRememberMeTokenRepository
                    ).also {
                        it.parameter = Constants.REMEMBER_ME_PARAMETER
                    })
                .and()
                .cors()
                .and()
                .addFilterBefore(CustomJsonReaderFilter(), UsernamePasswordAuthenticationFilter::class.java)
                .addFilterAfter(LoginArgumentValidationFilter(), CustomJsonReaderFilter::class.java)
                .httpBasic()
                .authenticationEntryPoint { _, response, _ ->
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.reasonPhrase)
                }

        return http.build()
    }

    @Bean
    fun webSecurityCustomizer(): WebSecurityCustomizer {
        return WebSecurityCustomizer { web: WebSecurity -> web.ignoring().antMatchers(HttpMethod.OPTIONS, "/**") }
    }

    private val loginSuccessHandler = AuthenticationSuccessHandler {
        request, response, authentication ->
        // Get user
        val userDto = (authentication.principal as CustomUserDetails).getAsSelfDto()

        val ip = request.getHeader("X-Real-IP") ?: request.remoteAddr

        // Store login info
        val successfulLogin = loginInfoExtractorService.getInfo(ip, request.getHeader("User-Agent"))
        loginInfoExtractorService.saveLoginAndSendNotification((authentication.principal as CustomUserDetails), successfulLogin)

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

        if (authentication is UserBannedException) {
            val mapper = ObjectMapper()
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            response.status = SC_FORBIDDEN
            response.contentType = "application/json"
            val dto = object {
                val isBanned = true
                val bannedUntil = authentication.banUntil
            }
            response.writer.write(mapper.writeValueAsString(dto))
        } else {
            response.status = SC_UNAUTHORIZED
            response.contentType = "text/plain"

            response.writer.write(authentication.message ?: "Login error")
        }
    }

    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry
                        .addMapping("/**")
                        .allowedOrigins(clientDomain)
                        .allowedMethods("POST", "GET", "DELETE", "PUT", "OPTIONS")
                        .allowedHeaders("DNT",
                                "User-Agent",
                                "X-Requested-With",
                                "If-Modified-Since",
                                "Cache-Control",
                                "Content-Type",
                                "Range",
                                "X-XSRF-TOKEN")
                        .allowCredentials(true)
            }
        }
    }

    @Bean
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

    @Bean
    @Primary
    fun webClient(): WebClient {
        val httpClient: HttpClient = HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE)
        val size = 100 * 1024 * 1024
        val strategies = ExchangeStrategies.builder()
                .codecs { codecs: ClientCodecConfigurer -> codecs.defaultCodecs().maxInMemorySize(size) }
                .build()
        return WebClient.builder()
                .exchangeStrategies(strategies)
                .clientConnector(ReactorClientHttpConnector(httpClient))
                .build()
    }

    @Bean
    fun mailSender(): JavaMailSender {
        val mailSender = JavaMailSenderImpl()

        mailSender.host = host
        mailSender.port = port?.toInt() ?: 25
        mailSender.protocol = protocol
        mailSender.username = username
        mailSender.password = password

        val javaMailProperties = Properties()
        javaMailProperties.load(resourceFile?.inputStream)
        mailSender.javaMailProperties = javaMailProperties
        return mailSender
    }

    @Bean
    fun emailMessageSource(): ResourceBundleMessageSource {
        val messageSource = ResourceBundleMessageSource()
        messageSource.setBasename("mail/messages/messages")
        messageSource.setDefaultLocale(Locale.ENGLISH)
        messageSource.setDefaultEncoding("UTF-8")
        return messageSource
    }

    @Bean
    fun emailTemplateEngine(): TemplateEngine {
        val templateEngine = SpringTemplateEngine()
        templateEngine.addTemplateResolver(htmlTemplateResolver())
        templateEngine.setTemplateEngineMessageSource(emailMessageSource())
        return templateEngine
    }

    private fun htmlTemplateResolver(): ITemplateResolver {
        val templateResolver = ClassLoaderTemplateResolver()
        templateResolver.order = Integer.valueOf(2)
        templateResolver.resolvablePatterns = Collections.singleton("html/*")
        templateResolver.prefix = "/mail/"
        templateResolver.suffix = ".html"
        templateResolver.templateMode = TemplateMode.HTML
        templateResolver.characterEncoding = "UTF-8"
        templateResolver.isCacheable = false
        return templateResolver
    }

}