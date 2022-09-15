package com.anomot.anomotbackend

import com.anomot.anomotbackend.entities.Authority
import com.anomot.anomotbackend.entities.MfaEmailCode
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.MfaEmailCodeRepository
import com.anomot.anomotbackend.security.Authorities
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest

@DataRedisTest
class RedisRepositoryTests @Autowired constructor(
        private val mfaEmailCodeRepository: MfaEmailCodeRepository
) {

    @Test
    fun `When findByCode then return email`() {
        val authority = Authority(Authorities.USER.roleName)
        val user = User("example@example.com", "password", "Georgi", mutableListOf(authority))
        val code = "12ab79"
        val mfaEmailCode =  MfaEmailCode(id = code, email = user.email)

        mfaEmailCodeRepository.save(mfaEmailCode)

        val result = mfaEmailCodeRepository.findById(code)

        assertThat(result.get().id).isEqualTo(mfaEmailCode.id)
        assertThat(result.get().email).isEqualTo(mfaEmailCode.email)
    }
}