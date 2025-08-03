package com.sandro.realtime.user.domain.model

import com.sandro.realtime.shared.domain.BaseAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false, unique = true)
    var email: String,
) : BaseAuditEntity() {

    fun update(name: String) {
        this.name = name
    }
}
