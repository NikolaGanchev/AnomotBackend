package com.anomot.anomotbackend

import com.anomot.anomotbackend.entities.Authority
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.AuthorityRepository
import com.anomot.anomotbackend.repositories.UserRepository
import com.anomot.anomotbackend.security.Authorities
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager


@DataJpaTest
class RepositoryTests @Autowired constructor(
        val entityManager: TestEntityManager,
        val userRepository: UserRepository,
        val authorityRepository: AuthorityRepository
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

}