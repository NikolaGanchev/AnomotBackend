package com.anomot.anomotbackend

import com.anomot.anomotbackend.dto.SelfUserDto
import com.anomot.anomotbackend.dto.UserRegisterDto
import com.anomot.anomotbackend.entities.Authority
import com.anomot.anomotbackend.entities.EmailVerificationToken
import com.anomot.anomotbackend.entities.MfaMethod
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.exceptions.UserAlreadyExistsException
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.security.Authorities
import com.anomot.anomotbackend.security.MfaMethodValue
import com.anomot.anomotbackend.services.*
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import java.time.Instant
import java.util.*

@SpringBootTest(properties = ["vote.jwt.private-key=1edbc7fb7bae1628f085f6db259b7de40b887157aea732c7c31f18403a562338"])
class UserDetailsServiceTests {
    @MockkBean
    private lateinit var userRepository: UserRepository
    @MockkBean
    private lateinit var passwordEncoder: Argon2PasswordEncoder
    @MockkBean
    private lateinit var emailVerificationService: EmailVerificationService
    @MockkBean
    private lateinit var totpService: MfaTotpService
    @MockkBean
    private lateinit var mfaMethodRepository: MfaMethodRepository
    @MockkBean
    private lateinit var authorityRepository: AuthorityRepository
    @MockkBean
    private lateinit var authenticationService: AuthenticationService
    @MockkBean
    private lateinit var mfaRecoveryService: MfaRecoveryService
    @MockkBean
    private lateinit var loginInfoExtractorService: LoginInfoExtractorService
    @MockkBean
    private lateinit var mediaService: MediaService
    @MockkBean
    private lateinit var mediaRepository: MediaRepository
    @MockkBean
    private lateinit var nsfwScanRepository: NsfwScanRepository
    @MockkBean
    private lateinit var userModerationService: UserModerationService
    @MockkBean
    private lateinit var voteService: VoteService
    @Autowired
    @InjectMockKs
    private lateinit var userDetailsService: UserDetailsServiceImpl

