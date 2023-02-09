package com.anomot.anomotbackend.security

import com.anomot.anomotbackend.dto.SelfUserDto
import com.anomot.anomotbackend.entities.Ban
import com.anomot.anomotbackend.entities.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

open class CustomUserDetails(user: User, ban: Ban? = null): UserDetails {
    // The naming of the variables needs to not conflict with the abstract methods
    val id: Long? = user.id
    private val _authorities: MutableCollection<out GrantedAuthority> = user.authorities.map { SimpleGrantedAuthority(it.authority) }.toCollection(mutableListOf())
    private val _password = user.password
    private val _username = user.username
    private val _email = user.email
    private val _mfaMethods: List<String>? = user.mfaMethods?.map { it.method }?.toCollection(mutableListOf())
    private var avatarId: String? = if (user.avatar == null) null else user.avatar!!.name.toString()
    val isEmailVerified = user.isEmailVerified
    private val isMfaActive = user.isMfaActive
    val isBanned = ban != null
    val bannedUntil: Date? = ban?.until

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return _authorities
    }

    override fun getPassword(): String {
       return _password
    }

    override fun getUsername(): String {
        return _email
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }

    fun getAsSelfDto(): SelfUserDto {
        return SelfUserDto(email = _email,
                username = _username,
                isEmailVerified = isEmailVerified,
                roles = getAuthoritiesAsList(),
                isMfaActive = isMfaActive,
                if (isMfaActive) _mfaMethods else null,
                avatarId,
                id.toString())
    }

    private fun getAuthoritiesAsList(): List<String> {
        return _authorities.map { it.authority }.toCollection(mutableListOf())
    }

    fun hasMfaMethod(mfaMethodValue: MfaMethodValue): Boolean {
        return _mfaMethods != null && _mfaMethods.contains(mfaMethodValue.method)
    }

    fun isMfaEnabled(): Boolean {
        return isMfaActive
    }

    fun getMfaMethodsAsList(): List<String>? {
        return _mfaMethods
    }

    fun clearAvatar() {
        avatarId = null
    }
}