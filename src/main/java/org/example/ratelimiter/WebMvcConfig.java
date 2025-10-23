package org.example.ratelimiter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * This class tells Spring to use our custom interceptor.
 * It's how we apply the rate limiting logic without touching every controller.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register our RateLimitInterceptor.
        registry.addInterceptor(rateLimitInterceptor)
                // Apply it only to routes that start with "/api/"
                // We can exclude routes like "/health" or "/" here if we wanted.
                .addPathPatterns("/api/**");
    }
}
