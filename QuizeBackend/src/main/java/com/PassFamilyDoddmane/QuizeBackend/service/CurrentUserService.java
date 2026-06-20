package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.exception.UnauthorizedException;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.repository.UserRepository;
import com.PassFamilyDoddmane.QuizeBackend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("No authenticated user");
        }
        UUID userId = principal.getUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }
}
