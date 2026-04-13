package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    /**
     * Used to validate uniqueness of usernames on create.
     */
    boolean existsByUsername(String username);

    /**
     * Search users by username only (matches the UI label "Buscar por usuario").
     */
    List<AppUser> findByUsernameContainingIgnoreCaseOrderByIdAsc(String q);
}
