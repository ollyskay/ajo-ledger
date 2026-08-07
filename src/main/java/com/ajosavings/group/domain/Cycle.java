package com.ajosavings.group.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cycles")
public class Cycle {

    @Id
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "cycle_number", nullable = false)
    private int cycleNumber;

    @Column(name = "opens_at", nullable = false)
    private Instant opensAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "beneficiary_membership_id", nullable = false)
    private UUID beneficiaryMembershipId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CycleStatus status;

    protected Cycle() {
    }

    private Cycle(UUID id, UUID groupId, int cycleNumber, Instant opensAt, Instant dueAt,
                   UUID beneficiaryMembershipId, CycleStatus status) {
        this.id = id;
        this.groupId = groupId;
        this.cycleNumber = cycleNumber;
        this.opensAt = opensAt;
        this.dueAt = dueAt;
        this.beneficiaryMembershipId = beneficiaryMembershipId;
        this.status = status;
    }

    public static Cycle schedule(UUID groupId, int cycleNumber, Instant opensAt, Instant dueAt,
                                  UUID beneficiaryMembershipId) {
        return new Cycle(UUID.randomUUID(), groupId, cycleNumber, opensAt, dueAt,
                beneficiaryMembershipId, CycleStatus.OPEN);
    }

    public UUID getId() {
        return id;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public int getCycleNumber() {
        return cycleNumber;
    }

    public Instant getOpensAt() {
        return opensAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public UUID getBeneficiaryMembershipId() {
        return beneficiaryMembershipId;
    }

    public CycleStatus getStatus() {
        return status;
    }
}
