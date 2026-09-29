package com.asfaw.review_ai.repository;

import com.asfaw.review_ai.model.entity.AppUser;
import com.asfaw.review_ai.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    long countByRoleAndEnabledTrue(UserRole role);

    List<AppUser> findAllByOrderByUsernameAsc();
}
