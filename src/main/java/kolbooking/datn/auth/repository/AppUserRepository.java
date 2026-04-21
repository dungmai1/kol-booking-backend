package kolbooking.datn.auth.repository;

import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);

    Page<AppUser> findByRole(Role role, Pageable pageable);
    Page<AppUser> findByEmailContainingIgnoreCase(String emailFragment, Pageable pageable);
    Page<AppUser> findByRoleAndEmailContainingIgnoreCase(Role role, String emailFragment, Pageable pageable);

    long countByRole(Role role);
}
