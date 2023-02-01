package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.AnomotBackendApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.util.Locale


@Service
class EmailService @Autowired constructor(
    private val mailSender: JavaMailSender,
    private val emailTemplateEngine: TemplateEngine,
    @Value("\${logo.url}")
    private val logoUrl: String,
    @Value("\${contact.mail}")
    private val contactMail: String,
    private val emailMessageSource: ResourceBundleMessageSource,
    @Value("\${environment.is-local}")
    private val isLocal: String?
) {

    private val logger: Logger = LoggerFactory.getLogger(AnomotBackendApplication::class.java)

    private fun processEmail(template: String, context: Context): String {
        return emailTemplateEngine.process(template, context)
    }

    fun sendEmailVerificationEmail(recipientEmail: String, link: String, locale: Locale) {
        val context = Context(locale)
        context.setVariable("imageSrc", logoUrl)
        context.setVariable("title", "title.email.verification")
        context.setVariable("mailToContactEmail", contactMail)
        context.setVariable("link", link)
        context.setVariable("message", "email.verification")

        val text = processEmail("html/email_link.html", context)

        sendEmail(recipientEmail,
                emailMessageSource.getMessage("title.email.verification", null, locale),
                text)
    }


    fun sendPasswordResetEmail(recipientEmail: String, link: String, locale: Locale) {
        val context = Context(locale)
        context.setVariable("imageSrc", logoUrl)
        context.setVariable("title", "title.password.reset")
        context.setVariable("mailToContactEmail", contactMail)
        context.setVariable("link", link)
        context.setVariable("message", "password.reset")

        val text = processEmail("html/email_link.html", context)

        sendEmail(recipientEmail,
                emailMessageSource.getMessage("title.password.reset", null, locale),
                text)

    }

    fun sendPasswordChangeEmail(recipientEmail: String, locale: Locale) {
        val context = Context(locale)
        context.setVariable("imageSrc", logoUrl)
        context.setVariable("title", "title.password.change")
        context.setVariable("mailToContactEmail", contactMail)

        val text = processEmail("html/email_password_change.html", context)

        sendEmail(recipientEmail,
                emailMessageSource.getMessage("title.password.change", null, locale),
                text)

    }

    fun sendMfaEmail(recipientEmail: String, code: String, locale: Locale) {
        val context = Context(locale)
        context.setVariable("imageSrc", logoUrl)
        context.setVariable("title", "title.mfa")
        context.setVariable("mailToContactEmail", contactMail)
        context.setVariable("code", code)

        val text = processEmail("html/email_mfa.html", context)

        sendEmail(recipientEmail,
                emailMessageSource.getMessage("title.mfa", null, locale),
                text)

    }

    private fun sendEmail(recipientEmail: String, subject: String, text: String) {
        val mimeMessage = mailSender.createMimeMessage()
        val message = MimeMessageHelper(mimeMessage, "UTF-8")
        message.setFrom("anomotapp@gmail.com")
        message.setTo(recipientEmail)
        message.setSubject(subject)
        message.setText(text, true)

        try {
            if (isLocal != null && isLocal.toBoolean()) {
                logger.info("Email $text")
                return
            }
            mailSender.send(mimeMessage)
        } catch (e: Exception) {
            logger.debug(e.message)
        }
    }


}