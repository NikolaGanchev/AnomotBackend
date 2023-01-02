package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.dto.UserDto
import java.io.Serializable
import java.util.*
import javax.persistence.*


@Entity
@Table(name="users")
class User(
    @Column(unique = true)
    var email: String,
    var password: String,
    var username: String,
    @ManyToMany(cascade = [
        CascadeType.PERSIST,
        CascadeType.MERGE
    ])
    @JoinTable(name = "user_authorities",
            joinColumns = [JoinColumn(name = "user_id")],
            inverseJoinColumns = [JoinColumn(name = "authority_id")]
    )
    var authorities: MutableList<Authority>,
    var isEmailVerified: Boolean = false,
    var isMfaActive: Boolean = false,
    @ManyToMany(cascade = [
        CascadeType.PERSIST,
        CascadeType.MERGE
    ])
    @JoinTable(name = "user_2fa_methods",
            joinColumns = [JoinColumn(name = "user_id")],
            inverseJoinColumns = [JoinColumn(name = "mfa_method_id")]
    )
    var mfaMethods: MutableList<MfaMethod>? = null,
    var creationDate: Date = Date(),
    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn
    var avatar: Media? = null,
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null) : Serializable {
        fun getAsDto(): UserDto {
            return UserDto(
                    email = email,
                    username = username,
                    avatarId = avatar?.name?.toString(),
                    id = id ?: throw IllegalStateException("Id not available")
            )
        }
    }