package com.installment.repository;

import com.installment.entity.InstallmentSchedule;
import com.installment.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstallmentScheduleRepository extends JpaRepository<InstallmentSchedule, Long> {
    List<InstallmentSchedule> findByLoanOrderByInstallmentNoAsc(Loan loan);
    void deleteByLoan(Loan loan);
}

