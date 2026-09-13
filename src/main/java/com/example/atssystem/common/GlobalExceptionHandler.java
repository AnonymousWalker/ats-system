package com.example.atssystem.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(
			ResourceNotFoundException ex,
			HttpServletRequest request
	) {
		log.warn("Not found on {}", sanitizePath(request.getRequestURI()));
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
				error(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request)
		);
	}

	@ExceptionHandler(ResourceExpiredException.class)
	public ResponseEntity<ErrorResponse> handleExpired(
			ResourceExpiredException ex,
			HttpServletRequest request
	) {
		log.info("Expired resource rejected on {}", sanitizePath(request.getRequestURI()));
		return ResponseEntity.status(HttpStatus.GONE).body(
				error(HttpStatus.GONE, "Gone", ex.getMessage(), request)
		);
	}

	@ExceptionHandler(TooManyRequestsException.class)
	public ResponseEntity<ErrorResponse> handleTooManyRequests(
			TooManyRequestsException ex,
			HttpServletRequest request
	) {
		log.warn("Rate limit hit on {}", sanitizePath(request.getRequestURI()));
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
				error(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", ex.getMessage(), request)
		);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(
			BadRequestException ex,
			HttpServletRequest request
	) {
		return ResponseEntity.badRequest().body(
				error(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request)
		);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleMaxUploadSize(
			MaxUploadSizeExceededException ex,
			HttpServletRequest request
	) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
				error(
						HttpStatus.PAYLOAD_TOO_LARGE,
						"Payload Too Large",
						"File exceeds the 5 MB upload limit. Paste the resume text instead, or upload a smaller PDF/DOCX.",
						request
				)
		);
	}

	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<ErrorResponse> handleMultipart(
			MultipartException ex,
			HttpServletRequest request
	) {
		if (isSizeExceeded(ex)) {
			return handleMaxUploadSize(new MaxUploadSizeExceededException(5L * 1024 * 1024), request);
		}
		return ResponseEntity.badRequest().body(
				error(
						HttpStatus.BAD_REQUEST,
						"Bad Request",
						"Invalid multipart upload. Send a form field named 'file', or paste resume text as JSON.",
						request
				)
		);
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ErrorResponse> handleMissingPart(
			MissingServletRequestPartException ex,
			HttpServletRequest request
	) {
		return ResponseEntity.badRequest().body(
				error(
						HttpStatus.BAD_REQUEST,
						"Bad Request",
						"Missing multipart part '" + ex.getRequestPartName()
								+ "'. Upload a PDF/DOCX as 'file', or paste resume text as JSON.",
						request
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
						sanitizePath(request.getRequestURI()),
						fieldErrors
				)
		);
	}

	private static ErrorResponse error(
			HttpStatus status,
			String error,
			String message,
			HttpServletRequest request
	) {
		return new ErrorResponse(
				Instant.now(),
				status.value(),
				error,
				message,
				sanitizePath(request.getRequestURI()),
				null
		);
	}

	/**
	 * Scrub UUID path segments so access identifiers are not echoed in logs/responses.
	 */
	public static String sanitizePath(String path) {
		if (path == null) {
			return null;
		}
		return path.replaceAll(
				"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
				"{id}"
		);
	}

	private static boolean isSizeExceeded(Throwable ex) {
		Throwable current = ex;
		while (current != null) {
			if (current instanceof MaxUploadSizeExceededException) {
				return true;
			}
			String message = current.getMessage();
			if (message != null && message.toLowerCase(Locale.ROOT).contains("exceeds")) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
