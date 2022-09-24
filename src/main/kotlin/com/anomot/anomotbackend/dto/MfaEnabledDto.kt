package com.anomot.anomotbackend.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class MfaEnabledDto(
        @get:JsonProperty("isMfaEnabled") // This annotation is needed for Jackson to properly map the json value
        var isMfaEnabled: Boolean
)