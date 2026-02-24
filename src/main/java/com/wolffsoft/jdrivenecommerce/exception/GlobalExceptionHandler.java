package com.wolffsoft.jdrivenecommerce.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.wolffsoft.jdrivenecommerce.util.ProblemDetailUtil.createProblemDetail;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CurrencyMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleCurrencyMismatchException(
            CurrencyMismatchException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(ElasticSearchFailedSearchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleElasticSearchFailedSearchException(
            ElasticSearchFailedSearchException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(ElasticSearchFailedUpdateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleElasticSearchFailedUpdateException(
            ElasticSearchFailedUpdateException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(ElasticSearchFailedUpsertException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleElasticSearchFailedUpsertException(
            ElasticSearchFailedUpsertException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(ElasticSearchIndicesExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleElasticSearchIndicesExistsException(
            ElasticSearchIndicesExistsException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail handleProductNotFoundException(
            ProductNotFoundException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ProblemDetail handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ProblemDetail handleException(
            Exception ex, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                ex.getMessage(),
                request
        );
    }
}
