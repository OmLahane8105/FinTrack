package com.fintrack.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
     * 400 - Bean validation errors
     *
     * Example:
     * Amount is required
     * Description must not exceed 255 characters
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                message,
                request
        );
    }

    /*
     * 400 - Invalid query/path parameter type
     *
     * Example:
     * /api/reports/summary?from=hello
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {

        String message =
                "Invalid value for parameter '" +
                        exception.getName() +
                        "'";

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                message,
                request
        );
    }

    /*
     * 400 - Invalid JSON body
     *
     * Example:
     * Invalid enum value
     * Malformed JSON
     * Invalid date format
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {

        String message = "Invalid request body";

        Throwable cause = exception.getMostSpecificCause();

        if (cause != null && cause.getMessage() != null) {

            String causeMessage = cause.getMessage();

            if (causeMessage.contains("TransactionType")) {
                message = "Invalid transaction type";
            } else if (causeMessage.contains("LocalDate")) {
                message = "Invalid date format. Expected yyyy-MM-dd";
            }
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                message,
                request
        );
    }

    /*
     * 400 - Constraint violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {

        String message = exception.getConstraintViolations()
                .stream()
                .map(violation ->
                        violation.getPropertyPath()
                                + ": "
                                + violation.getMessage())
                .collect(Collectors.joining(", "));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                message,
                request
        );
    }

    /*
     * 400 - Business validation errors
     *
     * Your services currently use IllegalArgumentException
     * for invalid financial operations.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                exception.getMessage(),
                request
        );
    }

    /*
     * 404 - Resource does not exist
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Not Found",
                exception.getMessage(),
                request
        );
    }

    /*
     * 409 - Database constraint conflict
     *
     * Example:
     * Duplicate unique value.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "Conflict",
                "The requested operation conflicts with existing data",
                request
        );
    }

    /*
     * 500 - Unexpected server errors
     *
     * Do not expose internal exception details to the frontend.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(
            Exception exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred",
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request
    ) {

        ApiErrorResponse response = new ApiErrorResponse(
                status.value(),
                error,
                message,
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}