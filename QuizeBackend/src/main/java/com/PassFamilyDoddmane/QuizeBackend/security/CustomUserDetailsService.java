package com.PassFamilyDoddmane.QuizeBackend.security;

import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.repository.UserRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public UserPrincipal loadUserByUsername(String username) {
        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<String> roles = userRoleRepository.findRoleCodesByUserId(user.getId()).stream()
                .map(roleCode -> "ROLE_" + roleCode.name())
                .toList();
        return UserPrincipal.from(user, roles);
    }
}
