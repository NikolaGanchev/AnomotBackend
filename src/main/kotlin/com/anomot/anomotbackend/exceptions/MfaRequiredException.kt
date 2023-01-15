package com.anomot.anomotbackend.exceptions

import org.springframework.security.core.AuthenticationException

class MfaRequiredException(message: String): AuthenticationException(message)