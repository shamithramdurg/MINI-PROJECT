package com.installment.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class LoanRequest {

    @NotNull(message = "Principal is required")
    @DecimalMin(value = "100.00", message = "Principal amount must be between 100 and 1000000000.")
    @DecimalMax(value = "1000000000.00", message = "Principal amount must be between 100 and 1000000000.")
    private BigDecimal principal;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.00", message = "Interest rate must be between 0 and 100%.")
    @DecimalMax(value = "100.00", message = "Interest rate must be between 0 and 100%.")
    private BigDecimal interestRate;

    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Tenure must be a whole number of months between 1 and 600.")
    @Max(value = 600, message = "Tenure must be a whole number of months between 1 and 600.")
    private Integer tenure;

    @NotBlank(message = "Frequency is required")
    @Pattern(regexp = "weekly|monthly|quarterly|yearly", message = "Frequency must be weekly, monthly, quarterly, or yearly")
    private String frequency;

    @NotBlank(message = "Start date is required")
    private String startDate;

    public BigDecimal getPrincipal() { return principal; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public Integer getTenure() { return tenure; }
    public void setTenure(Integer tenure) { this.tenure = tenure; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
}

