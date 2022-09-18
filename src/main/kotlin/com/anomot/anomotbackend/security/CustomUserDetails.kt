package com.anomot.anomotbackend.security

import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.entities.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserDetails(private val user: User): UserDetails {

    val id = user.id

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return user.authorities.map { it -> SimpleGrantedAuthority(it.authority) }.toCollection(mutableListOf())
    }

    override fun getPassword(): String {
       return user.password
    }

    override fun getUsername(): String {
        return user.email
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
        return user.isEmailVerified
    }

    fun getAsDto(): UserDto {
        return UserDto(email = user.email,
                username = user.username,
                roles = getAuthoritiesAsList(),
                isMfaActive = user.isMfaActive,
                if (user.isMfaActive) getMfaMethodsAsList() else null)
    }

    private fun getAuthoritiesAsList(): List<String> {
        return user.authorities.map { it -> it.authority }.toCollection(mutableListOf())
    }

    fun getMfaMethodsAsList(): List<String>? {
        return user.mfaMethods?.map { it.method }?.toCollection(mutableListOf())
    }

    fun isEmailVerified(): Boolean {
        return user.isEmailVerified
    }

    fun isMfaEnabled(): Boolean {
        return user.isMfaActive
    }
}