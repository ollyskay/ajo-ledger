package com.ajosavings.ledger.service;

import com.ajosavings.ledger.domain.LedgerEntry;
import com.ajosavings.ledger.dto.LedgerEntryRequest;
import com.ajosavings.ledger.exception.UnbalancedTransactionException;
import com.ajosavings.ledger.repository.AccountRepository;
import com.ajosavings.ledger.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Posts double-entry transactions to the ledger. A transaction is a set of
 * two or more {@link LedgerEntry} rows whose signed amounts sum to exactly
 * zero. Nothing here is ever updated or deleted; corrections must be posted
 * as new reversing entries.
 */
@Service
public class LedgerService {

    private static final String DEFAULT_CURRENCY = "NGN";

    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository, AccountRepository accountRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public UUID post(String description, List<LedgerEntryRequest> entries) {
        validate(entries);

        UUID transactionId = UUID.randomUUID();
        Instant now = Instant.now();

        List<LedgerEntry> posted = entries.stream()
                .map(e -> LedgerEntry.of(transactionId, e.accountId(), e.amountKobo(),
                        DEFAULT_CURRENCY, e.description() != null ? e.description() : description, now))
                .toList();

        ledgerEntryRepository.saveAll(posted);
        return transactionId;
    }

    private void validate(List<LedgerEntryRequest> entries) {
        if (entries == null || entries.size() < 2) {
            throw new UnbalancedTransactionException(
                    "A transaction requires at least two entries, got " + (entries == null ? 0 : entries.size()));
        }

        long sum = 0L;
        for (LedgerEntryRequest entry : entries) {
            if (entry.amountKobo() == 0) {
                throw new UnbalancedTransactionException("Ledger entry amount cannot be zero");
            }
            if (!accountRepository.existsById(entry.accountId())) {
                throw new IllegalArgumentException("Unknown account: " + entry.accountId());
            }
            sum += entry.amountKobo();
        }

        if (sum != 0) {
            throw new UnbalancedTransactionException(
                    "Transaction entries must sum to zero, got " + sum + " kobo");
        }
    }
}
