package com.taskmanager.service;

import com.taskmanager.dto.AuthResponse;
import com.taskmanager.dto.LoginRequest;
import com.taskmanager.dto.RegisterRequest;
import com.taskmanager.entity.User;
import com.taskmanager.exception.AuthenticationException;
import com.taskmanager.exception.UserAlreadyExistsException;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        logger.info("Attempting to register user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            logger.warn("Registration failed - username already exists: {}", request.getUsername());
            throw new UserAlreadyExistsException("Username is already taken!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        String jwt = jwtUtil.generateJwtToken(user.getUsername());

        logger.info("User successfully registered: {}", user.getUsername());

        return new AuthResponse(jwt, user.getUsername(), "User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {
        logger.info("Attempting to authenticate user: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword())
            );

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String jwt = jwtUtil.generateJwtToken(userPrincipal.getUsername());

            logger.info("User successfully authenticated: {}", userPrincipal.getUsername());

            return new AuthResponse(jwt, userPrincipal.getUsername(), "Login successful");

        } catch (Exception e) {
            logger.error("Authentication failed for user: {}", request.getUsername());
            throw new AuthenticationException("Invalid username or password");
        }
    }
}