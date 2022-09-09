package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.dto.UserRegisterDto
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.exceptions.UserAlreadyExistsException
import com.anomot.anomotbackend.repositories.UserRepository
import com.anomot.anomotbackend.security.CustomUserDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl: UserDetailsService {

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var passwordEncoder: Argon2PasswordEncoder

    override fun loadUserByUsername(email: String?): UserDetails {
        if (email == null) {
            throw NullPointerException("Email cannot be null")
        }
        val user = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("Email not found")
        return CustomUserDetails(user)
    }

    fun createUser(userRegisterDto: UserRegisterDto): UserDto {
        val emailExists = userExists(userRegisterDto)

        if (emailExists) {
            throw UserAlreadyExistsException("User already exists")
        }

        val hashedPassword = passwordEncoder.encode(userRegisterDto.password)

        val user = User(email = userRegisterDto.email, password = hashedPassword, username = userRegisterDto.username)

        val savedUser = userRepository.save(user)

        return UserDto(email = savedUser.email, username = savedUser.username)
    }

    private fun userExists(userRegisterDto: UserRegisterDto): Boolean {
        return userRepository.findByEmail(userRegisterDto.email) != null
    }
}


