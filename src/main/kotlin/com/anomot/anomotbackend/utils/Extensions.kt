package com.anomot.anomotbackend.utils

import java.security.MessageDigest

/**
 * Checks if the given string is equal to this object in constant time
 * Simple wrapper around MessageDigest.isEqual
 * @param stringToCompare The string to compare against
 * @return true if the string is equal to this object, else false
 */
fun String.secureEquals(stringToCompare: String): Boolean {
    return MessageDigest.isEqual(this.toByteArray(), stringToCompare.toByteArray())
}