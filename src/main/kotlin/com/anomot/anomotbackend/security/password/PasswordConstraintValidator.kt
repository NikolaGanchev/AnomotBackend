package com.anomot.anomotbackend.security.password

import org.passay.*
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class PasswordConstraintValidator: ConstraintValidator<ValidPassword, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        val passwordValidator = PasswordValidator(listOf(
                LengthRule(10, 512),
                CharacterRule(EnglishCharacterData.Digit, 1),
                CharacterRule(EnglishCharacterData.Special, 1)
        ))

        if (value == null) return false

        val result = passwordValidator.validate(PasswordData(value))

        return result.isValid
    }
}