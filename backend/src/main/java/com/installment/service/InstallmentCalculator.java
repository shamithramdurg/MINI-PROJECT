package com.installment.service;

import com.installment.entity.InstallmentSchedule;
import com.installment.entity.Loan;
import com.installment.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class InstallmentCalculator {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    public static class CalcResult {
        public final int numInstallments;
        public final BigDecimal installmentAmount;
        public final BigDecimal totalPayment;
        public final BigDecimal totalInterest;
        public final List<ScheduleRow> rows;

        public CalcResult(int numInstallments, BigDecimal installmentAmount,
                          BigDecimal totalPayment, BigDecimal totalInterest,
                          List<ScheduleRow> rows) {
            this.numInstallments = numInstallments;
            this.installmentAmount = installmentAmount;
            this.totalPayment = totalPayment;
            this.totalInterest = totalInterest;
            this.rows = rows;
        }
    }

    public static class ScheduleRow {
        public final int installmentNo;
        public final LocalDate dueDate;
        public final BigDecimal installmentAmount;
        public final BigDecimal principalPaid;
        public final BigDecimal interestPaid;
        public final BigDecimal balance;

        public ScheduleRow(int installmentNo, LocalDate dueDate,
                           BigDecimal installmentAmount, BigDecimal principalPaid,
                           BigDecimal interestPaid, BigDecimal balance) {
            this.installmentNo = installmentNo;
            this.dueDate = dueDate;
            this.installmentAmount = installmentAmount;
            this.principalPaid = principalPaid;
            this.interestPaid = interestPaid;
            this.balance = balance;
        }
    }

    public CalcResult calculate(BigDecimal principal, BigDecimal annualRatePercent,
                                int tenure, String frequency, LocalDate startDate) {
        int periodsPerYear = getPeriodsPerYear(frequency);
        int numInstallments = computeNumInstallments(tenure, frequency);

        BigDecimal annualRate = annualRatePercent.divide(BigDecimal.valueOf(100), MC);
        BigDecimal r = annualRate.divide(BigDecimal.valueOf(periodsPerYear), MC);

        BigDecimal emi;
        if (r.compareTo(BigDecimal.ZERO) == 0) {
            // Zero interest: just divide principal equally
            emi = principal.divide(BigDecimal.valueOf(numInstallments), MC);
        } else {
            // EMI = P * R * (1+R)^N / ((1+R)^N - 1)
            BigDecimal onePlusR = BigDecimal.ONE.add(r, MC);
            BigDecimal onePlusRPowN = onePlusR.pow(numInstallments, MC);
            BigDecimal numerator = principal.multiply(r, MC).multiply(onePlusRPowN, MC);
            BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE, MC);
            emi = numerator.divide(denominator, MC);
        }

        // Round EMI to 2 decimal places
        BigDecimal emiRounded = emi.setScale(2, RoundingMode.HALF_UP);

        List<ScheduleRow> rows = new ArrayList<>();
        BigDecimal balance = principal;
        BigDecimal totalPaid = BigDecimal.ZERO;

        for (int i = 1; i <= numInstallments; i++) {
            LocalDate dueDate = computeDueDate(startDate, frequency, i);
            BigDecimal interestPaid = balance.multiply(r, MC).setScale(2, RoundingMode.HALF_UP);

            BigDecimal thisEmi;
            BigDecimal principalPaid;
            BigDecimal newBalance;

            if (i == numInstallments) {
                // Final installment: clear the remaining balance exactly
                principalPaid = balance;
                thisEmi = principalPaid.add(interestPaid);
                newBalance = BigDecimal.ZERO;
            } else {
                thisEmi = emiRounded;
                principalPaid = thisEmi.subtract(interestPaid);
                if (principalPaid.compareTo(BigDecimal.ZERO) < 0) {
                    principalPaid = BigDecimal.ZERO;
                }
                if (principalPaid.compareTo(balance) > 0) {
                    principalPaid = balance;
                }
                newBalance = balance.subtract(principalPaid).setScale(2, RoundingMode.HALF_UP);
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    newBalance = BigDecimal.ZERO;
                }
            }

            rows.add(new ScheduleRow(
                    i,
                    dueDate,
                    thisEmi.setScale(2, RoundingMode.HALF_UP),
                    principalPaid.setScale(2, RoundingMode.HALF_UP),
                    interestPaid,
                    newBalance
            ));

            totalPaid = totalPaid.add(thisEmi);
            balance = newBalance;
        }

        BigDecimal totalPayment = totalPaid.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalPayment.subtract(principal).setScale(2, RoundingMode.HALF_UP);

        return new CalcResult(numInstallments, emiRounded, totalPayment, totalInterest, rows);
    }

    private int getPeriodsPerYear(String frequency) {
        return switch (frequency.toLowerCase()) {
            case "weekly" -> 52;
            case "monthly" -> 12;
            case "quarterly" -> 4;
            case "yearly" -> 1;
            default -> throw new AppException(HttpStatus.BAD_REQUEST, "Please select a valid installment frequency.", "frequency");
        };
    }

    private int computeNumInstallments(int tenure, String frequency) {
        int n = switch (frequency.toLowerCase()) {
            case "weekly" -> (int) Math.round(tenure * 52.0 / 12.0);
            case "monthly" -> tenure;
            case "quarterly" -> {
                if (tenure % 3 != 0) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "For quarterly installments the tenure must be a multiple of 3 months.", "tenure");
                }
                yield tenure / 3;
            }
            case "yearly" -> {
                if (tenure % 12 != 0) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "For yearly installments the tenure must be a multiple of 12 months.", "tenure");
                }
                yield tenure / 12;
            }
            default -> throw new AppException(HttpStatus.BAD_REQUEST, "Please select a valid installment frequency.", "frequency");
        };
        if (n < 1) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tenure is too short for the selected frequency.", "tenure");
        }
        return n;
    }

    private LocalDate computeDueDate(LocalDate startDate, String frequency, int installmentNo) {
        return switch (frequency.toLowerCase()) {
            case "weekly" -> startDate.plusDays(7L * installmentNo);
            case "monthly" -> startDate.plusMonths(installmentNo);
            case "quarterly" -> startDate.plusMonths((long) installmentNo * 3);
            case "yearly" -> startDate.plusYears(installmentNo);
            default -> throw new AppException(HttpStatus.BAD_REQUEST, "Invalid frequency: " + frequency, "frequency");
        };
    }

    public List<InstallmentSchedule> buildScheduleEntities(Loan loan, List<ScheduleRow> rows) {
        List<InstallmentSchedule> entities = new ArrayList<>();
        for (ScheduleRow row : rows) {
            InstallmentSchedule entry = new InstallmentSchedule();
            entry.setLoan(loan);
            entry.setInstallmentNo(row.installmentNo);
            entry.setDueDate(row.dueDate);
            entry.setInstallmentAmount(row.installmentAmount);
            entry.setPrincipalPaid(row.principalPaid);
            entry.setInterestPaid(row.interestPaid);
            entry.setBalance(row.balance);
            entities.add(entry);
        }
        return entities;
    }
}

