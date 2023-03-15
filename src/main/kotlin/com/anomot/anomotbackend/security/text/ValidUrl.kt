package com.anomot.anomotbackend.security.text

import jakarta.validation.Constraint
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [UrlValidator::class])
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValidUrl(val message: String = "Invalid URL",
                              val groups: Array<KClass<*>> = [],
                              val payload: Array<KClass<out Any>> = [])
