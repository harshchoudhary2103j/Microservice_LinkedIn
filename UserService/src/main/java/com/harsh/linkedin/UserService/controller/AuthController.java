package com.harsh.linkedin.UserService.controller;


import com.harsh.linkedin.UserService.dto.LoginRequestDto;
import com.harsh.linkedin.UserService.dto.SignupRequestDto;
import com.harsh.linkedin.UserService.dto.UserDto;
import com.harsh.linkedin.UserService.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signUp(@Valid @RequestBody SignupRequestDto signupRequestDto) {
        UserDto userDto = authService.signUp(signupRequestDto);
        return new ResponseEntity<>(userDto, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        String token = authService.login(loginRequestDto);
        return ResponseEntity.ok(token);
    }
    @GetMapping("/validate-token")
    public ResponseEntity<Boolean> isTokenBlacklisted(@RequestParam String token) {
        boolean isBlacklisted = authService.isTokenBlacklisted(token);
        return ResponseEntity.ok(isBlacklisted);
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {

        String tokenHeader = request.getHeader("Authorization");

        log.info("Logout request received for processing");


        authService.logout(tokenHeader);

        log.info("Logout successful, token invalidated");


        return ResponseEntity.noContent().build();
    }
}
