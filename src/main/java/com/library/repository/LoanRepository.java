package com.library.repository;

import com.library.model.Loan;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findAllByOrderByIdDesc();

    List<Loan> findByMemberIdOrderByIdDesc(Long memberId);

    List<Loan> findByMemberIdAndReturnDateIsNullOrderByIdDesc(Long memberId);

    List<Loan> findByReturnDateIsNullAndDueDateLessThanEqual(LocalDate date);

    Optional<Loan> findFirstByBookIdAndMemberIdAndReturnDateIsNull(Long bookId, Long memberId);

    boolean existsByBookIdAndMemberIdAndReturnDateIsNull(Long bookId, Long memberId);

    long countByBookIdAndReturnDateIsNull(Long bookId);

    long countByMemberIdAndReturnDateIsNull(Long memberId);

    long countByReturnDateIsNull();

    long countByReturnDateIsNullAndDueDateBefore(LocalDate date);

    /** Rows of [bookId, activeLoanCount]. */
    @Query("select l.book.id, count(l) from Loan l where l.returnDate is null group by l.book.id")
    List<Object[]> activeCountsByBook();

    void deleteByBookId(Long bookId);

    void deleteByMemberId(Long memberId);
}
