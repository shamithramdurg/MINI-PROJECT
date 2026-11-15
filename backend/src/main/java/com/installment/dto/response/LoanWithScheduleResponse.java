package com.installment.dto.response;

import java.util.List;

public class LoanWithScheduleResponse {

    private LoanResponse loan;
    private List<ScheduleEntryResponse> schedule;

    public LoanWithScheduleResponse() {}

    public LoanWithScheduleResponse(LoanResponse loan, List<ScheduleEntryResponse> schedule) {
        this.loan = loan;
        this.schedule = schedule;
    }

    public LoanResponse getLoan() { return loan; }
    public void setLoan(LoanResponse loan) { this.loan = loan; }

    public List<ScheduleEntryResponse> getSchedule() { return schedule; }
    public void setSchedule(List<ScheduleEntryResponse> schedule) { this.schedule = schedule; }
}

