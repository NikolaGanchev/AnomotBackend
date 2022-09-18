package com.anomot.anomotbackend

import com.anomot.anomotbackend.controllers.AuthController
import com.anomot.anomotbackend.dto.EmailVerifyDto
import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.dto.UserRegisterDto
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.exceptions.UserAlreadyExistsException
import com.anomot.anomotbackend.security.Authorities
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.security.MfaMethodValue
import com.anomot.anomotbackend.security.WebSecurityConfig
import com.anomot.anomotbackend.services.EmailVerificationService
import com.anomot.anomotbackend.services.MfaEmailTokenService
import com.anomot.anomotbackend.services.MfaTotpService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import com.anomot.anomotbackend.utils.Constants
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.*

@ContextConfiguration(classes = [WebSecurityConfig::class])
@Import(AuthController::class)
@WebMvcTest(controllers = [AuthController::class])
class AuthenticationWebTests @Autowired constructor(
        private val context: WebApplicationContext,
        private val passwordEncoder: Argon2PasswordEncoder
) {

    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl
    @MockkBean
    private lateinit var emailVerificationService: EmailVerificationService
    @MockkBean
    private lateinit var mfaEmailTokenService: MfaEmailTokenService
    @MockkBean
    private lateinit var mfaTotpService: MfaTotpService

    val mfaMethodEmail = MfaMethod(MfaMethodValue.EMAIL.method)
    val mfaMethodTotp = MfaMethod(MfaMethodValue.TOTP.method)

    @BeforeAll
    fun setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @Test
    fun `When register then return User`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = UserRegisterDto("example@test.com", "password12$", "Georgi")
        val expectedResult = UserDto("example@test.com", "Georgi", mutableListOf(authority.authority), false)
        val emailVerificationToken = EmailVerificationToken(
                "code",
                User("example@test.com", "password12$", "Georgi", mutableListOf(authority)),
                Date.from(Instant.now()))


        every { userDetailsServiceImpl.createUser(user) } returns expectedResult
        every { emailVerificationService.generateVerificationCode() } returns "code"
        every { emailVerificationService.createEmailVerificationToken(any(), any(), any()) } returns emailVerificationToken
        every { emailVerificationService.generateExpiryDate(any(), any()) } returns Date.from(Instant.now())
        every { emailVerificationService.sendVerificationEmail(any(), any()) }
        every { emailVerificationService.saveEmailVerificationToken(any()) } returns emailVerificationToken

        mockMvc.perform(post("/account/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJSON(user))
                .with(csrf()))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("\$.email").value(expectedResult.email))
                .andExpect(jsonPath("\$.username").value(expectedResult.username))
                .andExpect(jsonPath("\$.roles").value(expectedResult.roles))
    }

    @Test
    fun `When register and user exists then return 409`() {
        val user = UserRegisterDto("example@test.com", "password12$", "Georgi")

        every { userDetailsServiceImpl.createUser(user) } throws UserAlreadyExistsException("User already exists")

        mockMvc.perform(post("/account/new")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
                .content(TestUtils.objectToJSON(user)))
                .andExpect(status().isConflict)
    }

    @Test
    fun `When user login then return 200 and user`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority), true)

        val dbUser = User("example@test.com",
                passwordEncoder.encode("password12$"),
                "Georgi",
                mutableListOf(authority), true)

        val expectedUserDetails = CustomUserDetails(dbUser)

        every { userDetailsServiceImpl.loadUserByUsername(any()) } returns expectedUserDetails

        mockMvc.perform(formLogin("/account/login").user(Constants.USERNAME_PARAMETER, user.email)
                .password(Constants.PASSWORD_PARAMETER, user.password)
                .acceptMediaType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.email").value(user.email))
                .andExpect(jsonPath("\$.username").value(user.username))
                .andExpect(jsonPath("\$.roles").value(
                        user.authorities.map { it.authority }.toCollection(mutableListOf())))
    }

    @Test
    fun `When user login with bad credentials then return 403`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority), true)

        every { userDetailsServiceImpl.loadUserByUsername(any()) } throws UsernameNotFoundException("Email not found")

        mockMvc.perform(formLogin("/account/login").user(Constants.USERNAME_PARAMETER, user.email)
                .password(Constants.PASSWORD_PARAMETER, user.password)
                .acceptMediaType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden)
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Bad credentials"))
    }

    @Test
    fun `When user login without verified email then return 403`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority), false)

        val dbUser = User("example@test.com",
                passwordEncoder.encode("password12$"),
                "Georgi",
                mutableListOf(authority), false)

        val expectedUserDetails = CustomUserDetails(dbUser)

        every { userDetailsServiceImpl.loadUserByUsername(any()) } returns expectedUserDetails

        mockMvc.perform(formLogin("/account/login").user(Constants.USERNAME_PARAMETER, user.email)
                .password(Constants.PASSWORD_PARAMETER, user.password)
                .acceptMediaType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden)
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(content().string("User is disabled"))
    }

    @Test
    fun `When user login without mfa credentials and mfa enabled then return 403 and Mfa methods`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com",
                "password12$",
                "Georgi",
                mutableListOf(authority),
                true,
                true,
                mutableListOf(mfaMethodEmail, mfaMethodTotp))

        val request = mutableMapOf<String, String>()
        request[Constants.USERNAME_PARAMETER] = user.email
        request[Constants.PASSWORD_PARAMETER] = user.password

        val dbUser = User(user.email,
                passwordEncoder.encode("password12$"),
                user.username,
                mutableListOf(authority),
                user.isEmailVerified,
                user.isMfaActive,
                user.mfaMethods)

        val expectedUserDetails = CustomUserDetails(dbUser)

        every { userDetailsServiceImpl.loadUserByUsername(any()) } returns expectedUserDetails

        mockMvc.perform(post("/account/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJSON(request))
                .with(csrf()))
                .andExpect(status().isForbidden)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"mfaMethods\":[\"email\",\"totp\"]}"))
    }

    @Test
    fun `When user login with wrong mfa method and mfa enabled then return 403 and Mfa methods`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com",
                "password12$",
                "Georgi",
                mutableListOf(authority),
                true,
                true,
                mutableListOf(mfaMethodTotp))

        val request = mutableMapOf<String, String>()
        request[Constants.USERNAME_PARAMETER] = user.email
        request[Constants.PASSWORD_PARAMETER] = user.password
        request[Constants.MFA_CODE_PARAMETER] = "656565"
        request[Constants.MFA_METHOD_PARAMETER] = "email"

        val dbUser = User(user.email,
                passwordEncoder.encode("password12$"),
                user.username,
                mutableListOf(authority),
                user.isEmailVerified,
                user.isMfaActive,
                user.mfaMethods)

        val expectedUserDetails = CustomUserDetails(dbUser)

        every { userDetailsServiceImpl.loadUserByUsername(any()) } returns expectedUserDetails

        mockMvc.perform(post("/account/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJSON(request))
                .with(csrf()))
                .andExpect(status().isForbidden)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"mfaMethods\":[\"totp\"]}"))
    }

    @Test
    fun `When user login with invalid mfa credentials and mfa enabled then return 403`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com",
                "password12$",
                "Georgi",
                mutableListOf(authority),
                true,
                true,
                mutableListOf(mfaMethodEmail, mfaMethodTotp))

        val request = mutableMapOf<String, String>()
        request[Constants.USERNAME_PARAMETER] = user.email
        request[Constants.PASSWORD_PARAMETER] = user.password
        request[Constants.MFA_CODE_PARAMETER] = "656565"
        request[Constants.MFA_METHOD_PARAMETER] = "totp"

        val dbUser = User(user.email,
                passwordEncoder.encode("password12$"),
                user.username,
                mutableListOf(authority),
                user.isEmailVerified,
                user.isMfaActive,
                user.mfaMethods)

        val expectedUserDetails = CustomUserDetails(dbUser)

        every { userDetailsServiceImpl.loadUserByUsername(any()) } returns expectedUserDetails
        every { mfaTotpService.verifyMfaCode(any(), any()) } returns false

        mockMvc.perform(post("/account/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJSON(request))
                .with(csrf()))
                .andExpect(status().isForbidden)
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Bad Multi-factor authentication code"))
    }

    @Test
    fun `When user login with send email mfa credentials and mfa enabled then return 403`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com",
                "password12$",
                "Georgi",
                mutableListOf(authority),
                true,
                true,
                mutableListOf(mfaMethodEmail, mfaMethodTotp))
        val code = "65abv7"
        val id = 5
        val expectedMfaToken = MfaEmailToken(id = id.toString(), code)

        val request = mutableMapOf<String, String>()
        request[Constants.USERNAME_PARAMETER] = user.email
        request[Constants.PASSWORD_PARAMETER] = user.password
        request[Constants.MFA_CODE_PARAMETER] = "656565"
        request[Constants.MFA_METHOD_PARAMETER] = "email"
        request[Constants.MFA_SHOULD_SEND_MFA_EMAIL] = "true"

        val dbUser = User(user.email,
                passwordEncoder.encode("password12$"),
                user.username,
                mutableListOf(authority),
                user.isEmailVerified,
                user.isMfaActive,
                user.mfaMethods)

        val expectedUserDetails = CustomUserDetails(dbUser)

        every { userDetailsServiceImpl.loadUserByUsername(any()) } returns expectedUserDetails
        every { mfaEmailTokenService.createMfaEmailToken(any()) } returns expectedMfaToken
        every { mfaEmailTokenService.saveEmailToken(any()) } returns Unit
        every { mfaEmailTokenService.sendMfaEmail(any()) } returns Unit


        mockMvc.perform(post("/account/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJSON(request))
                .with(csrf()))
                .andExpect(status().isForbidden)
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Sent mfa email"))
    }

    @Test
    fun `When verify email code valid then return 201`() {
        val token = EmailVerifyDto("code")

        every { emailVerificationService.verifyEmail("code", any()) } returns true

        mockMvc.perform(post("/account/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJSON(token)))
                .andExpect(status().isCreated)
    }

    @Test
    fun `When verify email code invalid then return 403`() {
        val token = EmailVerifyDto("code")

        every { emailVerificationService.verifyEmail("code", any()) } returns false

        mockMvc.perform(post("/account/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJSON(token)))
                .andExpect(status().isForbidden)
    }
}