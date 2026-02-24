package com.wolffsoft.jdrivenecommerce.rest;

import com.wolffsoft.jdrivenecommerce.domain.request.PriceSyncRequest;
import com.wolffsoft.jdrivenecommerce.domain.response.ProductResponse;
import com.wolffsoft.jdrivenecommerce.repository.entity.ProductEntity;
import com.wolffsoft.jdrivenecommerce.repository.mapper.ProductMapper;
import com.wolffsoft.jdrivenecommerce.service.integration.PriceSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/price-integration")
public class PriceIntegrationController {

    private final PriceSyncService priceSyncService;

    @PostMapping("/prices")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponse pushPrice(@Valid @RequestBody PriceSyncRequest request) {
        ProductEntity product = priceSyncService.syncPrice(request);
        return ProductMapper.toResponse(product);
    }
}
