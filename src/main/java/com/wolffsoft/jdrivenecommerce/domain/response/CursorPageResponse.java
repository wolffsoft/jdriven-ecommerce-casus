package com.wolffsoft.jdrivenecommerce.domain.response;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> items,
        int size,
        String nextCursor
) {}
