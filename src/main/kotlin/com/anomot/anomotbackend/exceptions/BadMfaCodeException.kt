package com.anomot.anomotbackend.exceptions

import org.springframework.security.core.AuthenticationException

class BadMfaCodeException(message: String): AuthenticationException(message) {
}