package com.anomot.anomotbackend.entities

import java.io.Serializable
import jakarta.persistence.*

@Entity
@Table(name="mfa_methods")
class MfaMethod(@Column(unique = true)
                var method: String,
                @ManyToMany(mappedBy = "mfaMethods")
                var users: MutableList<User>? = null,
                @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null): Serializable