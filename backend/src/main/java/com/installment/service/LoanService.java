package com.installment.service;

import com.installment.dto.request.LoanRequest;
import com.installment.dto.response.LoanResponse;
import com.installment.dto.response.LoanWithScheduleResponse;
import com.installment.dto.response.ScheduleEntryResponse;
import com.installment.entity.InstallmentSchedule;
import com.installment.entity.Loan;
import com.installment.entity.User;
import com.installment.exception.AppException;
import com.installment.repository.InstallmentScheduleRepository;
import com.installment.repository.LoanRepository;
import com.installment.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LoanService {

        private final LoanRepository loanRepository;
        private final InstallmentScheduleRepository scheduleRepository;
        private final UserRepository userRepository;
        private final InstallmentCalculator calculator;

        public LoanService(LoanRepository loanRepository,
                        InstallmentScheduleRepository scheduleRepository,
                        UserRepository userRepository,
                        InstallmentCalculator calculator) {
                this.loanRepository = loanRepository;
                this.scheduleRepository = scheduleRepository;
                this.userRepository = userRepository;
                this.calculator = calculator;
        }

        @Transactional
        public Map<String, Object> createLoan(LoanRequest req, Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

                LocalDate startDate;
                try {
                        startDate = LocalDate.parse(req.getStartDate());
                        if (startDate.getYear() < 2000 || startDate.getYear() > 2100) {
                                throw new AppException(HttpStatus.BAD_REQUEST,
                                                "Start date year must be between 2000 and 2100.", "startDate");
                        }
                } catch (DateTimeParseException e) {
                        throw new AppException(HttpStatus.BAD_REQUEST, "Start date must be a valid date.", "startDate");
                }

                InstallmentCalculator.CalcResult result = calculator.calculate(
                                req.getPrincipal(),
                                req.getInterestRate(),
                                req.getTenure(),
                                req.getFrequency(),
                                startDate);

                if (result.numInstallments <= 0) {
                        throw new AppException(HttpStatus.BAD_REQUEST,
                                        "Invalid tenure or frequency: results in 0 installments");
                }

                Loan loan = new Loan();
                loan.setUser(user);
                loan.setPrincipal(req.getPrincipal());
                loan.setInterestRate(req.getInterestRate());
                loan.setTenure(req.getTenure());
                loan.setFrequency(req.getFrequency());
                loan.setStartDate(startDate);
                loan.setNumInstallments(result.numInstallments);
                loan.setInstallmentAmount(result.installmentAmount);
                loan.setTotalPayment(result.totalPayment);
                loan.setTotalInterest(result.totalInterest);

                Loan savedLoan = loanRepository.save(loan);

                List<InstallmentSchedule> scheduleEntities = calculator.buildScheduleEntities(savedLoan, result.rows);
                scheduleRepository.saveAll(scheduleEntities);

                List<ScheduleEntryResponse> scheduleResponse = scheduleEntities.stream()
                                .map(ScheduleEntryResponse::new)
                                .collect(Collectors.toList());

                return Map.of(
                                "id", savedLoan.getLoanId(),
                                "loan", new LoanResponse(savedLoan),
                                "schedule", scheduleResponse);
        }

        @Transactional(readOnly = true)
        public Map<String, Object> getUserLoans(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

                List<LoanResponse> loans = loanRepository.findByUserOrderByCreatedAtDesc(user)
                                .stream()
                                .map(LoanResponse::new)
                                .collect(Collectors.toList());

                return Map.of("loans", loans);
        }

        @Transactional(readOnly = true)
        public Map<String, Object> getLoanById(Long loanId, Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

                Loan loan = loanRepository.findById(loanId)
                                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Schedule not found."));

                if (!"admin".equals(user.getRole()) && !loan.getUser().getUserId().equals(userId)) {
                        throw new AppException(HttpStatus.NOT_FOUND, "Schedule not found.");
                }

                List<ScheduleEntryResponse> schedule = scheduleRepository
                                .findByLoanOrderByInstallmentNoAsc(loan)
                                .stream()
                                .map(ScheduleEntryResponse::new)
                                .collect(Collectors.toList());

                return Map.of(
                                "loan", new LoanResponse(loan),
                                "schedule", schedule);
        }

        @Transactional
        public Map<String, String> deleteLoan(Long loanId, Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

                Loan loan = loanRepository.findById(loanId)
                                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Schedule not found."));

                if (!"admin".equals(user.getRole()) && !loan.getUser().getUserId().equals(userId)) {
                        throw new AppException(HttpStatus.NOT_FOUND, "Schedule not found.");
                }

                loanRepository.delete(loan);
                return Map.of("message", "Schedule deleted.");
        }
}
