package com.ajosavings.group.service;

import com.ajosavings.group.domain.Cycle;
import com.ajosavings.group.domain.Group;
import com.ajosavings.group.domain.GroupStatus;
import com.ajosavings.group.domain.Membership;
import com.ajosavings.group.dto.CreateGroupRequest;
import com.ajosavings.group.repository.CycleRepository;
import com.ajosavings.group.repository.GroupRepository;
import com.ajosavings.group.repository.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Creates groups, admits members in join order, and generates the rotation
 * schedule when a group activates. Payout position is assigned by join
 * order - the simplest rule that is still fully auditable after the fact.
 */
@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final CycleRepository cycleRepository;

    public GroupService(GroupRepository groupRepository, MembershipRepository membershipRepository,
                         CycleRepository cycleRepository) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.cycleRepository = cycleRepository;
    }

    @Transactional
    public Group createGroup(CreateGroupRequest request) {
        Group group = Group.form(request.name(), request.contributionKobo(), request.cycle(),
                request.memberLimit(), request.createdBy());
        return groupRepository.save(group);
    }

    @Transactional
    public Membership join(UUID groupId, UUID userId) {
        Group group = getGroup(groupId);

        if (group.getStatus() != GroupStatus.FORMING) {
            throw new IllegalStateException("Group " + groupId + " is not open for joining");
        }
        if (membershipRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new IllegalStateException("User " + userId + " has already joined group " + groupId);
        }

        long currentMembers = membershipRepository.countByGroupId(groupId);
        if (currentMembers >= group.getMemberLimit()) {
            throw new IllegalStateException("Group " + groupId + " is already full");
        }

        Membership membership = Membership.join(groupId, userId, (int) currentMembers + 1);
        return membershipRepository.save(membership);
    }

    @Transactional
    public List<Cycle> activate(UUID groupId) {
        Group group = getGroup(groupId);
        List<Membership> members = membershipRepository.findByGroupIdOrderByPayoutPositionAsc(groupId);

        if (members.size() != group.getMemberLimit()) {
            throw new IllegalStateException("Group " + groupId + " needs exactly " + group.getMemberLimit()
                    + " members to activate, has " + members.size());
        }

        List<Cycle> cycles = new ArrayList<>();
        Instant cursor = Instant.now();
        for (Membership member : members) {
            Instant opensAt = cursor;
            Instant dueAt = group.getCycle().advance(opensAt);
            cycles.add(Cycle.schedule(groupId, member.getPayoutPosition(), opensAt, dueAt, member.getId()));
            cursor = dueAt;
        }

        group.activate();
        groupRepository.save(group);
        return cycleRepository.saveAll(cycles);
    }

    public List<Cycle> schedule(UUID groupId) {
        return cycleRepository.findByGroupIdOrderByCycleNumberAsc(groupId);
    }

    private Group getGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("No group with id " + groupId));
    }
}
