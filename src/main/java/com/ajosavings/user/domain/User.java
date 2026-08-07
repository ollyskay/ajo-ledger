package com.ajosavings.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(unique = true)
    private String email;

    @Column(name = "bvn_hash")
    private String bvnHash;

    @Column(name = "kyc_tier", nullable = false)
    private int kycTier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    private User(UUID id, String phone, String email, String bvnHash, int kycTier, Instant createdAt) {
        this.id = id;
        this.phone = phone;
        this.email = email;
        this.bvnHash = bvnHash;
        this.kycTier = kycTier;
        this.createdAt = createdAt;
    }

    public static User register(String phone, String email, String bvnHash, int kycTier) {
        return new User(UUID.randomUUID(), phone, email, bvnHash, kycTier, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getBvnHash() {
        return bvnHash;
    }

    public int getKycTier() {
        return kycTier;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
