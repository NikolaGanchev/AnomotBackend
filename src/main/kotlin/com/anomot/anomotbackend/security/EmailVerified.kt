package com.anomot.anomotbackend.security

import org.springframework.security.access.prepost.PreAuthorize

@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("authentication.principal.isEmailVerified == true")
annotation class EmailVerified