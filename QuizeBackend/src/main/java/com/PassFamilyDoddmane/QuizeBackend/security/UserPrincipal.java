package com.PassFamilyDoddmane.QuizeBackend.security;

import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    private final User user;
    private final List<GrantedAuthority> authorities;

    public UUID getUserId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public static UserPrincipal from(User user, List<String> roleNames) {
        List<GrantedAuthority> authorities = roleNames.stream()
                .map(roleName -> (GrantedAuthority) new SimpleGrantedAuthority(roleName))
                .toList();
        return new UserPrincipal(user, authorities);
    }
}
