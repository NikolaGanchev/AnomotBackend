package com.anomot.anomotbackend.entities

import javax.persistence.*
import javax.validation.constraints.Email
import javax.validation.constraints.Size


@Entity
@Table(name="users")
class User(
    @Email(regexp ="[a-z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*", message = "Email is not valid")
    @Size(max=254, message = "Email is too long")
    @Column(unique = true)
    var email: String,
    var password: String,
    @Size(max = 40, message = "Username is too long") var username: String,
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null)