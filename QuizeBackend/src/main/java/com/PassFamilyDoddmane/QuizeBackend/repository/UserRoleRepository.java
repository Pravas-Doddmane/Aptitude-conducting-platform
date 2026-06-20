package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.RoleName;
import com.PassFamilyDoddmane.QuizeBackend.entity.UserRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUserId(UUID userId);
    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    @Query("select ur.role.code from UserRole ur where ur.user.id = :userId")
    List<RoleName> findRoleCodesByUserId(@Param("userId") UUID userId);
}
