package com.anomot.anomotbackend.dto

import org.springframework.http.MediaType

data class MediaResponse(val file: ByteArray, val contentType: MediaType) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaResponse

        if (!file.contentEquals(other.file)) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = file.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}