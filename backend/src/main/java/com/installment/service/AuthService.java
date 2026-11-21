package com.installment.service;

import com.installment.dto.request.LoginRequest;
import com.installment.dto.request.RegisterRequest;
import com.installment.dto.response.UserResponse;
import com.installment.entity.User;
import com.installment.exception.AppException;
import com.installment.repository.UserRepository;
import com.installment.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int MAX_FAILS = 10;
    private static final long WINDOW_MS = 15L * 60 * 1000; // 15 minutes

    // key = ip + ":" + email, value = [count, firstFailTimestamp]
    private final ConcurrentHashMap<String, long[]> loginAttempts = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, String> register(RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new AppException(HttpStatus.CONFLICT, "Email already registered", "email");
        }

        User user = new User();
        user.setName(req.getName().trim());
        user.setEmail(req.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole("user");
        userRepository.save(user);

        return Map.of("message", "Registration successful");
    }

    public Map<String, Object> login(LoginRequest req, String clientIp, HttpServletResponse response) {
        String rateLimitKey = clientIp + ":" + req.getEmail().toLowerCase();
        checkRateLimit(rateLimitKey);

        User user = userRepository.findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> {
                    recordFailedAttempt(rateLimitKey);
                    return new AppException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
                });

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            recordFailedAttempt(rateLimitKey);
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // Clear rate limit on success
        loginAttempts.remove(rateLimitKey);

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole());
        setTokenCookie(response, token);

        return Map.of("user", new UserResponse(user));
    }

    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("isg_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public Map<String, Object> getSession(String token) {
        if (token == null || !jwtUtil.validateToken(token)) {
            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("user", null);
            return resp;
        }
        try {
            Long userId = jwtUtil.getUserId(token);
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                Map<String, Object> resp = new java.util.HashMap<>();
                resp.put("user", null);
                return resp;
            }
            return Map.of("user", new UserResponse(user));
        } catch (Exception e) {
            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("user", null);
            return resp;
        }
    }

    public Map<String, Object> getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        return Map.of("user", new UserResponse(user));
    }

    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("isg_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(8 * 3600); // 8 hours
        // SameSite=Lax via header (Servlet API doesn't support SameSite directly pre-6.1)
        response.addCookie(cookie);
        // Override Set-Cookie header to add SameSite=Lax
        String cookieHeader = response.getHeader("Set-Cookie");
        if (cookieHeader != null && !cookieHeader.contains("SameSite")) {
            response.setHeader("Set-Cookie", cookieHeader + "; SameSite=Lax");
        }
    }

    private void checkRateLimit(String key) {
        long[] entry = loginAttempts.get(key);
        if (entry == null) return;
        long count = entry[0];
        long firstFail = entry[1];
        long now = System.currentTimeMillis();
        if (now - firstFail > WINDOW_MS) {
            loginAttempts.remove(key);
            return;
        }
        if (count >= MAX_FAILS) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Please try again after 15 minutes.");
        }
    }

    private void recordFailedAttempt(String key) {
        loginAttempts.compute(key, (k, entry) -> {
            long now = System.currentTimeMillis();
            if (entry == null || now - entry[1] > WINDOW_MS) {
                return new long[]{1, now};
            }
            entry[0]++;
            return entry;
        });
    }
}

