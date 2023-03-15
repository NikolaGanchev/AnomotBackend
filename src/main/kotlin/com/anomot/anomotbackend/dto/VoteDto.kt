package com.anomot.anomotbackend.dto

import jakarta.validation.constraints.Size

data class VoteDto(@Size(min = 1, max = 10000) val jwt: String, val forId: String)
