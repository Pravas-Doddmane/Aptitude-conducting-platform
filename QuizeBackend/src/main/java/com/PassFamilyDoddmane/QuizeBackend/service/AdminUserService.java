package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.AuditAction;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.UserStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.RoleName;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.BadRequestException;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.dto.user.AdminUserResponse;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public User updateStatus(UUID userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (isAdministrator(user)) {
            throw new BadRequestException("Administrator account cannot be modified");
        }
        UserStatus before = user.getStatus();
        user.setStatus(status);
        User saved = userRepository.save(user);
        auditService.logAction(
                AuditAction.UPDATE,
                "User",
                saved.getId().toString(),
                Map.of("status", before.name()),
                Map.of("status", status.name())
        );
        return saved;
    }

    private boolean isAdministrator(User user) {
        return user.getRoles().stream()
                .map(userRole -> userRole.getRole())
                .anyMatch(role -> role != null && role.getCode() == RoleName.ADMIN);
    }

    public User blockUser(UUID userId) {
        return updateStatus(userId, UserStatus.BLOCKED);
    }

    public User unblockUser(UUID userId) {
        return updateStatus(userId, UserStatus.ACTIVE);
    }

    public List<AdminUserResponse> findAll() {
        return userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    private AdminUserResponse toResponse(User user) {
        String role = user.getRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .sorted()
                .findFirst()
                .orElse(null);
        boolean active = user.getStatus() == UserStatus.ACTIVE;
        String firstName = user.getProfile() == null ? null : user.getProfile().getFirstName();
        String lastName = user.getProfile() == null ? null : user.getProfile().getLastName();
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                firstName,
                lastName,
                role,
                active,
                user.getStatus().name()
        );
    }
}
