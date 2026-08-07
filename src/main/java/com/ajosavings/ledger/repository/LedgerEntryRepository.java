package com.ajosavings.ledger.repository;

import com.ajosavings.ledger.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByAccountIdOrderByCreatedAtAsc(UUID accountId);

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    @Query("select coalesce(sum(e.amountKobo), 0) from LedgerEntry e where e.accountId = :accountId")
    long sumAmountKoboByAccountId(@Param("accountId") UUID accountId);

    @Query("select coalesce(sum(e.amountKobo), 0) from LedgerEntry e")
    long sumAllAmountKobo();
}
