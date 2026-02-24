package com.wolffsoft.jdrivenecommerce.repository.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wolffsoft.jdrivenecommerce.domain.response.ProductResponse;
import com.wolffsoft.jdrivenecommerce.repository.entity.ProductEntity;
import com.wolffsoft.jdrivenecommerce.util.JsonUtil;
import com.wolffsoft.jdrivenecommerce.util.MoneyUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductMapper {

    public static ProductResponse toResponse(ProductEntity product) {
        return new ProductResponse(
                product.getId().toString(),
                product.getName(),
                product.getDescription(),
                MoneyUtil.fromCents(product.getPriceInCents()),
                product.getCurrency(),
                JsonUtil.fromJson(product.getAttributes(), new TypeReference<>() {}),
                product.getPriceUpdatedAt()
        );
    }
}
