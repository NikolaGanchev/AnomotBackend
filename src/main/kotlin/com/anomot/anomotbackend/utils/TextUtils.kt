package com.anomot.anomotbackend.utils

import org.apache.commons.validator.routines.UrlValidator
import org.owasp.html.HtmlPolicyBuilder

class TextUtils {
    companion object {
        private val schemes = arrayOf("http", "https")
        /**
        Extracts only the text out of non-sanitised HTML, removing all tags
         */
        fun getTextFromHtml(text: String): String {
            val policy = HtmlPolicyBuilder().toFactory()
            return policy.sanitize(text)
        }

        fun getTextLengthFromHtml(text: String): Int {
            return getTextFromHtml(text).length
        }

        fun isUrl(text: String): Boolean {
            return UrlValidator(schemes).isValid(text)
        }
    }
}