package com.wolffsoft.jdrivenecommerce.repository;

import com.wolffsoft.jdrivenecommerce.repository.entity.PriceUpdateInboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PriceUpdateInboxRepository extends JpaRepository<PriceUpdateInboxEntity, UUID> {

    boolean existsByRequestId(String requestId);
}
