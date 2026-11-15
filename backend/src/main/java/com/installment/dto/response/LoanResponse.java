package com.installment.dto.response;

import com.installment.entity.Loan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LoanResponse {

    // "id" alias used by the frontend JS (l.id, loan.id)
    private Long id;
    private Long loanId;
    private Long userId;
    private String ownerName;
    private String ownerEmail;
    private BigDecimal principal;
    private BigDecimal interestRate;
    private Integer tenure;
    private String frequency;
    private LocalDate startDate;
    private Integer numInstallments;
    private BigDecimal installmentAmount;
    private BigDecimal totalPayment;
    private BigDecimal totalInterest;
    private LocalDateTime createdAt;

    public LoanResponse() {}

    public LoanResponse(Loan loan) {
        this.id = loan.getLoanId();
        this.loanId = loan.getLoanId();
        this.userId = loan.getUser().getUserId();
        this.ownerName = loan.getUser().getName();
        this.ownerEmail = loan.getUser().getEmail();
        this.principal = loan.getPrincipal();
        this.interestRate = loan.getInterestRate();
        this.tenure = loan.getTenure();
        this.frequency = loan.getFrequency();
        this.startDate = loan.getStartDate();
        this.numInstallments = loan.getNumInstallments();
        this.installmentAmount = loan.getInstallmentAmount();
        this.totalPayment = loan.getTotalPayment();
        this.totalInterest = loan.getTotalInterest();
        this.createdAt = loan.getCreatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public BigDecimal getPrincipal() { return principal; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public Integer getTenure() { return tenure; }
    public void setTenure(Integer tenure) { this.tenure = tenure; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public Integer getNumInstallments() { return numInstallments; }
    public void setNumInstallments(Integer numInstallments) { this.numInstallments = numInstallments; }

    public BigDecimal getInstallmentAmount() { return installmentAmount; }
    public void setInstallmentAmount(BigDecimal installmentAmount) { this.installmentAmount = installmentAmount; }

    public BigDecimal getTotalPayment() { return totalPayment; }
    public void setTotalPayment(BigDecimal totalPayment) { this.totalPayment = totalPayment; }

    public BigDecimal getTotalInterest() { return totalInterest; }
    public void setTotalInterest(BigDecimal totalInterest) { this.totalInterest = totalInterest; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

