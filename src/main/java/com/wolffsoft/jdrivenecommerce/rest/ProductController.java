package com.wolffsoft.jdrivenecommerce.rest;

import com.wolffsoft.jdrivenecommerce.domain.request.CreateProductRequest;
import com.wolffsoft.jdrivenecommerce.domain.request.UpdatePriceRequest;
import com.wolffsoft.jdrivenecommerce.domain.request.UpdateProductRequest;
import com.wolffsoft.jdrivenecommerce.domain.response.ProductResponse;
import com.wolffsoft.jdrivenecommerce.repository.entity.ProductEntity;
import com.wolffsoft.jdrivenecommerce.repository.mapper.ProductMapper;
import com.wolffsoft.jdrivenecommerce.service.product.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        ProductEntity savedProduct = productService.create(request);
        return ProductMapper.toResponse(savedProduct);
    }

    @GetMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponse getById(@PathVariable UUID id) {
        ProductEntity retrievedProduct = productService.getProductOrThrow(id);
        return ProductMapper.toResponse(retrievedProduct);
    }

    @PutMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        ProductEntity updatedProduct = productService.update(id, request);
        return ProductMapper.toResponse(updatedProduct);
    }

    @PatchMapping(path = "/{id}/price")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponse updatePrice(@PathVariable UUID id, @Valid @RequestBody UpdatePriceRequest request) {
        ProductEntity updatedPrice = productService.updatePrice(id, request);
        return ProductMapper.toResponse(updatedPrice);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID productId) {
        productService.deleteProduct(productId);
    }
}
