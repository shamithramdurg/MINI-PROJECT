package com.installment.controller;

import com.installment.dto.request.LoginRequest;
import com.installment.dto.request.RegisterRequest;
import com.installment.security.JwtAuthFilter;
import com.installment.security.JwtUtil;
import com.installment.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping({"/register", "/register/"})
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        Map<String, String> result = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping({"/login", "/login/"})
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        String clientIp = getClientIp(request);
        Map<String, Object> result = authService.login(req, clientIp, response);
        return ResponseEntity.ok(result);
    }

    @PostMapping({"/logout", "/logout/"})
    public ResponseEntity<?> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping({"/session", "/session/"})
    public ResponseEntity<?> session(HttpServletRequest request) {
        String token = extractToken(request);
        Map<String, Object> result = authService.getSession(token);
        return ResponseEntity.ok(result);
    }

    @GetMapping({"/me", "/me/"})
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        JwtAuthFilter.AuthenticatedUser principal =
                (JwtAuthFilter.AuthenticatedUser) auth.getPrincipal();
        Map<String, Object> result = authService.getMe(principal.getUserId());
        return ResponseEntity.ok(result);
    }

    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if ("isg_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

