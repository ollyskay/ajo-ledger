package com.ajosavings.group.repository;

import com.ajosavings.group.domain.Cycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CycleRepository extends JpaRepository<Cycle, UUID> {

    List<Cycle> findByGroupIdOrderByCycleNumberAsc(UUID groupId);
}
