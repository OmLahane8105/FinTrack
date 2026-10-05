package com.fintrack.service;

import com.fintrack.dto.UserRegisterRequest;
import com.fintrack.dto.UserResponse;
import com.fintrack.entity.User;
import com.fintrack.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(UserRegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException(
                    "Email already registered"
            );
        }

        String hashedPassword =
                passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getName(),
                request.getEmail(),
                hashedPassword
        );

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> {
                            throw new IllegalArgumentException(
                                    "User not found"
                            );
                        }
                );

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}