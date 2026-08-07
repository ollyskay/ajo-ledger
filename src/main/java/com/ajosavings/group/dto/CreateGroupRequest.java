package com.ajosavings.group.dto;

import com.ajosavings.group.domain.ContributionCycle;

import java.util.UUID;

public record CreateGroupRequest(String name, long contributionKobo, ContributionCycle cycle,
                                  int memberLimit, UUID createdBy) {
}
