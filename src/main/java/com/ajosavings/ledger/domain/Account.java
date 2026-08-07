package com.ajosavings.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * An account in the double-entry ledger. Accounts never store a balance -
 * balances are always derived by summing {@link LedgerEntry} rows.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Account() {
    }

    public Account(UUID id, AccountType type, UUID ownerId, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    public static Account open(AccountType type, UUID ownerId) {
        return new Account(UUID.randomUUID(), type, ownerId, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public AccountType getType() {
        return type;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
