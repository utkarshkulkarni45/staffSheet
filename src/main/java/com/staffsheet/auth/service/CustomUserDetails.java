package com.staffsheet.auth.service;

import com.staffsheet.employee.entity.Employee;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record CustomUserDetails(
        Long id,
        String email,
        String password,
        String role,
        boolean active,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {

    public static CustomUserDetails fromEntity(Employee employee) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + employee.getRole().name())
        );

        return new CustomUserDetails(
                employee.getId(),
                employee.getEmail(),
                employee.getPasswordHash(),
                employee.getRole().name(),
                employee.isActive(),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}

