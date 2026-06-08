package com.nisarg.repositories;

import com.nisarg.entities.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    Page<EventEntity> findByOrganizerId(UUID organizerId, Pageable pageable);

    long countByOrganizerId(UUID organizerId);
}