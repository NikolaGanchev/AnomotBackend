package com.anomot.anomotbackend

import com.anomot.anomotbackend.controllers.AuthController
import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.dto.UserRegisterDto
import com.anomot.anomotbackend.entities.Authority
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.exceptions.UserAlreadyExistsException
import com.anomot.anomotbackend.security.Authorities
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.security.WebSecurityConfig
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
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

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
        val expectedResult = UserDto("example@test.com", "Georgi", mutableListOf(authority.authority))

        every { userDetailsServiceImpl.createUser(user) } returns expectedResult

        mockMvc.perform(post("/account/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.objectToJSON(user)))
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
                .content(TestUtils.objectToJSON(user)))
                .andExpect(status().isConflict)
    }

    @Test
    fun `When user login then return 200 and user`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))

        val dbUser = User("example@test.com",
                passwordEncoder.encode("password12$"),
                "Georgi",
                mutableListOf(authority))

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
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))

        every { userDetailsServiceImpl.loadUserByUsername(any()) } throws UsernameNotFoundException("Email not found")

        mockMvc.perform(formLogin("/account/login").user(Constants.USERNAME_PARAMETER, user.email)
                .password(Constants.PASSWORD_PARAMETER, user.password)
                .acceptMediaType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden)
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Login error"))
    }
}