package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.UserStatus;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByStatus(UserStatus status);
}
