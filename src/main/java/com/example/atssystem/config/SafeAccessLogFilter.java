package com.example.atssystem.config;

import com.example.atssystem.common.GlobalExceptionHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Access logging without request bodies or raw resource identifiers.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class SafeAccessLogFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(SafeAccessLogFilter.class);

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		long start = System.currentTimeMillis();
		try {
			filterChain.doFilter(request, response);
		} finally {
			String uri = request.getRequestURI();
			if (uri != null && uri.startsWith("/api/")) {
				log.info(
						"{} {} -> {} ({} ms)",
						request.getMethod(),
						GlobalExceptionHandler.sanitizePath(uri),
						response.getStatus(),
						System.currentTimeMillis() - start
				);
			}
		}
	}
}
