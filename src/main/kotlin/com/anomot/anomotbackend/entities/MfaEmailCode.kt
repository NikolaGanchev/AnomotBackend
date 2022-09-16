package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.Constants
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import java.io.Serializable

@RedisHash("MfaEmailCode", timeToLive = Constants.MFA_EMAIL_CODE_LIFETIME)
class MfaEmailCode(
        @Id var email: String,
        var code: String
): Serializable