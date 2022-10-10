package com.anomot.anomotbackend.dto

class MfaStatusDto(
        val isEnabled: Boolean,
        val mfaMethods: List<String>?
)