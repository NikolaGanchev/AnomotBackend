package com.anomot.anomotbackend.exceptions

import org.springframework.security.core.AuthenticationException
import java.util.*

class UserBannedException(message: String, val banUntil: Date) : AuthenticationException(message)