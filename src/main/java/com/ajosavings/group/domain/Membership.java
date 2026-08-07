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
@Table(name = "memberships")
public class Membership {

    @Id
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "payout_position", nullable = false)
    private int payoutPosition;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status;

    protected Membership() {
    }

    private Membership(UUID id, UUID groupId, UUID userId, int payoutPosition,
                        Instant joinedAt, MembershipStatus status) {
        this.id = id;
        this.groupId = groupId;
        this.userId = userId;
        this.payoutPosition = payoutPosition;
        this.joinedAt = joinedAt;
        this.status = status;
    }

    public static Membership join(UUID groupId, UUID userId, int payoutPosition) {
        return new Membership(UUID.randomUUID(), groupId, userId, payoutPosition,
                Instant.now(), MembershipStatus.ACTIVE);
    }

    public UUID getId() {
        return id;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getPayoutPosition() {
        return payoutPosition;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public MembershipStatus getStatus() {
        return status;
    }
}
