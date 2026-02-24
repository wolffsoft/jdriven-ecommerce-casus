package com.wolffsoft.jdrivenecommerce.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "price_update_inbox",
        uniqueConstraints = @UniqueConstraint(name = "catalog_price_update_inbox_request_id", columnNames = "request_id")
)
@Getter
@Setter
public class PriceUpdateInboxEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id", nullable = false, updatable = false)
    private String requestId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "effective_at", nullable = false, updatable = false)
    private Instant effectiveAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "source")
    private String source;

    protected PriceUpdateInboxEntity() {}

    public PriceUpdateInboxEntity(
            String requestId,
            UUID productId,
            Instant effectiveAt,
            Instant receivedAt,
            String source) {
        this.requestId = requestId;
        this.productId = productId;
        this.effectiveAt = effectiveAt;
        this.receivedAt = receivedAt;
        this.source = source;
    }
}
