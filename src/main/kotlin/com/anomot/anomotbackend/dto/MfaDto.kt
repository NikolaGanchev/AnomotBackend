package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

class MfaDto(
        @Size(min = Constants.MFA_PASSWORD_LENGTH, max = Constants.MFA_PASSWORD_LENGTH)
        code: String?,
        @Pattern(regexp = "totp|email", flags = [Pattern.Flag.CASE_INSENSITIVE])
        method: String?,
        @Size(min = Constants.MFA_RECOVERY_CODE_LENGTH, max = Constants.MFA_RECOVERY_CODE_LENGTH)
        recoveryCode: String?)