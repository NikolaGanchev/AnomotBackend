package com.anomot.anomotbackend

import com.anomot.anomotbackend.controllers.AuthController
import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.exceptions.UserAlreadyExistsException
import com.anomot.anomotbackend.security.*
import com.anomot.anomotbackend.services.*
import com.anomot.anomotbackend.utils.Constants
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
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

    @MockkBean
    private lateinit var authenticationService: AuthenticationService

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
                .content(TestUtils.objectToJson(user))
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
                .content(TestUtils.objectToJson(user)))
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
    fun `When user login with bad credentials then return 401`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority), true)

        every { userDetailsServiceImpl.loadUserByUsername(any()) } throws UsernameNotFoundException("Email not found")

        mockMvc.perform(formLogin("/account/login").user(Constants.USERNAME_PARAMETER, user.email)
                .password(Constants.PASSWORD_PARAMETER, user.password)
                .acceptMediaType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized)
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
    fun `When user login with wrong mfa method and mfa enabled then return 401`() {
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
                .content(TestUtils.objectToJson(request))
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }

    @Test
    fun `When get mfa methods with correct credentials then return 200`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com",
                "password12$",
                "Georgi",
                mutableListOf(authority),
                true,
                true,
                mutableListOf(mfaMethodEmail, mfaMethodTotp))

        val loginDto = LoginDto("example@example.com", "password12$")

        val dbUser = User(user.email,
                passwordEncoder.encode("password12$"),
                user.username,
                mutableListOf(authority),
                user.isEmailVerified,
                user.isMfaActive,
                user.mfaMethods)

        val expectedUserDetails = CustomUserDetails(dbUser)

        every { userDetailsServiceImpl.loadUserByUsername(any()) } returns expectedUserDetails
        every { authenticationService.verifyAuthenticationWithoutMfa(any<String>(), any()) } returns mockk()

        mockMvc.perform(post("/account/mfa/email/methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(loginDto))
                .with(csrf()))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"mfaMethods\":[\"email\",\"totp\"]}"))
    }

    @Test
    fun `When get mfa methods with incorrect credentials then return 401`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com",
                "password12$",
                "Georgi",
                mutableListOf(authority),
                true,
                true,
                mutableListOf(mfaMethodTotp))

        val loginDto = LoginDto("example@example.com", "password12$")

        val dbUser = User(user.email,
                passwordEncoder.encode("password12$"),
                user.username,
                mutableListOf(authority),
                user.isEmailVerified,
                user.isMfaActive,
                user.mfaMethods)

        val expectedUserDetails = CustomUserDetails(dbUser)

        every { userDetailsServiceImpl.loadUserByUsername(any()) } returns expectedUserDetails
        every { authenticationService.verifyAuthenticationWithoutMfa(any<String>(), any()) } returns null

        mockMvc.perform(post("/account/mfa/email/methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(loginDto))
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }

    @Test
    fun `When get mfa methods with no mfa enabled then return 409`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com",
                "password12$",
                "Georgi",
                mutableListOf(authority),
                true,
                false,
                mutableListOf(mfaMethodTotp))

        val loginDto = LoginDto("example@example.com", "password12$")

        val dbUser = User(user.email,
                passwordEncoder.encode("password12$"),
                user.username,
                mutableListOf(authority),
                user.isEmailVerified,
                user.isMfaActive,
                user.mfaMethods)

        val expectedUserDetails = CustomUserDetails(dbUser)

        every { userDetailsServiceImpl.loadUserByUsername(any()) } returns expectedUserDetails
        every { authenticationService.verifyAuthenticationWithoutMfa(any<String>(), any()) } returns mockk()

        mockMvc.perform(post("/account/mfa/email/methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(loginDto))
                .with(csrf()))
                .andExpect(status().isConflict)
    }

    @Test
    fun `When get mfa status with correct credentials and mfa enabled then return 200 and true`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com",
                "password12$",
                "Georgi",
                mutableListOf(authority),
                true,
                true,
                mutableListOf(mfaMethodEmail, mfaMethodTotp))

        val loginDto = LoginDto("example@example.com", "password12$")

        val dbUser = User(user.email,
                passwordEncoder.encode("password12$"),
                user.username,
                mutableListOf(authority),
                user.isEmailVerified,
                user.isMfaActive,
                user.mfaMethods)

        val expectedUserDetails = CustomUserDetails(dbUser)

        every { userDetailsServiceImpl.loadUserByUsername(any()) } returns expectedUserDetails
        every { authenticationService.verifyAuthenticationWithoutMfa(any<String>(), any()) } returns mockk()

        mockMvc.perform(post("/account/mfa/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(loginDto))
                .with(csrf()))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.isMfaEnabled").value(true))
    }

    @Test
    fun `When get mfa status with correct credentials and mfa disabled then return 200 and false`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com",
                "password12$",
                "Georgi",
                mutableListOf(authority),
                true,
                false,
                mutableListOf(mfaMethodEmail, mfaMethodTotp))

        val loginDto = LoginDto("example@example.com", "password12$")

        val dbUser = User(user.email,
                passwordEncoder.encode("password12$"),
                user.username,
                mutableListOf(authority),
                user.isEmailVerified,
                user.isMfaActive,
                user.mfaMethods)

        val expectedUserDetails = CustomUserDetails(dbUser)

        every { userDetailsServiceImpl.loadUserByUsername(any()) } returns expectedUserDetails
        every { authenticationService.verifyAuthenticationWithoutMfa(any<String>(), any()) } returns mockk()

        mockMvc.perform(post("/account/mfa/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(loginDto))
                .with(csrf()))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.isMfaEnabled").value(false))
    }

    @Test
    fun `When get mfa status with incorrect credentials then return 401`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com",
                "password12$",
                "Georgi",
                mutableListOf(authority),
                true,
                true,
                mutableListOf(mfaMethodTotp))

        val loginDto = LoginDto("example@example.com", "password12$")

        val dbUser = User(user.email,
                passwordEncoder.encode("password12$"),
                user.username,
                mutableListOf(authority),
                user.isEmailVerified,
                user.isMfaActive,
                user.mfaMethods)

        val expectedUserDetails = CustomUserDetails(dbUser)

        every { userDetailsServiceImpl.loadUserByUsername(any()) } returns expectedUserDetails
        every { authenticationService.verifyAuthenticationWithoutMfa(any<String>(), any()) } returns null

        mockMvc.perform(post("/account/mfa/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(loginDto))
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }

    @Test
    fun `When user login with invalid mfa credentials and mfa enabled then return 401`() {
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
                .content(TestUtils.objectToJson(request))
                .with(csrf()))
                .andExpect(status().isUnauthorized)
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Bad Multi-factor authentication code"))
    }

    @Test
    fun `When request mfa email with mfa enabled then return 200`() {
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

        val loginDto = LoginDto("example@example.com", "password12$")

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
        every { authenticationService.verifyAuthenticationWithoutMfa(any<String>(), any()) } returns mockk()


        mockMvc.perform(post("/account/mfa/email/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(loginDto))
                .with(csrf()))
                .andExpect(status().isOk)
    }

    @Test
    fun `When request mfa email with mfa disabled then return 409`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com",
                "password12$",
                "Georgi",
                mutableListOf(authority),
                true,
                false,
                mutableListOf(mfaMethodEmail, mfaMethodTotp))
        val code = "65abv7"
        val id = 5
        val expectedMfaToken = MfaEmailToken(id = id.toString(), code)

        val loginDto = LoginDto("example@example.com", "password12$")

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
        every { authenticationService.verifyAuthenticationWithoutMfa(any<String>(), any()) } returns mockk()


        mockMvc.perform(post("/account/mfa/email/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(loginDto))
                .with(csrf()))
                .andExpect(status().isConflict)
    }

    @Test
    fun `When request mfa email with incorrect credentials then return 401`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com",
                "password12$",
                "Georgi",
                mutableListOf(authority),
                true,
                false,
                mutableListOf(mfaMethodEmail, mfaMethodTotp))
        val code = "65abv7"
        val id = 5
        val expectedMfaToken = MfaEmailToken(id = id.toString(), code)

        val loginDto = LoginDto("example@example.com", "password12$")

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
        every { authenticationService.verifyAuthenticationWithoutMfa(any<String>(), any()) } returns null


        mockMvc.perform(post("/account/mfa/email/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(loginDto))
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }

    @Test
    fun `When verify email code valid then return 201`() {
        val token = EmailVerifyDto("code")

        every { emailVerificationService.verifyEmail("code", any()) } returns true

        mockMvc.perform(post("/account/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(token)))
                .andExpect(status().isCreated)
    }

    @Test
    fun `When verify email code invalid then return 401`() {
        val token = EmailVerifyDto("code")

        every { emailVerificationService.verifyEmail("code", any()) } returns false

        mockMvc.perform(post("/account/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(token)))
                .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockCustomUser
    fun `When change password and successful then return 200`() {
        val passwordChangeDto = PasswordChangeDto("oldPassword1$", "newPassword1$")

        every { userDetailsServiceImpl.changePassword(any(), any()) } returns Unit

        mockMvc.perform(put("/account/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(passwordChangeDto))
                .with(csrf()))
                .andExpect(status().isOk)
    }

    @Test
    fun `When change password and access denied then return 401`() {
        val passwordChangeDto = PasswordChangeDto("oldPassword1$", "newPassword1$")

        every { userDetailsServiceImpl.changePassword(any(), any()) } throws AccessDeniedException("No authentication present")

        mockMvc.perform(put("/account/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(passwordChangeDto))
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockCustomUser
    fun `When change password and incorrect old password then return 401`() {
        val passwordChangeDto = PasswordChangeDto("oldPassword1$", "newPassword1$")

        every { userDetailsServiceImpl.changePassword(any(), any()) } throws BadCredentialsException("Bad credentials")

        mockMvc.perform(put("/account/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(passwordChangeDto))
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockCustomUser
    fun `When change email and successful then return 200`() {
        val emailChangeDto = EmailChangeDto("example@example.com", "password1$")

        every { userDetailsServiceImpl.changeEmail(any(), any()) } returns Unit

        mockMvc.perform(put("/account/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(emailChangeDto))
                .with(csrf()))
                .andExpect(status().isOk)
    }

    @Test
    fun `When change email and access denied then return 401`() {
        val emailChangeDto = EmailChangeDto("example@example.com", "password1$")

        every { userDetailsServiceImpl.changeEmail(any(), any()) } throws AccessDeniedException("No authentication present")

        mockMvc.perform(put("/account/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(emailChangeDto))
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockCustomUser
    fun `When change email and incorrect password then return 401`() {
        val emailChangeDto = EmailChangeDto("example@example.com", "password1$")

        every { userDetailsServiceImpl.changeEmail(any(), any()) } throws BadCredentialsException("Bad credentials")

        mockMvc.perform(put("/account/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(emailChangeDto))
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockCustomUser
    fun `When change username and successful then return 200`() {
        val usernameChangeDto = UsernameChangeDto("Georgi")

        every { userDetailsServiceImpl.changeUsername(any()) } returns Unit

        mockMvc.perform(put("/account/username")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(usernameChangeDto))
                .with(csrf()))
                .andExpect(status().isOk)
    }

    @Test
    fun `When change username and access denied then return 401`() {
        val usernameChangeDto = UsernameChangeDto("Georgi")

        every { userDetailsServiceImpl.changeUsername(any()) } throws AccessDeniedException("No authentication present")

        mockMvc.perform(put("/account/username")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(usernameChangeDto))
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockCustomUser
    fun `When activate totp then return 200 and TotpDto`() {
        every {userDetailsServiceImpl.activateTotpMfa()} returns TotpDto("", "")

        mockMvc.perform(put("/account/mfa/totp")
                .with(csrf()))
                .andExpect(status().isOk)
    }

    @Test
    @WithMockCustomUser
    fun `When activate totp and already active then return 409`() {
        every {userDetailsServiceImpl.activateTotpMfa()} returns null

        mockMvc.perform(put("/account/mfa/totp")
                .with(csrf()))
                .andExpect(status().isConflict)
    }

    @Test
    fun `When activate totp and access denied then return 401`() {
        every {userDetailsServiceImpl.activateTotpMfa()} throws AccessDeniedException("No authentication present")

        mockMvc.perform(put("/account/mfa/totp")
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockCustomUser
    fun `When disable totp then return 200`() {
        every {userDetailsServiceImpl.deactivateTotpMfa()} returns true

        mockMvc.perform(delete("/account/mfa/totp")
                .with(csrf()))
                .andExpect(status().isOk)
    }

    @Test
    @WithMockCustomUser
    fun `When disable totp and already disabled then return 409`() {
        every {userDetailsServiceImpl.deactivateTotpMfa()} returns false

        mockMvc.perform(delete("/account/mfa/totp")
                .with(csrf()))
                .andExpect(status().isConflict)
    }

    @Test
    fun `When disable totp and access denied then return 401`() {
        every {userDetailsServiceImpl.deactivateTotpMfa()} throws AccessDeniedException("No authentication present")

        mockMvc.perform(delete("/account/mfa/totp")
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockCustomUser()
    fun `When activate email mfa then return 200`() {
        every {userDetailsServiceImpl.activateEmailMfa()} returns true

        mockMvc.perform(put("/account/mfa/email")
                .with(csrf()))
                .andExpect(status().isOk)
    }

    @Test
    @WithMockCustomUser
    fun `When activate email mfa and already activated then return 409`() {
        every {userDetailsServiceImpl.activateEmailMfa()} returns false

        mockMvc.perform(put("/account/mfa/email")
                .with(csrf()))
                .andExpect(status().isConflict)
    }

    @Test
    fun `When activate email mfa and access denied then return 401`() {
        every {userDetailsServiceImpl.activateEmailMfa()} throws AccessDeniedException("No authentication present")

        mockMvc.perform(put("/account/mfa/email")
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockCustomUser
    fun `When disable email mfa then return 200`() {
        every {userDetailsServiceImpl.deactivateEmailMfa()} returns true

        mockMvc.perform(delete("/account/mfa/email")
                .with(csrf()))
                .andExpect(status().isOk)
    }

    @Test
    @WithMockCustomUser
    fun `When disable email mfa and already disabled then return 409`() {
        every {userDetailsServiceImpl.deactivateEmailMfa()} returns false

        mockMvc.perform(delete("/account/mfa/email")
                .with(csrf()))
                .andExpect(status().isConflict)
    }

    @Test
    fun `When disable email mfa and access denied then return 401`() {
        every {userDetailsServiceImpl.deactivateEmailMfa()} throws AccessDeniedException("No authentication present")

        mockMvc.perform(delete("/account/mfa/email")
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockCustomUser
    fun `When delete account and successful then return 200`() {
        val accountDeleteDto = AccountDeleteDto("password1$")

        every { userDetailsServiceImpl.deleteUser(any()) } returns Unit

        mockMvc.perform(delete("/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(accountDeleteDto))
                .with(csrf()))
                .andExpect(status().isOk)
    }

    @Test
    fun `When delete account and access denied then return 401`() {
        val accountDeleteDto = AccountDeleteDto("password1$")

        every { userDetailsServiceImpl.deleteUser(any()) } throws AccessDeniedException("No authentication present")

        mockMvc.perform(delete("/account/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(accountDeleteDto))
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockCustomUser
    fun `When delete account and incorrect old password then return 401`() {
        val accountDeleteDto = AccountDeleteDto("password1$")

        every { userDetailsServiceImpl.deleteUser(any()) } throws BadCredentialsException("Bad credentials")

        mockMvc.perform(delete("/account/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJson(accountDeleteDto))
                .with(csrf()))
                .andExpect(status().isUnauthorized)
    }
}