package com.servicehub.repository;

import com.servicehub.model.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Provides persistence operations for blacklisted JWT tokens.
 */

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    boolean existsByToken(String token);

}

