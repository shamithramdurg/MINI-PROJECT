package com.installment.repository;

import com.installment.entity.Loan;
import com.installment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserOrderByCreatedAtDesc(User user);

    Optional<Loan> findByLoanIdAndUser(Long loanId, User user);

    @Query("SELECT l FROM Loan l JOIN FETCH l.user ORDER BY l.createdAt DESC")
    List<Loan> findAllWithUser();

    @Query("SELECT l.user.userId, COUNT(l) FROM Loan l GROUP BY l.user.userId")
    List<Object[]> countLoansGroupedByUserId();

    long count();
}

