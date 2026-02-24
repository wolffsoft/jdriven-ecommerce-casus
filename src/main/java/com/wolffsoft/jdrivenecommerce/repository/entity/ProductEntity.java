package com.wolffsoft.jdrivenecommerce.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
public class ProductEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "price_in_cents", nullable = false)
    private long priceInCents;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "attributes", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String attributes;

    @Column(name = "price_updated_at")
    private Instant priceUpdatedAt;

    protected ProductEntity() {}

    public ProductEntity(
            String name,
            String description,
            long priceInCents,
            String currency,
            String attributes) {
        this.name = name;
        this.description = description;
        this.priceInCents = priceInCents;
        this.currency = currency;
        this.attributes = attributes;
    }
}
