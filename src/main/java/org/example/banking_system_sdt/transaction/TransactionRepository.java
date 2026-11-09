package org.example.banking_system_sdt.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(Long sourceAccountId, Long targetAccountId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.sourceAccountId = :sourceAccountId AND t.status = 'SUCCESSFUL' AND t.createdAt >= :since")
    BigDecimal calculateSentToday(@Param("sourceAccountId") Long sourceAccountId, @Param("since") LocalDateTime since);
}
