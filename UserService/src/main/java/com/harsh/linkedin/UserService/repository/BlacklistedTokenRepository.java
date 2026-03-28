package com.harsh.linkedin.UserService.repository;

import com.harsh.linkedin.UserService.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    boolean existsByToken(String token);
}
