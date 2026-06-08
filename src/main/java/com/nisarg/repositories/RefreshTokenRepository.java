package com.nisarg.repositories;

import com.nisarg.entities.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByToken(String token);

    List<RefreshTokenEntity> findByUser_Id(UUID userId);

    void deleteByUser_Id(UUID userId);
}