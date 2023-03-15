package com.anomot.anomotbackend.security.text

import jakarta.validation.Constraint
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [TextPostSizeValidator::class])
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TextPostSize(val message: String = "Post not in size limits",
                              val groups: Array<KClass<*>> = [],
                              val payload: Array<KClass<out Any>> = [],
                              val min: Int,
                              val max: Int,
                              val absoluteMin: Int,
                              val absoluteMax: Int)
