package com.anomot.anomotbackend.security.password

import com.anomot.anomotbackend.utils.Constants
import org.passay.*
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class PasswordConstraintValidator: ConstraintValidator<ValidPassword, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        val passwordValidator = PasswordValidator(listOf(
                LengthRule(Constants.PASSWORD_MIN_SIZE, Constants.PASSWORD_MAX_SIZE),
                CharacterRule(EnglishCharacterData.Digit, 1),
                CharacterRule(EnglishCharacterData.Special, 1)
        ))

        if (value == null) return false

        val result = passwordValidator.validate(PasswordData(value))

        return result.isValid
    }
}