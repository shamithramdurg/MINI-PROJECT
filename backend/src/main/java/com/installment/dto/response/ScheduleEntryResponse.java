package com.installment.dto.response;

import com.installment.entity.InstallmentSchedule;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ScheduleEntryResponse {

    private Long scheduleId;
    // JS uses: row.no, row.dueDate, row.amount, row.principal, row.interest, row.balance
    private Integer no;
    private Integer installmentNo;
    private LocalDate dueDate;
    private BigDecimal amount;
    private BigDecimal installmentAmount;
    private BigDecimal principal;
    private BigDecimal principalPaid;
    private BigDecimal interest;
    private BigDecimal interestPaid;
    private BigDecimal balance;

    public ScheduleEntryResponse() {}

    public ScheduleEntryResponse(InstallmentSchedule entry) {
        this.scheduleId = entry.getScheduleId();
        this.no = entry.getInstallmentNo();
        this.installmentNo = entry.getInstallmentNo();
        this.dueDate = entry.getDueDate();
        this.amount = entry.getInstallmentAmount();
        this.installmentAmount = entry.getInstallmentAmount();
        this.principal = entry.getPrincipalPaid();
        this.principalPaid = entry.getPrincipalPaid();
        this.interest = entry.getInterestPaid();
        this.interestPaid = entry.getInterestPaid();
        this.balance = entry.getBalance();
    }

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public Integer getNo() { return no; }
    public void setNo(Integer no) { this.no = no; }

    public Integer getInstallmentNo() { return installmentNo; }
    public void setInstallmentNo(Integer installmentNo) { this.installmentNo = installmentNo; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getInstallmentAmount() { return installmentAmount; }
    public void setInstallmentAmount(BigDecimal installmentAmount) { this.installmentAmount = installmentAmount; }

    public BigDecimal getPrincipal() { return principal; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }

    public BigDecimal getPrincipalPaid() { return principalPaid; }
    public void setPrincipalPaid(BigDecimal principalPaid) { this.principalPaid = principalPaid; }

    public BigDecimal getInterest() { return interest; }
    public void setInterest(BigDecimal interest) { this.interest = interest; }

    public BigDecimal getInterestPaid() { return interestPaid; }
    public void setInterestPaid(BigDecimal interestPaid) { this.interestPaid = interestPaid; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}


