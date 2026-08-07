package com.ajosavings.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A single append-only movement of money against one account. Rows are never
 * updated or deleted - corrections are posted as new reversing entries.
 * Balances are derived by summing entries per account; nothing here is mutable.
 */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "amount_kobo", nullable = false)
    private long amountKobo;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    private LedgerEntry(UUID id, UUID transactionId, UUID accountId, long amountKobo,
                         String currency, String description, Instant createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amountKobo = amountKobo;
        this.currency = currency;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static LedgerEntry of(UUID transactionId, UUID accountId, long amountKobo,
                                  String currency, String description, Instant createdAt) {
        if (amountKobo == 0) {
            throw new IllegalArgumentException("Ledger entry amount cannot be zero");
        }
        return new LedgerEntry(UUID.randomUUID(), transactionId, accountId, amountKobo,
                currency, description, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public long getAmountKobo() {
        return amountKobo;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
