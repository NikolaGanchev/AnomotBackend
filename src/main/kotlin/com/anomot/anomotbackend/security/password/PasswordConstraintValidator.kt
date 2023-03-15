package com.anomot.anomotbackend.security.password

import com.anomot.anomotbackend.utils.Constants
import org.passay.*
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class PasswordConstraintValidator: ConstraintValidator<ValidPassword, String> {
    companion object {
        val passwordValidator = PasswordValidator(listOf(
                LengthRule(Constants.PASSWORD_MIN_SIZE, Constants.PASSWORD_MAX_SIZE),
                CharacterRule(EnglishCharacterData.Digit, 1),
                CharacterRule(EnglishCharacterData.Special, 1)
        ))
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return false

        val result = passwordValidator.validate(PasswordData(value))

        return result.isValid
    }
}