package com.anomot.anomotbackend.security.text

import com.anomot.anomotbackend.utils.TextUtils
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class UrlValidator: ConstraintValidator<ValidUrl, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return false

        return TextUtils.isUrl(value)
    }
}