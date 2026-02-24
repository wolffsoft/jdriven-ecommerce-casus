package com.wolffsoft.jdrivenecommerce.rest;

import com.wolffsoft.jdrivenecommerce.domain.response.CursorPageResponse;
import com.wolffsoft.jdrivenecommerce.domain.response.ProductSearchResponse;
import com.wolffsoft.jdrivenecommerce.service.elasticsearch.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/products")
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @GetMapping(path = "/search")
    public CursorPageResponse<ProductSearchResponse> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cursor) {
        String trimmedQuery = query.trim();
        return productSearchService.search(trimmedQuery, size, cursor);
    }

}
