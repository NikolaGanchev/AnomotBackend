package com.anomot.anomotbackend

import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.security.Authorities
import com.anomot.anomotbackend.security.MfaMethodValue
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
        val mfaTotpSecretRepository: MfaTotpSecretRepository,
        val mfaRecoveryCodeRepository: MfaRecoveryCodeRepository
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
    fun `When setPassword then return 1`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))

        entityManager.persist(user)
        entityManager.flush()
        val editedRows = userRepository.setPassword("newPassword", user.id!!)

        assertThat(editedRows).isEqualTo(1)
    }

    @Test
    fun `When setEmail then return 1`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))

        entityManager.persist(user)
        entityManager.flush()
        val editedRows = userRepository.setEmail("example@test.com", user.id!!)

        assertThat(editedRows).isEqualTo(1)
    }

    @Test
    fun `When setUsername then return 1`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))

        entityManager.persist(user)
        entityManager.flush()
        val editedRows = userRepository.setUsername("newUsername", user.id!!)

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
        val mfaMethodValue = MfaMethod(MfaMethodValue.TOTP.method)

        entityManager.persist(mfaMethodValue)
        entityManager.flush()

        val result = mfaMethodRepository.findByMethod(MfaMethodValue.TOTP.method)

        assertThat(result).isEqualTo(mfaMethodValue)
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

    @Test
    fun `When deleteByEmail mfaTotpSecret then have 0 elements`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))

        val mfaTotpSecret = MfaTotpSecret("secret", user)

        entityManager.persist(user)
        entityManager.persist(mfaTotpSecret)
        entityManager.flush()

        mfaTotpSecretRepository.deleteByUserId(user.id!!)

        assertThat(mfaTotpSecretRepository.count()).isEqualTo(0)
    }

    @Test
    fun `When existsByUserAndCode and does exist then return true`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))
        val code = "9m96n5"

        val mfaRecoveryCode = MfaRecoveryCode(code, user)

        entityManager.persist(user)
        entityManager.persist(mfaRecoveryCode)
        entityManager.flush()

        val exists = mfaRecoveryCodeRepository.existsByUserAndCode(user, code)

        assertThat(exists).isTrue
    }

    @Test
    fun `When existsByUserAndCode and doesn't exist then return true`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))
        val code = "9m96n5"

        val mfaRecoveryCode = MfaRecoveryCode(code, user)

        entityManager.persist(user)
        entityManager.persist(mfaRecoveryCode)
        entityManager.flush()

        val exists = mfaRecoveryCodeRepository.existsByUserAndCode(user,"9b76i5")

        assertThat(exists).isFalse
    }

    @Test
    fun `When delete by user and code then return 1`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))
        val code = "9m96n5"

        val mfaRecoveryCode = MfaRecoveryCode(code, user)

        entityManager.persist(user)
        entityManager.persist(mfaRecoveryCode)
        entityManager.flush()

        val deletedRows = mfaRecoveryCodeRepository.deleteByUserAndCode(user, code)

        assertThat(deletedRows).isEqualTo(1)
    }

    @Test
    fun `When delete all by user then have 0 elements`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))
        val code = "9896y5"
        val code1 = "3475w4"

        val mfaRecoveryCode = MfaRecoveryCode(code, user)
        val mfaRecoveryCode1 = MfaRecoveryCode(code1, user)

        entityManager.persist(user)
        entityManager.persist(mfaRecoveryCode)
        entityManager.persist(mfaRecoveryCode1)
        entityManager.flush()

        val deletedRows = mfaRecoveryCodeRepository.deleteAllByUser(user)

        assertThat(deletedRows).isEqualTo(2)
        assertThat(mfaTotpSecretRepository.count()).isEqualTo(0)
    }
}