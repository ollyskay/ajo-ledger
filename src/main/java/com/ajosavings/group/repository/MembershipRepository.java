package com.ajosavings.group.repository;

import com.ajosavings.group.domain.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    long countByGroupId(UUID groupId);

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    List<Membership> findByGroupIdOrderByPayoutPositionAsc(UUID groupId);
}
