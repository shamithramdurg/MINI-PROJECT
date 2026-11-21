package com.installment.service;

import com.installment.dto.response.LoanResponse;
import com.installment.dto.response.UserResponse;
import com.installment.entity.User;
import com.installment.exception.AppException;
import com.installment.repository.LoanRepository;
import com.installment.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final LoanRepository loanRepository;

    public AdminService(UserRepository userRepository, LoanRepository loanRepository) {
        this.userRepository = userRepository;
        this.loanRepository = loanRepository;
    }

    public Map<String, Object> getStats() {
        long userCount = userRepository.count();
        long loanCount = loanRepository.count();
        return Map.of("users", userCount, "loans", loanCount);
    }

    public Map<String, Object> getAllUsers() {
        Map<Long, Long> loanCounts = loanRepository.countLoansGroupedByUserId()
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        List<UserResponse> users = userRepository.findAll()
                .stream()
                .map(u -> new UserResponse(u, loanCounts.getOrDefault(u.getUserId(), 0L)))
                .collect(Collectors.toList());
        return Map.of("users", users);
    }

    @Transactional
    public Map<String, String> updateUserRole(Long targetUserId, String role, Long currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "You cannot change your own role.");
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found."));
        user.setRole(role);
        userRepository.save(user);
        return Map.of("message", "Role updated.");
    }

    @Transactional
    public Map<String, String> deleteUser(Long targetUserId, Long currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "You cannot delete your own account.");
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found."));
        userRepository.delete(user);
        return Map.of("message", "User and their schedules deleted.");
    }

    public Map<String, Object> getAllLoans() {
        List<LoanResponse> loans = loanRepository.findAllWithUser()
                .stream()
                .map(LoanResponse::new)
                .collect(Collectors.toList());
        return Map.of("loans", loans);
    }
}

