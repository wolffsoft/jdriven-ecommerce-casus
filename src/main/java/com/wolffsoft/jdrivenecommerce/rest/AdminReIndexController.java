package com.wolffsoft.jdrivenecommerce.rest;

import com.wolffsoft.jdrivenecommerce.service.elasticsearch.ReIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminReIndexController {

    private final ReIndexService reIndexService;

    /**
     * Triggers a rebuild of the ES index from Postgres.
     * Example: POST /admin/reindex?batchSize=1000
     */
    @PostMapping("/reindex")
    public ReIndexService.ReindexResult reindex(
            @RequestParam(value = "batchSize", defaultValue = "1000") int batchSize
    ) {
        return reIndexService.reindexAll(batchSize);
    }
}
