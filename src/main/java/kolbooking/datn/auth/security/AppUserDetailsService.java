package kolbooking.datn.auth.security;

import kolbooking.datn.auth.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return repository.findByEmail(email)
                .map(AppUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    public UserDetails loadUserById(Long id) {
        return repository.findById(id)
                .map(AppUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: id=" + id));
    }
}
