package com.anomot.anomotbackend.exceptions

import org.springframework.security.core.AuthenticationException

class MfaEmailSentException(message: String): AuthenticationException(message) {
}