package com.installment.controller;

import com.installment.dto.request.RoleRequest;
import com.installment.security.JwtAuthFilter;
import com.installment.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping({"/stats", "/stats/"})
    public ResponseEntity<?> getStats() {
        Map<String, Object> result = adminService.getStats();
        return ResponseEntity.ok(result);
    }

    @GetMapping({"/users", "/users/"})
    public ResponseEntity<?> getAllUsers() {
        Map<String, Object> result = adminService.getAllUsers();
        return ResponseEntity.ok(result);
    }

    @PatchMapping({"/users/{id}/role", "/users/{id}/role/"})
    public ResponseEntity<?> updateUserRole(@PathVariable Long id,
                                            @Valid @RequestBody RoleRequest req) {
        Long currentUserId = getCurrentUserId();
        Map<String, String> result = adminService.updateUserRole(id, req.getRole(), currentUserId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping({"/users/{id}", "/users/{id}/"})
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        Map<String, String> result = adminService.deleteUser(id, currentUserId);
        return ResponseEntity.ok(result);
    }

    @GetMapping({"/loans", "/loans/"})
    public ResponseEntity<?> getAllLoans() {
        Map<String, Object> result = adminService.getAllLoans();
        return ResponseEntity.ok(result);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        JwtAuthFilter.AuthenticatedUser principal =
                (JwtAuthFilter.AuthenticatedUser) auth.getPrincipal();
        return principal.getUserId();
    }
}

