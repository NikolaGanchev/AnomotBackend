package com.anomot.anomotbackend.exceptions

import org.springframework.security.core.AuthenticationException

class BadRecoveryCodeException(message: String) : AuthenticationException(message)
