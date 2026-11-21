package com.installment.controller;

import com.installment.dto.request.LoanRequest;
import com.installment.security.JwtAuthFilter;
import com.installment.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping({ "", "/" })
    public ResponseEntity<?> createLoan(@Valid @RequestBody LoanRequest req) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = loanService.createLoan(req, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping({ "", "/" })
    public ResponseEntity<?> getUserLoans() {
        Long userId = getCurrentUserId();
        Map<String, Object> result = loanService.getUserLoans(userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLoanById(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = loanService.getLoanById(id, userId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLoan(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        Map<String, String> result = loanService.deleteLoan(id, userId);
        return ResponseEntity.ok(result);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        JwtAuthFilter.AuthenticatedUser principal = (JwtAuthFilter.AuthenticatedUser) auth.getPrincipal();
        return principal.getUserId();
    }
}
