package com.asfaw.review_ai.service;

import com.asfaw.review_ai.config.BootstrapAdminProperties;
import com.asfaw.review_ai.model.entity.AppUser;
import com.asfaw.review_ai.model.enums.UserRole;
import com.asfaw.review_ai.repository.AppUserRepository;
import com.asfaw.review_ai.web.api.dto.UserCreateRequest;
import com.asfaw.review_ai.web.api.dto.UserResponse;
import com.asfaw.review_ai.web.api.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserAccountService implements UserDetailsService, ApplicationRunner {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties bootstrapAdmin;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser user = repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(!user.isEnabled())
                .build();
    }

    /** Creates the bootstrap admin from APP_ADMIN_USERNAME / APP_ADMIN_PASSWORD when no users exist yet. */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() == 0) {
            save(new AppUser(), bootstrapAdmin.username(), bootstrapAdmin.password(), UserRole.ADMIN, true);
            log.info("Created bootstrap admin user '{}'", bootstrapAdmin.username());
        }
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return repository.findAllByOrderByUsernameAsc().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (repository.existsByUsernameIgnoreCase(request.username().strip())) {
            throw new ResponseStatusException(CONFLICT, "Username already exists");
        }
        return UserResponse.from(save(new AppUser(), request.username().strip(), request.password(), request.role(), true));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        AppUser user = repository.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        boolean losesAdmin = user.getRole() == UserRole.ADMIN && user.isEnabled()
                && (request.role() != UserRole.ADMIN || !request.enabled());
        if (losesAdmin && repository.countByRoleAndEnabledTrue(UserRole.ADMIN) <= 1) {
            throw new ResponseStatusException(CONFLICT, "Cannot demote or disable the last active admin");
        }
        String password = request.password() == null || request.password().isBlank() ? null : request.password();
        return UserResponse.from(save(user, user.getUsername(), password, request.role(), request.enabled()));
    }

    private AppUser save(AppUser user, String username, String rawPassword, UserRole role, boolean enabled) {
        user.setUsername(username);
        if (rawPassword != null) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
        user.setRole(role);
        user.setEnabled(enabled);
        return repository.save(user);
    }
}
