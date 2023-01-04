package com.anomot.anomotbackend

import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.security.Authorities
import com.anomot.anomotbackend.security.MfaMethodValue
import com.anomot.anomotbackend.utils.Constants
import com.anomot.anomotbackend.utils.PostType
import com.anomot.anomotbackend.utils.TimeUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = ["spring.datasource.url=jdbc:h2:mem:anomot;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;"])
class RepositoryTests @Autowired constructor(
        val entityManager: TestEntityManager,
        val userRepository: UserRepository,
        val authorityRepository: AuthorityRepository,
        val emailVerificationTokenRepository: EmailVerificationTokenRepository,
        val mfaMethodRepository: MfaMethodRepository,
        val mfaTotpSecretRepository: MfaTotpSecretRepository,
        val mfaRecoveryCodeRepository: MfaRecoveryCodeRepository,
        val passwordResetTokenRepository: PasswordResetTokenRepository,
        val rememberMeTokenRepository: RememberMeTokenRepository,
        val successfulLoginRepository: SuccessfulLoginRepository,
        val followRepository: FollowRepository,
        val postRepository: PostRepository,
        val battleQueueRepository: BattleQueueRepository,
        val battleRepository: BattleRepository
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
    fun `When existsByEmail then return true`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))

        entityManager.persist(user)
        entityManager.flush()

        val result = userRepository.existsByEmail(user.email)

        assertThat(result).isTrue
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
    fun `When getAllByUser then return all codes`() {
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

        val codes = mfaRecoveryCodeRepository.getAllByUser(user)
        val codeStrings = codes?.map {
            it.code
        }

        assertThat(codes).isNotNull
        assertThat(codeStrings!!.contains(code)).isTrue
        assertThat(codeStrings.contains(code1)).isTrue
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

    @Test
    fun `When findByIdentifier then return token`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))

        entityManager.persist(user)
        entityManager.flush()

        val expiryDate = TimeUtils.generateFutureAfterMinutes(60)
        val identifier = UUID.randomUUID().toString()
        val token = PasswordResetToken("test", identifier, user, expiryDate)

        entityManager.persist(token)
        entityManager.flush()

        val result = passwordResetTokenRepository.findByIdentifier(identifier)

        assertThat(result).isEqualTo(token)
    }

    @Test
    fun `When delete old password reset tokens then remove`() {
        val authority = Authority(Authorities.USER.roleName)
        val user1 = User("example@example1.com", "password", "Georgi", mutableListOf(authority))
        val user2 = User("example@example2.com", "password", "Georgi", mutableListOf(authority))
        val user3 = User("example@example3.com", "password", "Georgi", mutableListOf(authority))

        entityManager.persist(user1)
        entityManager.persist(user2)
        entityManager.persist(user3)
        entityManager.flush()

        val expiryDate = Date.from(Instant.now())

        val token1 = PasswordResetToken("test1", "id1", user1, expiryDate)
        val token2 = PasswordResetToken("test2", "id2", user2, expiryDate)
        val token3 = PasswordResetToken("test3", "id3", user3, expiryDate)

        entityManager.persist(token1)
        entityManager.persist(token2)
        entityManager.persist(token3)
        entityManager.flush()

        val currentDateForward = Date.from(OffsetDateTime.now( ZoneOffset.UTC )
                .plusDays(1).toInstant())

        val editedRows = passwordResetTokenRepository.deleteOldTokens(currentDateForward)
        val numberOfRows = passwordResetTokenRepository.count()

        assertThat(editedRows).isEqualTo(3)
        assertThat(numberOfRows).isEqualTo(0)
    }

    @Test
    fun `When findBySeries then return rememberMeToken`() {
        val rememberMeToken = RememberMeToken("egge", "sdgsgd", "example@example.com", Date())

        entityManager.persist(rememberMeToken)
        entityManager.flush()

        val result = rememberMeTokenRepository.findBySeries(rememberMeToken.series)

        assertThat(result).isEqualTo(rememberMeToken)
    }

    @Test
    fun `When updateBySeries then update`() {
        val rememberMeToken = RememberMeToken("egge", "sdgsgd", "example@example.com", Date())
        val newValue = "sdfgsf"
        val newDate = Date()

        entityManager.persist(rememberMeToken)
        entityManager.flush()

        val editedRows = rememberMeTokenRepository.updateBySeries(rememberMeToken.series, newValue, newDate)

        assertThat(editedRows).isEqualTo(1)
    }

    @Test
    fun `When deleteAllByEmail then delete`() {
        val email = "example@example.com"
        val rememberMeToken = RememberMeToken("egge", "sdgsgd", email, Date())
        val rememberMeToken1 = RememberMeToken("egge1", "sdgsgd1", email, Date())
        val rememberMeToken2 = RememberMeToken("egge2", "sdgsgd2", email, Date())

        entityManager.persist(rememberMeToken)
        entityManager.persist(rememberMeToken1)
        entityManager.persist(rememberMeToken2)
        entityManager.flush()

        val deletedRows = rememberMeTokenRepository.deleteAllByEmail(email)
        val rows = rememberMeTokenRepository.count()

        assertThat(deletedRows).isEqualTo(3)
        assertThat(rows).isEqualTo(0)
    }

    @Test
    fun `When deleteOldTokens then delete`() {
        val oldDate = TimeUtils.generatePastMinutesAgo(Constants.REMEMBER_ME_VALIDITY_DURATION / 60 + 5)
        val rememberMeToken = RememberMeToken("egge", "sdgsgd", "example@example.com", oldDate)

        entityManager.persist(rememberMeToken)
        entityManager.flush()

        val deletedRows = rememberMeTokenRepository.deleteOldTokens(Date())
        val rows = rememberMeTokenRepository.count()

        assertThat(deletedRows).isEqualTo(1)
        assertThat(rows).isEqualTo(0)
    }

    @Test
    fun `When findAllByUser return successful logins`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))
        val login = SuccessfulLogin("l", "", "", "", "", "", "", Date.from(Instant.EPOCH), user)
        val login1 = SuccessfulLogin("l1", "", "", "", "", "", "", Date.from(Instant.now()), user)

        entityManager.persist(user)
        entityManager.persist(login)
        entityManager.persist(login1)

        val pageRequest = PageRequest.of(0, 20, Sort.by("date").descending())
        val result = successfulLoginRepository.findAllByUser(user, pageRequest)

        assertThat(result[0].city).isEqualTo("l1")
        assertThat(result[1].city).isEqualTo("l")
    }

    @Test
    fun `When get followers then return followers`() {
        val authority = Authority(Authorities.USER.roleName)
        var user = User("example@example.com", "password", "Georgi", mutableListOf(authority))
        var user1 = User("example1@example.com", "password", "Georgi", mutableListOf(authority))
        var user2 = User("example2@example.com", "password", "Georgi", mutableListOf(authority))

        user = entityManager.persist(user)
        user1 = entityManager.persist(user1)
        user2 = entityManager.persist(user2)
        entityManager.flush()

        val follow1 = Follow(user, user1)
        val follow2 = Follow(user, user2)
        val follow3 = Follow(user2, user1)
        val follow4 = Follow(user1, user)

        entityManager.persist(follow1)
        entityManager.persist(follow2)
        entityManager.persist(follow3)
        entityManager.persist(follow4)

        val followerNumber = followRepository.countFollowsByFollowed(user)
        val followers = followRepository.getFollowsByFollowed(user)

        assertThat(followerNumber).isEqualTo(2)
        assertThat(followers.contains(Follow(user, user1)))
        assertThat(followers.contains(Follow(user, user2)))
    }

    @Test
    fun `When get follows then return follows`() {
        val authority = Authority(Authorities.USER.roleName)
        var user = User("example@example.com", "password", "Georgi", mutableListOf(authority))
        var user1 = User("example1@example.com", "password", "Georgi", mutableListOf(authority))
        var user2 = User("example2@example.com", "password", "Georgi", mutableListOf(authority))

        user = entityManager.persist(user)
        user1 = entityManager.persist(user1)
        user2 = entityManager.persist(user2)
        entityManager.flush()

        val follow1 = Follow(user, user1)
        val follow2 = Follow(user, user2)

        entityManager.persist(follow1)
        entityManager.persist(follow2)

        val followedNumber = followRepository.countFollowsByFollower(user1)
        val followed = followRepository.getFollowsByFollower(user1)

        assertThat(followedNumber).isEqualTo(1)
        assertThat(followed.contains(Follow(user, user1)))
    }

    @Test
    fun `When search battles find most similar elo of same type`() {
        val authority = Authority(Authorities.USER.roleName)
        var user = User("example@example.com", "password", "Georgi", mutableListOf(authority), elo = 2400)
        var user1 = User("example1@example.com", "password", "Georgi", mutableListOf(authority), elo = 1700)
        var user2 = User("example2@example.com", "password", "Georgi", mutableListOf(authority), elo = 1100)
        var user3 = User("example3@example.com", "password", "Georgi", mutableListOf(authority), elo = 1600)
        var user4 = User("example3@example.com", "password", "Georgi", mutableListOf(authority), elo = 600)

        user = entityManager.persist(user)
        user1 = entityManager.persist(user1)
        user2 = entityManager.persist(user2)
        user3 = entityManager.persist(user3)
        user4 = entityManager.persist(user4)
        entityManager.flush()

        val post = entityManager.persist(Post(user, null, null, PostType.MEDIA))
        val post1 = entityManager.persist(Post(user1, null, null, PostType.MEDIA))
        val post2 = entityManager.persist(Post(user2, null, null, PostType.MEDIA))
        val post3 = entityManager.persist(Post(user3, null, null, PostType.TEXT))
        val post4 = entityManager.persist(Post(user4, null, null, PostType.MEDIA))
        entityManager.flush()

        val battleQueuePost = BattleQueuePost(post)
        val battleQueuePost1 = BattleQueuePost(post1)
        val battleQueuePost2 = BattleQueuePost(post2)
        val battleQueuePost3 = BattleQueuePost(post3)
        val battleQueuePost4 = BattleQueuePost(post4)

        entityManager.persist(battleQueuePost)
        entityManager.persist(battleQueuePost1)
        entityManager.persist(battleQueuePost2)
        entityManager.persist(battleQueuePost3)
        entityManager.persist(battleQueuePost4)
        entityManager.flush()

        val candidates = battleQueueRepository.findSimilarByElo(battleQueuePost1)

        assertThat(candidates.size).isEqualTo(2)
        assertThat(candidates[0].post).isEqualTo(post2)
        assertThat(candidates[1].post).isEqualTo(post)
    }
}