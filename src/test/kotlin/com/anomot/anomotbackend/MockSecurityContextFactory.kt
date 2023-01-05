package com.anomot.anomotbackend

import com.anomot.anomotbackend.entities.Authority
import com.anomot.anomotbackend.entities.Media
import com.anomot.anomotbackend.entities.MfaMethod
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.utils.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory
import java.util.*

class MockSecurityContextFactory: WithSecurityContextFactory<WithMockCustomUser> {

    override fun createSecurityContext(customUser: WithMockCustomUser): SecurityContext {
        val context = SecurityContextHolder.createEmptyContext()

        val user = User(email = customUser.email,
                        password = customUser.password,
                        username = customUser.username,
                        authorities = customUser.authorities.map { Authority(it) }.toCollection(mutableListOf()),
                        isEmailVerified = customUser.isEmailVerified,
                        isMfaActive = customUser.isMfaActive,
                        mfaMethods = customUser.mfaMethods.map { MfaMethod(it) }.toCollection(mutableListOf()),
                        id = customUser.id,
                        avatar = Media(UUID.fromString(customUser.avatar),
                        null, null, MediaType.IMAGE, User("", "", "", mutableListOf())))

        val principal = CustomUserDetails(user)
        val auth: Authentication = UsernamePasswordAuthenticationToken(principal, user.password, principal.authorities)
        context.authentication = auth
        return context
    }
}