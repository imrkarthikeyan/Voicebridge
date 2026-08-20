package com.voicebridge.security;

import com.voicebridge.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizerDetailsService implements UserDetailsService {

    private final OrganizerRepository organizerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return organizerRepository.findByEmail(email)
                .map(OrganizerPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No organizer found with email: " + email));
    }
}
