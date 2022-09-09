package com.anomot.anomotbackend

import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager


@DataJpaTest
class LoginTests @Autowired constructor(
        val entityManager: TestEntityManager,
        val userRepository: UserRepository
) {

    @Test
    fun `When findByEmail then return User`() {
        val user = User("example@example.com", "password", "Georgi")

        entityManager.persist(user)
        entityManager.flush()
        val foundUser = userRepository.findByEmail(user.email)

        assertThat(foundUser).isEqualTo(user)
    }

}