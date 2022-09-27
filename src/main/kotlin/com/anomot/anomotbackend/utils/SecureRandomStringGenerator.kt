package com.anomot.anomotbackend.utils

import java.security.SecureRandom

class SecureRandomStringGenerator(private val alphabet: String) {
    companion object {
        const val NUMERIC = "0123456789"
        const val ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    }

    private val random = SecureRandom()

    fun generate(length: Int): String {
        val code = StringBuilder(length)

        for (i in 1..length) {
            code.append(alphabet[random.nextInt(alphabet.length)])
        }

        return code.toString()
    }
}