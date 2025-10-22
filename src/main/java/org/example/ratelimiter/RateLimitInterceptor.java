package org.example.ratelimiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.response.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitProperties rateLimitProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public RateLimitInterceptor(RedisTemplate<String, String> redisTemplate, RateLimitProperties rateLimitProperties) {
        this.redisTemplate = redisTemplate;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String ip = getClientIp(request);
        String key = "ratelimit:" + ip;

        long now = System.currentTimeMillis();
        long windowStart = now - TimeUnit.SECONDS.toMillis(rateLimitProperties.getWindowSeconds());

        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        zSetOps.removeRangeByScore(key, 0, windowStart);

        Set<String> entries = zSetOps.range(key, 0, -1);
        long currentRequestCount = (entries != null) ? entries.size() : 0;

        if (currentRequestCount >= rateLimitProperties.getLimit()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
            response.setContentType("application/json");

            ErrorResponse err = new ErrorResponse(429, "Too Many Requests");
            String jsonResponse = objectMapper.writeValueAsString(err);
            response.getWriter().write(jsonResponse);

            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitProperties.getLimit()));
            response.setHeader("X-RateLimit-Remaining", "0");

            return false;
        }

        zSetOps.add(key, String.valueOf(System.nanoTime()), now);

        redisTemplate.expire(key, rateLimitProperties.getWindowSeconds(), TimeUnit.SECONDS);

        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitProperties.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimitProperties.getLimit() - (currentRequestCount + 1)));

        return true;
    }

    /**
     * Gets the client's IP address, handling proxies.
     * Checks "X-Forwarded-For" header first, then falls back to getRemoteAddr().
     */
    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        } else {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }
}