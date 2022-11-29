package com.anomot.anomotbackend

import org.springframework.security.test.context.support.WithSecurityContext


@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = MockSecurityContextFactory::class)
annotation class WithMockCustomUser(val username: String = "default",
                                    val email: String = "example@example.com",
                                    val password: String = "password",
                                    val authorities: Array<String> = ["ROLE_USER"],
                                    val isEmailVerified: Boolean = false,
                                    val isMfaActive: Boolean = false,
                                    val mfaMethods: Array<String> = [],
                                    val avatar: String = "00000000-0000-0000-0000-000000000000",
                                    val id: Long = 5)