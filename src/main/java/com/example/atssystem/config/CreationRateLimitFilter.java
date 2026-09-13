package com.example.atssystem.config;

import com.example.atssystem.common.TooManyRequestsException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class CreationRateLimitFilter extends OncePerRequestFilter {

	private static final Set<String> LIMITED_PATHS = Set.of(
			"/api/resumes",
			"/api/jobs",
			"/api/analyses"
	);

	private final AtsProperties atsProperties;
	private final HandlerExceptionResolver handlerExceptionResolver;
	private final Map<String, Deque<Long>> hitsByClient = new ConcurrentHashMap<>();

	public CreationRateLimitFilter(
			AtsProperties atsProperties,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver
	) {
		this.atsProperties = atsProperties;
		this.handlerExceptionResolver = handlerExceptionResolver;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		if (isLimited(request) && !allow(clientKey(request))) {
			handlerExceptionResolver.resolveException(
					request,
					response,
					null,
					new TooManyRequestsException("Too many create requests. Please wait and try again.")
			);
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean isLimited(HttpServletRequest request) {
		return HttpMethod.POST.matches(request.getMethod())
				&& LIMITED_PATHS.contains(request.getRequestURI());
	}

	private boolean allow(String clientKey) {
		int limit = Math.max(1, atsProperties.getRateLimit().getCreatesPerMinute());
		long now = Instant.now().toEpochMilli();
		long windowStart = now - 60_000L;
		Deque<Long> hits = hitsByClient.computeIfAbsent(clientKey, key -> new ArrayDeque<>());
		synchronized (hits) {
			while (!hits.isEmpty() && hits.peekFirst() < windowStart) {
				hits.removeFirst();
			}
			if (hits.size() >= limit) {
				return false;
			}
			hits.addLast(now);
			return true;
		}
	}

	private static String clientKey(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
	}
}
