package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class MfaDto(
        @Size(min = Constants.MFA_PASSWORD_LENGTH, max = Constants.MFA_PASSWORD_LENGTH)
        val code: String?,
        @Pattern(regexp = "totp|email", flags = [Pattern.Flag.CASE_INSENSITIVE])
        val method: String?,
        @Size(min = Constants.MFA_RECOVERY_CODE_LENGTH, max = Constants.MFA_RECOVERY_CODE_LENGTH)
        val recoveryCode: String?)