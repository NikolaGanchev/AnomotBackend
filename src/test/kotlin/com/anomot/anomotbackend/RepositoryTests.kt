package com.anomot.anomotbackend

import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.security.Authorities
import com.anomot.anomotbackend.security.MfaMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*


@DataJpaTest
class RepositoryTests @Autowired constructor(
        val entityManager: TestEntityManager,
        val userRepository: UserRepository,
        val authorityRepository: AuthorityRepository,
        val emailVerificationTokenRepository: EmailVerificationTokenRepository,
        val mfaMethodRepository: MfaMethodRepository,
        val mfaTotpSecretRepository: MfaTotpSecretRepository
) {

    @Test
    fun `When findByEmail then return User`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))

        entityManager.persist(user)
        entityManager.flush()
        val foundUser = userRepository.findByEmail(user.email)

        assertThat(foundUser).isEqualTo(user)
    }

    @Test
    fun `When findByAuthority then return authority`() {
        val authority = Authority(Authorities.USER.roleName)

        entityManager.persist(authority)
        entityManager.flush()
        val foundAuthority = authorityRepository.findByAuthority(Authorities.USER.roleName)

        assertThat(foundAuthority).isEqualTo(authority)
    }

    @Test
    fun `When setIsEmailVerifiedByEmail then return 1`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))

        entityManager.persist(user)
        entityManager.flush()
        val editedRows = userRepository.setIsEmailVerifiedByEmail(true, user.email)

        assertThat(editedRows).isEqualTo(1)
    }

    @Test
    fun `When findByVerificationCode then return token`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))

        entityManager.persist(user)
        entityManager.flush()

        val expiryDate = Date.from(OffsetDateTime.now( ZoneOffset.UTC )
                .plusDays(1).toInstant())
        val token = EmailVerificationToken("test", user, expiryDate)

        entityManager.persist(token)
        entityManager.flush()

        val result = emailVerificationTokenRepository.findByVerificationCode(token.verificationCode)

        assertThat(result).isEqualTo(token)
    }

    @Test
    fun `When delete old tokens then remove`() {
        val authority = Authority(Authorities.USER.roleName)
        val user1 = User("example@example1.com", "password", "Georgi", mutableListOf(authority))
        val user2 = User("example@example2.com", "password", "Georgi", mutableListOf(authority))
        val user3 = User("example@example3.com", "password", "Georgi", mutableListOf(authority))

        entityManager.persist(user1)
        entityManager.persist(user2)
        entityManager.persist(user3)
        entityManager.flush()

        val expiryDate = Date.from(Instant.now())

        val token1 = EmailVerificationToken("test1", user1, expiryDate)
        val token2 = EmailVerificationToken("test2", user2, expiryDate)
        val token3 = EmailVerificationToken("test3", user3, expiryDate)

        entityManager.persist(token1)
        entityManager.persist(token2)
        entityManager.persist(token3)
        entityManager.flush()

        val currentDateForward = Date.from(OffsetDateTime.now( ZoneOffset.UTC )
                .plusDays(1).toInstant())

        val editedRows = emailVerificationTokenRepository.deleteOldTokens(currentDateForward)
        val numberOfRows = emailVerificationTokenRepository.count()

        assertThat(editedRows).isEqualTo(3)
        assertThat(numberOfRows).isEqualTo(0)
    }

    @Test
    fun `When findByMethod then return mfaMethod`() {
        val mfaMethod = MfaMethod(MfaMethod.TOTP.method)

        entityManager.persist(mfaMethod)
        entityManager.flush()

        val result = mfaMethodRepository.findByMethod(MfaMethod.TOTP.method)

        assertThat(result).isEqualTo(mfaMethod)
    }

    @Test
    fun `When findByEmail then return mfaTotpSecret`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))

        val mfaTotpSecret = MfaTotpSecret("secret", user)

        entityManager.persist(user)
        entityManager.persist(mfaTotpSecret)
        entityManager.flush()

        val result = mfaTotpSecretRepository.findByEmail(user.email)

        assertThat(result).isEqualTo(mfaTotpSecret)
    }
}