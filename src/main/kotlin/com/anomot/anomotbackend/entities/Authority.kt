package com.anomot.anomotbackend.entities

import java.io.Serializable
import javax.persistence.*

@Entity
@Table(name="authorities")
class Authority(
        @Column(unique = true)
        var authority: String,
        @ManyToMany(mappedBy = "authorities")
        var users: MutableList<User>? = null,
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null): Serializable