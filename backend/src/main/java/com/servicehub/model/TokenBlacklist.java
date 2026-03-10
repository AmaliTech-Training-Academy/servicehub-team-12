package com.servicehub.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores blacklisted JWT access tokens so they are rejected even before
 * their natural expiry (e.g. after explicit logout).
 */
@Entity
@Table(name = "token_blacklist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The full JWT string that has been invalidated. */
    @Column(nullable = false, unique = true, length = 1024)
    private String token;

    /** When the JWT would have naturally expired — used for scheduled cleanup. */
    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant blacklistedAt;

    @PrePersist
    protected void onBlacklist() {
        blacklistedAt = Instant.now();
    }
}
