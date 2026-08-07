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
@Table(name = "groups")
public class Group {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "contribution_kobo", nullable = false)
    private long contributionKobo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContributionCycle cycle;

    @Column(name = "member_limit", nullable = false)
    private int memberLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupStatus status;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Group() {
    }

    private Group(UUID id, String name, long contributionKobo, ContributionCycle cycle, int memberLimit,
                   GroupStatus status, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.contributionKobo = contributionKobo;
        this.cycle = cycle;
        this.memberLimit = memberLimit;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static Group form(String name, long contributionKobo, ContributionCycle cycle, int memberLimit,
                              UUID createdBy) {
        if (contributionKobo <= 0) {
            throw new IllegalArgumentException("Contribution amount must be positive");
        }
        if (memberLimit < 2) {
            throw new IllegalArgumentException("A group needs at least two members");
        }
        return new Group(UUID.randomUUID(), name, contributionKobo, cycle, memberLimit,
                GroupStatus.FORMING, createdBy, Instant.now());
    }

    public void activate() {
        if (status != GroupStatus.FORMING) {
            throw new IllegalStateException("Group " + id + " cannot be activated from status " + status);
        }
        this.status = GroupStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getContributionKobo() {
        return contributionKobo;
    }

    public ContributionCycle getCycle() {
        return cycle;
    }

    public int getMemberLimit() {
        return memberLimit;
    }

    public GroupStatus getStatus() {
        return status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
