package com.anomot.anomotbackend.entities

import java.io.Serializable
import javax.persistence.*


@Entity
@Table(name="users")
class User(
    @Column(unique = true)
    var email: String,
    var password: String,
    var username: String,
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null) : Serializable