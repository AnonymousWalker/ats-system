package com.example.atssystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final AtsProperties atsProperties;

	public WebConfig(AtsProperties atsProperties) {
		this.atsProperties = atsProperties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(atsProperties.getCors().getAllowedOrigins().toArray(String[]::new))
				.allowedMethods(
						HttpMethod.GET.name(),
						HttpMethod.POST.name(),
						HttpMethod.PUT.name(),
						HttpMethod.DELETE.name(),
						HttpMethod.OPTIONS.name()
				)
				.allowedHeaders(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT)
				.allowCredentials(false)
				.maxAge(3600);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(noStoreInterceptor()).addPathPatterns("/api/**");
	}

	@Bean
	HandlerInterceptor noStoreInterceptor() {
		return new HandlerInterceptor() {
			@Override
			public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
				response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
				response.setHeader(HttpHeaders.PRAGMA, "no-cache");
				response.setHeader("X-Robots-Tag", "noindex, nofollow");
				return true;
			}
		};
	}
}
