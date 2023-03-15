package com.anomot.anomotbackend.security.text

import com.anomot.anomotbackend.utils.TextUtils
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class TextPostSizeValidator: ConstraintValidator<TextPostSize, String> {
    private var min: Int? = null
    private var max: Int? = null
    private var absoluteMin: Int? = null
    private var absoluteMax: Int? = null

    override fun initialize(textPostSize: TextPostSize) {
        min = textPostSize.min
        max = textPostSize.max
        absoluteMin = textPostSize.absoluteMin
        absoluteMax = textPostSize.absoluteMax
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null || min == null || max == null || absoluteMin == null || absoluteMax == null) return false

        if (value.length < absoluteMin!! || value.length > absoluteMax!!) return false

        if (value.length < min!! || value.length > max!!) return false

        val strippedLength = TextUtils.getTextLengthFromHtml(value)

        if (strippedLength < min!! || strippedLength > max!!) return false

        return true
    }
}