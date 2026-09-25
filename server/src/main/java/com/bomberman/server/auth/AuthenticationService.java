package com.bomberman.server.auth;

import com.bomberman.common.enums.AuthResultCode;
import com.bomberman.server.repository.UserRepository;
import com.bomberman.server.user.OnlineUserRegistry;
import com.bomberman.server.user.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Account registration, credential verification, and online-presence rules.
 */
@Service
public class AuthenticationService {

    private static final int MAX_USERNAME_LENGTH = 50;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OnlineUserRegistry onlineUserRegistry;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OnlineUserRegistry onlineUserRegistry
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.onlineUserRegistry = onlineUserRegistry;
    }

    @Transactional
    public RegistrationResult register(String rawUsername, String password) {
        String username = normalizeUsername(rawUsername);
        AuthResultCode validationResult = validateCredentials(username, password);
        if (validationResult != AuthResultCode.SUCCESS) {
            return new RegistrationResult(validationResult, null, null);
        }
        if (userRepository.existsByUsername(username)) {
            return new RegistrationResult(
                    AuthResultCode.USERNAME_ALREADY_EXISTS,
                    null,
                    username
            );
        }

        User savedUser = userRepository.saveAndFlush(
                new User(username, passwordEncoder.encode(password))
        );
        return new RegistrationResult(
                AuthResultCode.SUCCESS,
                savedUser.getId(),
                savedUser.getUsername()
        );
    }

    @Transactional(readOnly = true)
    public LoginResult login(String rawUsername, String password, String sessionId) {
        String username = normalizeUsername(rawUsername);
        AuthResultCode validationResult = validateCredentials(username, password);
        if (validationResult != AuthResultCode.SUCCESS) {
            return new LoginResult(validationResult, null);
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return new LoginResult(AuthResultCode.INVALID_CREDENTIALS, null);
        }
        if (!onlineUserRegistry.markOnline(user.getId(), user.getUsername(), sessionId)) {
            return new LoginResult(AuthResultCode.ACCOUNT_ALREADY_ONLINE, null);
        }

        return new LoginResult(
                AuthResultCode.SUCCESS,
                new AuthenticatedUser(user.getId(), user.getUsername())
        );
    }

    public void logout(AuthenticatedUser user, String sessionId) {
        Objects.requireNonNull(user, "user must not be null");
        onlineUserRegistry.markOffline(user.userId(), sessionId);
    }

    private AuthResultCode validateCredentials(String username, String password) {
        if (username == null || username.isBlank() || username.length() > MAX_USERNAME_LENGTH) {
            return AuthResultCode.INVALID_USERNAME;
        }
        if (password == null || password.isBlank()) {
            return AuthResultCode.INVALID_PASSWORD;
        }
        return AuthResultCode.SUCCESS;
    }

    private String normalizeUsername(String username) {
        return username == null ? null : username.trim();
    }
}
