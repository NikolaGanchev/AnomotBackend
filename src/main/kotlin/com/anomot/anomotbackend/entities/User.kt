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
    @ManyToMany(cascade = [
        CascadeType.PERSIST,
        CascadeType.MERGE
    ])
    @JoinTable(name = "user_authorities",
            joinColumns = [JoinColumn(name = "user_id")],
            inverseJoinColumns = [JoinColumn(name = "authority_id")]
    )
    var authorities: MutableList<Authority>,
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null) : Serializable