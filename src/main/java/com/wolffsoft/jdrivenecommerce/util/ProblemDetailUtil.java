package com.wolffsoft.jdrivenecommerce.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.time.Instant;

@NoArgsConstructor
@Slf4j
public final class ProblemDetailUtil {

    public static ProblemDetail createProblemDetail(
            HttpStatus httpStatus,
            String title,
            String detail,
            HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(httpStatus, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("path", request.getRequestURI());
        return problemDetail;
    }
}
