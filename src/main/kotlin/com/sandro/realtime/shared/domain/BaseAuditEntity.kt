package com.sandro.realtime.shared.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseAuditEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long? = null,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    open var createdAt: LocalDateTime? = null,

    @LastModifiedDate
    @Column(nullable = false)
    open var updatedAt: LocalDateTime? = null,
)