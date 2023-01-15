package com.anomot.anomotbackend

import com.anomot.anomotbackend.security.password.PasswordConstraintValidator
import org.junit.jupiter.api.Test
import java.security.SecureRandom

class PasswordTests {

    private val passwordConstraintValidator: PasswordConstraintValidator = PasswordConstraintValidator()

    @Test
    fun `When create password refuse if doesn't contain number`() {
        val passwordToTest = "securepassword$"

        assert(!passwordConstraintValidator.isValid(passwordToTest, null))
    }

    @Test
    fun `When create password refuse if below 10 characters`() {
        val passwordToTest = "password1"

        assert(!passwordConstraintValidator.isValid(passwordToTest, null))
    }

    @Test
    fun `When create password refuse if doesn't contain special symbol`() {
        val passwordToTest = "password123"

        assert(!passwordConstraintValidator.isValid(passwordToTest, null))
    }

    @Test
    fun `When create password refuse if over 512 character`() {
        val letters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val random = SecureRandom()
        val length = 513
        val passwordToTest = StringBuilder(length)

        for (i in 1..length) {
            passwordToTest.append(letters[random.nextInt(letters.length)])
        }

        assert(!passwordConstraintValidator.isValid(passwordToTest.toString(), null))
    }

    @Test
    fun `When create valid password pass`() {
        val passwordToTest = "password123$"

        assert(passwordConstraintValidator.isValid(passwordToTest, null))
    }
}