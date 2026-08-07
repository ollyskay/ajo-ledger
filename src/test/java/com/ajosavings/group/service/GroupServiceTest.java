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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private CycleRepository cycleRepository;

    private GroupService groupService;

    private final UUID creator = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        groupService = new GroupService(groupRepository, membershipRepository, cycleRepository);
    }

    @Test
    void createsGroupInFormingStatus() {
        when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Group group = groupService.createGroup(
                new CreateGroupRequest("Six Friends", 500_000L, ContributionCycle.MONTHLY, 6, creator));

        assertThat(group.getStatus()).isEqualTo(GroupStatus.FORMING);
        assertThat(group.getMemberLimit()).isEqualTo(6);
        verify(groupRepository).save(group);
    }

    @Test
    void rejectsNonPositiveContribution() {
        assertThatThrownBy(() -> groupService.createGroup(
                new CreateGroupRequest("Bad", 0L, ContributionCycle.MONTHLY, 6, creator)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(groupRepository, never()).save(any());
    }

    @Test
    void rejectsMemberLimitBelowTwo() {
        assertThatThrownBy(() -> groupService.createGroup(
                new CreateGroupRequest("Bad", 500_000L, ContributionCycle.MONTHLY, 1, creator)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(groupRepository, never()).save(any());
    }

    @Test
    void joinAssignsSequentialPayoutPosition() {
        UUID groupId = UUID.randomUUID();
        Group group = Group.form("Six Friends", 500_000L, ContributionCycle.MONTHLY, 6, creator);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(membershipRepository.existsByGroupIdAndUserId(any(), any())).thenReturn(false);
        when(membershipRepository.countByGroupId(groupId)).thenReturn(2L);
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Membership membership = groupService.join(groupId, UUID.randomUUID());

        assertThat(membership.getPayoutPosition()).isEqualTo(3);
    }

    @Test
    void rejectsJoiningAGroupThatIsNotForming() {
        UUID groupId = UUID.randomUUID();
        Group group = Group.form("Six Friends", 500_000L, ContributionCycle.MONTHLY, 2, creator);
        group.activate(); // needs no members here, just flips status for this test
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.join(groupId, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);

        verify(membershipRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateJoin() {
        UUID groupId = UUID.randomUUID();
        Group group = Group.form("Six Friends", 500_000L, ContributionCycle.MONTHLY, 6, creator);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(membershipRepository.existsByGroupIdAndUserId(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> groupService.join(groupId, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);

        verify(membershipRepository, never()).save(any());
    }

    @Test
    void rejectsJoiningAFullGroup() {
        UUID groupId = UUID.randomUUID();
        Group group = Group.form("Six Friends", 500_000L, ContributionCycle.MONTHLY, 6, creator);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(membershipRepository.existsByGroupIdAndUserId(any(), any())).thenReturn(false);
        when(membershipRepository.countByGroupId(groupId)).thenReturn(6L);

        assertThatThrownBy(() -> groupService.join(groupId, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);

        verify(membershipRepository, never()).save(any());
    }

    @Test
    void joinAgainstUnknownGroupThrows() {
        UUID groupId = UUID.randomUUID();
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.join(groupId, UUID.randomUUID()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void rejectsActivationWithIncompleteMembership() {
        UUID groupId = UUID.randomUUID();
        Group group = Group.form("Six Friends", 500_000L, ContributionCycle.MONTHLY, 6, creator);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(membershipRepository.findByGroupIdOrderByPayoutPositionAsc(groupId))
                .thenReturn(List.of(Membership.join(groupId, UUID.randomUUID(), 1)));

        assertThatThrownBy(() -> groupService.activate(groupId))
                .isInstanceOf(IllegalStateException.class);

        verify(cycleRepository, never()).saveAll(any());
    }

    @Test
    void rejectsReactivatingAnAlreadyActiveGroup() {
        UUID groupId = UUID.randomUUID();
        Group group = Group.form("Duo", 500_000L, ContributionCycle.WEEKLY, 2, creator);
        List<Membership> members = List.of(
                Membership.join(groupId, UUID.randomUUID(), 1),
                Membership.join(groupId, UUID.randomUUID(), 2));
        group.activate();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(membershipRepository.findByGroupIdOrderByPayoutPositionAsc(groupId)).thenReturn(members);

        assertThatThrownBy(() -> groupService.activate(groupId))
                .isInstanceOf(IllegalStateException.class);

        verify(cycleRepository, never()).saveAll(any());
    }

    @Test
    void activationGeneratesOneCyclePerMemberInPayoutOrder() {
        UUID groupId = UUID.randomUUID();
        Group group = Group.form("Trio", 300_000L, ContributionCycle.WEEKLY, 3, creator);
        Membership m1 = Membership.join(groupId, UUID.randomUUID(), 1);
        Membership m2 = Membership.join(groupId, UUID.randomUUID(), 2);
        Membership m3 = Membership.join(groupId, UUID.randomUUID(), 3);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(membershipRepository.findByGroupIdOrderByPayoutPositionAsc(groupId))
                .thenReturn(List.of(m1, m2, m3));
        when(cycleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Cycle> cycles = groupService.activate(groupId);

        assertThat(cycles).hasSize(3);
        assertThat(cycles).extracting(Cycle::getCycleNumber).containsExactly(1, 2, 3);
        assertThat(cycles).extracting(Cycle::getBeneficiaryMembershipId)
                .containsExactly(m1.getId(), m2.getId(), m3.getId());

        for (int i = 0; i < cycles.size() - 1; i++) {
            assertThat(cycles.get(i).getDueAt()).isEqualTo(cycles.get(i + 1).getOpensAt());
            assertThat(cycles.get(i).getDueAt()).isAfter(cycles.get(i).getOpensAt());
        }

        assertThat(group.getStatus()).isEqualTo(GroupStatus.ACTIVE);

        ArgumentCaptor<Group> savedGroup = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(savedGroup.capture());
        assertThat(savedGroup.getValue().getStatus()).isEqualTo(GroupStatus.ACTIVE);
    }
}
