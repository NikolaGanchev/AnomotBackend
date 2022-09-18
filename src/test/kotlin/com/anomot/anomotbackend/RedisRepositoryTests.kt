package com.anomot.anomotbackend

import com.anomot.anomotbackend.entities.MfaEmailToken
import com.anomot.anomotbackend.repositories.MfaEmailCodeRepository
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
        val code = "12ab79"
        val mfaEmailToken = MfaEmailToken(id = "5", code = code)

        mfaEmailCodeRepository.save(mfaEmailToken)

        val result = mfaEmailCodeRepository.findById("5")

        assertThat(result.get().id).isEqualTo(mfaEmailToken.id)
        assertThat(result.get().code).isEqualTo(mfaEmailToken.code)
    }
}