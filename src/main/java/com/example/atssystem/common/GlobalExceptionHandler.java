package com.example.atssystem.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(
			ResourceNotFoundException ex,
			HttpServletRequest request
	) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
				new ErrorResponse(
						Instant.now(),
						HttpStatus.NOT_FOUND.value(),
						"Not Found",
						ex.getMessage(),
						request.getRequestURI(),
						null
				)
		);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(
			BadRequestException ex,
			HttpServletRequest request
	) {
		return ResponseEntity.badRequest().body(
				new ErrorResponse(
						Instant.now(),
						HttpStatus.BAD_REQUEST.value(),
						"Bad Request",
						ex.getMessage(),
						request.getRequestURI(),
						null
				)
		);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(
			MethodArgumentNotValidException ex,
			HttpServletRequest request
	) {
		List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
				.toList();

		return ResponseEntity.badRequest().body(
				new ErrorResponse(
						Instant.now(),
						HttpStatus.BAD_REQUEST.value(),
						"Bad Request",
						"Validation failed",
						request.getRequestURI(),
						fieldErrors
				)
		);
	}
}
