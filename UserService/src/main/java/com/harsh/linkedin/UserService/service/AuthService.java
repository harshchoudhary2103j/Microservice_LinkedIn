package com.harsh.linkedin.UserService.service;


import com.harsh.linkedin.UserService.dto.LoginRequestDto;
import com.harsh.linkedin.UserService.dto.SignupRequestDto;
import com.harsh.linkedin.UserService.dto.UserDto;
import com.harsh.linkedin.UserService.entity.BlacklistedToken;
import com.harsh.linkedin.UserService.entity.User;
import com.harsh.linkedin.UserService.event.UserCreatedEvent;
import com.harsh.linkedin.UserService.exception.BadRequestException;
import com.harsh.linkedin.UserService.exception.ResourceNotFoundException;
import com.harsh.linkedin.UserService.repository.BlacklistedTokenRepository;
import com.harsh.linkedin.UserService.repository.UserRepository;
import com.harsh.linkedin.UserService.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final JwtService jwtService;
    private final KafkaTemplate<Long, UserCreatedEvent> kafkaTemplate;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    public UserDto signUp(SignupRequestDto signupRequestDto) {
        log.info("Starting signup process for email: {}", signupRequestDto.getEmail());

        boolean exists = userRepository.existsByEmail(signupRequestDto.getEmail());
        if(exists) {
            log.warn("Signup failed: User with email {} already exists", signupRequestDto.getEmail());
            throw new BadRequestException("User already exists, cannot signup again.");
        }

        User user = modelMapper.map(signupRequestDto, User.class);

        // Hashing can be CPU intensive, good to log if it takes too long
        user.setPassword(PasswordUtil.hashPassword(signupRequestDto.getPassword()));

        User savedUser = userRepository.save(user);
        log.info("User saved to database with ID: {}", savedUser.getId());

        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .build();

        try {
            kafkaTemplate.send("user-created-topic", savedUser.getId(), event);
            log.info("Successfully published UserCreatedEvent to Kafka for userId: {}", savedUser.getId());
        } catch (Exception e) {
            // Critical: We log this as ERROR because the user is saved but notifications might fail
            log.error("Failed to send UserCreatedEvent to Kafka for userId: {}. Error: {}",
                    savedUser.getId(), e.getMessage());
        }

        return modelMapper.map(savedUser, UserDto.class);
    }

    public String login(LoginRequestDto loginRequestDto) {
        log.info("Attempting authentication for email: {}", loginRequestDto.getEmail());

        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: No user found with email: {}", loginRequestDto.getEmail());
                    return new ResourceNotFoundException("User not found with email: " + loginRequestDto.getEmail());
                });

        boolean isPasswordMatch = PasswordUtil.checkPassword(loginRequestDto.getPassword(), user.getPassword());

        if(!isPasswordMatch) {
            log.warn("Login failed: Incorrect password for email: {}", loginRequestDto.getEmail());
            throw new BadRequestException("Incorrect password");
        }

        String token = jwtService.generateAccessToken(user);
        log.info("Authentication successful. Generated JWT for userId: {}", user.getId());

        return token;
    }

    public void logout(String tokenHeader) {
        log.info("Processing logout request...");

        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            log.error("Logout failed: Invalid or missing Authorization header");
            throw new BadRequestException("Invalid token header");
        }

        String token = tokenHeader.split("Bearer ")[1];

        BlacklistedToken blacklistedToken = new BlacklistedToken();
        blacklistedToken.setToken(token);

        blacklistedTokenRepository.save(blacklistedToken);

        log.info("Token successfully blacklisted. User is now logged out.");
    }

    public boolean isTokenBlacklisted(String token) {
        boolean blacklisted = blacklistedTokenRepository.existsByToken(token);
        if (blacklisted) {
            log.debug("Validation check: Token is blacklisted");
        }
        return blacklisted;
    }

}