    @BeforeEach
    fun setup() {
        val authority = Authority(Authorities.USER.roleName)
        val emailVerificationToken = EmailVerificationToken(
                "code",
                User("example@test.com", "password12$", "Georgi", mutableListOf(authority)),
                Date.from(Instant.now()))
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))

        every { emailVerificationService.createEmailVerificationToken(any(), any(), any()) } returns emailVerificationToken
        every { emailVerificationService.generateVerificationCode() } returns "code"
        every { emailVerificationService.saveEmailVerificationToken(any()) } returns emailVerificationToken
        every { emailVerificationService.generateExpiryDate(any(), any()) } returns  Date.from(Instant.now())
        every { emailVerificationService.sendVerificationEmail(any(), any()) } returns Unit
        every { userRepository.findByEmail(any()) } returns user
    }

    @Test
    fun `When create user then return User`() {
        val authority = Authority(Authorities.USER.roleName, users = null, 5)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))
        val expectedResult = SelfUserDto("example@test.com", "Georgi", false, mutableListOf(authority.authority), false, null, null, null)

        every { passwordEncoder.encode(user.password) } returns user.password
        every { userRepository.save(any()) } returns user
        every { userRepository.findByEmail(any()) } returns null
        every { authorityRepository.findByAuthority(any()) } returns authority
        every { authorityRepository.getReferenceById(any()) } returns authority

        val result = userDetailsService.createUser(UserRegisterDto(user.email, user.password, user.username))

        assertThat(result.email).isEqualTo(expectedResult.email)
        assertThat(result.avatarId).isEqualTo(expectedResult.avatarId)
        assertThat(result.isMfaActive).isEqualTo(expectedResult.isMfaActive)
        assertThat(result.isEmailVerified).isEqualTo(expectedResult.isEmailVerified)
        assertThat(result.roles).isEqualTo(expectedResult.roles)
        assertThat(result.username).isEqualTo(expectedResult.username)
    }

    @Test
    fun `When user exists then throw exception`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))

        every { passwordEncoder.encode(user.password) } returns user.password
        every { userRepository.save(any()) } returns user
        every { userRepository.findByEmail(any()) } returns user

        assertThrows<UserAlreadyExistsException>("User already exists") {
            userDetailsService.createUser(UserRegisterDto(user.email, user.password, user.username))
        }
    }

    @Test
    @WithMockCustomUser(password = "oldPassword", isMfaActive = true, mfaMethods = ["totp", "email"])
    fun `When change password with correct credentials then don't throw exceptions`() {
        every {passwordEncoder.encode("newPassword")} returns "newPassword"
        every {userRepository.setPassword(any(), any())} returns 1
        every { authenticationService.verifyAuthenticationWithoutMfa(any<Authentication>(), any() ) } returns
                UsernamePasswordAuthenticationToken.authenticated(null, null, null)
        every { authenticationService.reAuthenticate(any()) } returns Unit

        userDetailsService.changePassword("oldPassword", "newPassword")
    }

    @Test
    @WithMockCustomUser(password = "password", isMfaActive = true, mfaMethods = ["totp", "email"])
    fun `When change password with incorrect credentials then throw bad credentials exception`() {
        every { authenticationService.verifyAuthenticationWithoutMfa(any<Authentication>(), any()) } returns null

        assertThrows<BadCredentialsException> {
            userDetailsService.changePassword("wrongPassword", "newPassword")
        }
    }

    @Test
    fun `When change password without authentication then throw AccessDeniedException`() {
        assertThrows<AccessDeniedException> {
            userDetailsService.changePassword("oldPassword", "newPassword")
        }
    }

    @Test
    @WithMockCustomUser(password = "password", isMfaActive = true, mfaMethods = ["totp", "email"])
    fun `When change email with correct credentials then don't throw exceptions`() {
        every { userRepository.setEmail(any(), any()) } returns 1
        every { authenticationService.verifyAuthenticationWithoutMfa(any<Authentication>(), any() ) } returns
                UsernamePasswordAuthenticationToken.authenticated(null, null, null)
        every { authenticationService.reAuthenticate(any()) } returns Unit
        every { userRepository.existsByEmail(any()) } returns false
        every { userRepository.flush() } returns Unit
        every { userRepository.setIsEmailVerifiedByEmail(any(), any()) } returns 1
        every { mfaMethodRepository.getReferenceById(any()) } returns MfaMethod(MfaMethodValue.TOTP.method)
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com",
                "password",
                "Georgi",
                mutableListOf(authority),
                true, true,
                mutableListOf(MfaMethod(MfaMethodValue.EMAIL.method), MfaMethod(MfaMethodValue.TOTP.method)))
        every { userRepository.findByEmail(any()) } returns user

        val newEmail = "example@test.com"

        userDetailsService.changeEmail("password", newEmail)
    }

    @Test
    @WithMockCustomUser(password = "password", isMfaActive = true, mfaMethods = ["totp", "email"])
    fun `When change email and user exists then throw user already exists`() {
        every { userRepository.existsByEmail(any()) } returns true
        every { authenticationService.verifyAuthenticationWithoutMfa(any<Authentication>(), any() ) } returns
                UsernamePasswordAuthenticationToken.authenticated(null, null, null)

        assertThrows<UserAlreadyExistsException> {
            userDetailsService.changeEmail("password", "example@test.com")
        }
    }

    @Test
    @WithMockCustomUser(password = "password", isMfaActive = true, mfaMethods = ["totp", "email"])
    fun `When change email with incorrect credentials then throw bad credentials exception`() {
        every { authenticationService.verifyAuthenticationWithoutMfa(any<Authentication>(), any()) } returns null

        assertThrows<BadCredentialsException> {
            userDetailsService.changeEmail("wrongPassword", "example@test.com")
        }
    }

    @Test
    fun `When change email without authentication then throw access denied exception`() {
        assertThrows<AccessDeniedException> {
            userDetailsService.changeEmail("password", "example@test.com")
        }
    }

    @Test
    @WithMockCustomUser(password = "password", isMfaActive = true, mfaMethods = ["totp", "email"])
    fun `When change username with authentication then don't throw exceptions`() {
        every {userRepository.setUsername(any(), any())} returns 1
        every { authenticationService.reAuthenticate(any()) } returns Unit

        userDetailsService.changeUsername("Georgi2")
    }

    @Test
    fun `When change username without authentication then throw AccessDeniedException`() {
        assertThrows<AccessDeniedException> {
            userDetailsService.changeUsername("Georgi2")
        }
    }

    @Test
    @WithMockCustomUser(password = "password", isMfaActive = false, mfaMethods = [])
    fun `When activate totp with authentication and inactive mfa then return TotpDto`() {
        val mfaMethod = MfaMethod(MfaMethodValue.TOTP.method, users = null, 5)
        val authority = Authority(Authorities.USER.roleName, users = null, 5)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))
        val secret = "secret".toByteArray()

        every { userRepository.findByEmail(any()) } returns user
        every { mfaMethodRepository.findByMethod(MfaMethodValue.TOTP.method) } returns mfaMethod
        every { totpService.generateSecret() } returns secret
        every { totpService.saveCode(any())} returns Unit
        every { authenticationService.reAuthenticate(any()) } returns Unit
        every { mfaMethodRepository.getReferenceById(any()) } returns mfaMethod

        val result = userDetailsService.activateTotpMfa()

        assertThat(result).isNotNull
    }

    @Test
    @WithMockCustomUser(password = "password", isMfaActive = true, mfaMethods = ["totp"])
    fun `When activate totp with authentication and active mfa then return null`() {
        val mfaMethod = MfaMethod(MfaMethodValue.TOTP.method)
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))
        val secret = "secret".toByteArray()

        every { userRepository.findByEmail(any()) } returns user
        every { mfaMethodRepository.findByMethod(MfaMethodValue.TOTP.method) } returns mfaMethod
        every { totpService.generateSecret() } returns secret
        every { totpService.saveCode(any())} returns Unit

        val result = userDetailsService.activateTotpMfa()

        assertThat(result).isNull()
    }

    @Test
    fun `When activate totp without authentication then throw AccessDeniedException`() {
        assertThrows<AccessDeniedException> {
            userDetailsService.activateTotpMfa()
        }
    }

    @Test
    @WithMockCustomUser(password = "password", isMfaActive = true, mfaMethods = ["totp"])
    fun `When deactivate totp with authentication and active mfa then return true`() {
        val mfaMethod = MfaMethod(MfaMethodValue.TOTP.method, users = null, 5)
        val authority = Authority(Authorities.USER.roleName, users = null, 5)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority),
                        mfaMethods = mutableListOf(MfaMethod(MfaMethodValue.TOTP.method)), id = 5)

        every { userRepository.findByEmail(any()) } returns user
        every { mfaMethodRepository.findByMethod(MfaMethodValue.TOTP.method) } returns mfaMethod
        every { authenticationService.reAuthenticate(any()) } returns Unit
        every { mfaMethodRepository.getReferenceById(any()) } returns mfaMethod
        every { totpService.deleteMfaSecret(any()) } returns Unit

        val result = userDetailsService.deactivateTotpMfa()

        assertThat(result).isTrue
    }

    @Test
    @WithMockCustomUser(password = "password", isMfaActive = false, mfaMethods = [])
    fun `When deactivate totp with authentication and inactive mfa then return false`() {
        val mfaMethod = MfaMethod(MfaMethodValue.TOTP.method)
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))

        every { userRepository.findByEmail(any()) } returns user
        every { mfaMethodRepository.findByMethod(MfaMethodValue.TOTP.method) } returns mfaMethod

        val result = userDetailsService.deactivateTotpMfa()

        assertThat(result).isFalse
    }

    @Test
    fun `When deactivate totp then throw AccessDeniedException`() {
        assertThrows<AccessDeniedException> {
            userDetailsService.deactivateTotpMfa()
        }
    }

    @Test
    @WithMockCustomUser(password = "password", isMfaActive = false)
    fun `When activate email mfa with authentication and inactive mfa then return true`() {
        val mfaMethod = MfaMethod(MfaMethodValue.EMAIL.method,  users = null, 5)
        val authority = Authority(Authorities.USER.roleName, users = null, 5)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))

        every { userRepository.findByEmail(any()) } returns user
        every { mfaMethodRepository.findByMethod(MfaMethodValue.EMAIL.method) } returns mfaMethod
        every { authenticationService.reAuthenticate(any()) } returns Unit
        every { mfaMethodRepository.getReferenceById(any()) } returns mfaMethod

        val result = userDetailsService.activateEmailMfa()

        assertThat(result).isTrue
    }

    @Test
    @WithMockCustomUser(password = "password", isMfaActive = true, mfaMethods = ["email"])
    fun `When activate email mfa with authentication and active mfa then return false`() {
        val mfaMethod = MfaMethod(MfaMethodValue.EMAIL.method)
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))

        every { userRepository.findByEmail(any()) } returns user
        every { mfaMethodRepository.findByMethod(MfaMethodValue.EMAIL.method) } returns mfaMethod

        val result = userDetailsService.activateEmailMfa()

        assertThat(result).isFalse
    }

    @Test
    fun `When activate email mfa then throw AccessDeniedException`() {
        assertThrows<AccessDeniedException> {
            userDetailsService.activateEmailMfa()
        }
    }

    @Test
    @WithMockCustomUser(password = "password", isMfaActive = true, mfaMethods = ["email"])
    fun `When deactivate email mfa with authentication and active mfa then return true`() {
        val mfaMethod = MfaMethod(MfaMethodValue.EMAIL.method, users = null, 5)
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority),
                mfaMethods = mutableListOf(MfaMethod(MfaMethodValue.EMAIL.method)))

        every { userRepository.findByEmail(any()) } returns user
        every { mfaMethodRepository.findByMethod(MfaMethodValue.EMAIL.method) } returns mfaMethod
        every { authenticationService.reAuthenticate(any()) } returns Unit
        every { mfaMethodRepository.getReferenceById(any()) } returns mfaMethod

        val result = userDetailsService.deactivateEmailMfa()

        assertThat(result).isTrue
    }

    @Test
    @WithMockCustomUser(password = "password", isMfaActive = false, mfaMethods = [])
    fun `When deactivate email mfa with authentication and inactive mfa then return false`() {
        val mfaMethod = MfaMethod(MfaMethodValue.EMAIL.method)
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@test.com", "password12$", "Georgi", mutableListOf(authority))

        every { userRepository.findByEmail(any()) } returns user
        every { mfaMethodRepository.findByMethod(MfaMethodValue.EMAIL.method) } returns mfaMethod

        val result = userDetailsService.deactivateTotpMfa()

        assertThat(result).isFalse
    }

    @Test
    fun `When deactivate email mfa then throw AccessDeniedException`() {
        assertThrows<AccessDeniedException> {
            userDetailsService.deactivateEmailMfa()
        }
    }
}