package com.ajosavings.group.service;

import com.ajosavings.group.domain.ContributionCycle;
import com.ajosavings.group.domain.Cycle;
import com.ajosavings.group.domain.Group;
import com.ajosavings.group.domain.GroupStatus;
import com.ajosavings.group.domain.Membership;
import com.ajosavings.group.dto.CreateGroupRequest;
import com.ajosavings.group.repository.CycleRepository;
import com.ajosavings.group.repository.GroupRepository;
import com.ajosavings.group.repository.MembershipRepository;
import com.ajosavings.user.domain.User;
import com.ajosavings.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Milestone 2 definition of done: create a 6-member monthly group, activate
 * it, and see a correct 6-cycle schedule with each member assigned exactly
 * one payout position.
 */
@SpringBootTest
@Testcontainers
class GroupServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private CycleRepository cycleRepository;

    @AfterEach
    void tearDown() {
        cycleRepository.deleteAll();
        membershipRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void sixMemberMonthlyGroupActivatesIntoASixCycleSchedule() {
        User creator = userRepository.save(newUser());
        List<User> members = IntStream.range(0, 6).mapToObj(i -> userRepository.save(newUser())).toList();

        Group group = groupService.createGroup(
                new CreateGroupRequest("Six Friends", 500_000L, ContributionCycle.MONTHLY, 6, creator.getId()));

        for (User member : members) {
            groupService.join(group.getId(), member.getId());
        }

        groupService.activate(group.getId());

        List<Cycle> schedule = groupService.schedule(group.getId());
        List<Membership> memberships = membershipRepository.findByGroupIdOrderByPayoutPositionAsc(group.getId());

        assertThat(schedule).hasSize(6);
        assertThat(schedule).extracting(Cycle::getCycleNumber).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(schedule).extracting(Cycle::getBeneficiaryMembershipId)
                .containsExactlyInAnyOrderElementsOf(memberships.stream().map(Membership::getId).toList());

        for (int i = 0; i < schedule.size() - 1; i++) {
            assertThat(schedule.get(i).getDueAt()).isEqualTo(schedule.get(i + 1).getOpensAt());
            long daysInCycle = ChronoUnit.DAYS.between(schedule.get(i).getOpensAt(), schedule.get(i).getDueAt());
            assertThat(daysInCycle).isBetween(28L, 31L);
        }

        assertThat(groupRepository.findById(group.getId()).orElseThrow().getStatus())
                .isEqualTo(GroupStatus.ACTIVE);
    }

    @Test
    void cannotActivateBeforeTheGroupIsFull() {
        User creator = userRepository.save(newUser());
        User onlyMember = userRepository.save(newUser());

        Group group = groupService.createGroup(
                new CreateGroupRequest("Six Friends", 500_000L, ContributionCycle.MONTHLY, 6, creator.getId()));
        groupService.join(group.getId(), onlyMember.getId());

        assertThatThrownBy(() -> groupService.activate(group.getId()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(cycleRepository.findByGroupIdOrderByCycleNumberAsc(group.getId())).isEmpty();
    }

    private static final AtomicLong PHONE_SEQUENCE = new AtomicLong(7_000_000_000L);

    private User newUser() {
        long phoneNumber = PHONE_SEQUENCE.incrementAndGet();
        return User.register("+" + phoneNumber, "user" + phoneNumber + "@example.com", "hashed-bvn", 1);
    }
}
