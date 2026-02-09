package com.example.bankcards.controller;

import com.example.bankcards.config.JwtProperties;
import com.example.bankcards.dto.AuthRequest;
import com.example.bankcards.dto.AuthResponse;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String token = jwtService.generateToken(principal);
        return new AuthResponse(token, jwtProperties.getExpirationMinutes());
    }
}
