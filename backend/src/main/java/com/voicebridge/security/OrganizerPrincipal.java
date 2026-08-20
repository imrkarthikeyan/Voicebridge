package com.voicebridge.security;

import com.voicebridge.entity.Organizer;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class OrganizerPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final String name;

    public OrganizerPrincipal(Organizer organizer) {
        this.id = organizer.getId();
        this.email = organizer.getEmail();
        this.passwordHash = organizer.getPasswordHash();
        this.name = organizer.getName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_ORGANIZER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
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
}
