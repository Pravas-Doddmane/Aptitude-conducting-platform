package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.dto.user.UserStatusUpdateRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.user.AdminUserResponse;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> all() {
        return ResponseEntity.ok(adminUserService.findAll());
    }

    @PatchMapping("/status")
    public ResponseEntity<?> updateStatus(@Valid @RequestBody UserStatusUpdateRequest request) {
        User updated = adminUserService.updateStatus(request.userId(), request.status());
        return ResponseEntity.ok(
                java.util.Map.of(
                        "userId", updated.getId(),
                        "email", updated.getEmail(),
                        "status", updated.getStatus().name()
                )
        );
    }

    @PostMapping("/{userId}/block")
    public ResponseEntity<?> block(@PathVariable java.util.UUID userId) {
        User updated = adminUserService.blockUser(userId);
        return ResponseEntity.ok(java.util.Map.of(
                "userId", updated.getId(),
                "email", updated.getEmail(),
                "status", updated.getStatus().name()
        ));
    }

    @PostMapping("/{userId}/unblock")
    public ResponseEntity<?> unblock(@PathVariable java.util.UUID userId) {
        User updated = adminUserService.unblockUser(userId);
        return ResponseEntity.ok(java.util.Map.of(
                "userId", updated.getId(),
                "email", updated.getEmail(),
                "status", updated.getStatus().name()
        ));
    }
}
