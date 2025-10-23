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

/**
 * This interceptor runs before every request that matches its path pattern.
 * This is where all the rate limiting logic lives.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitProperties rateLimitProperties;

    // ObjectMapper is thread-safe, so we can create one instance and reuse it.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public RateLimitInterceptor(RedisTemplate<String, String> redisTemplate, RateLimitProperties rateLimitProperties) {
        this.redisTemplate = redisTemplate;
        this.rateLimitProperties = rateLimitProperties;
    }

    /**
     * This method is called before the request reaches the controller.
     * We return true to allow the request, or false to block it.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1. Identify the user. We use IP, but this could be an API key.
        String ip = getClientIp(request);
        String key = "ratelimit:" + ip; // The Redis key we'll use

        // 2. Define the time window
        long now = System.currentTimeMillis();
        long windowStart = now - TimeUnit.SECONDS.toMillis(rateLimitProperties.getWindowSeconds());

        // 3. We use a Redis Sorted Set (ZSet).
        // It's perfect for sliding windows because we can store requests by timestamp (the "score").
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        // 4. This is the "sliding" part of the window.
        // We remove all requests (members) from the set that are older than our window.
        zSetOps.removeRangeByScore(key, 0, windowStart);

        // 5. Get all the requests that are *within* the current window.
        Set<String> entries = zSetOps.range(key, 0, -1);
        long currentRequestCount = (entries != null) ? entries.size() : 0;

        // 6. Check if they're over the limit.
        if (currentRequestCount >= rateLimitProperties.getLimit()) {
            // --- BLOCKED ---
            // Send a 429 "Too Many Requests" response
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");

            // Create a clean JSON error body
            ErrorResponse err = new ErrorResponse(429, "Too Many Requests");
            String jsonResponse = objectMapper.writeValueAsString(err);
            response.getWriter().write(jsonResponse);

            // Add helpful headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitProperties.getLimit()));
            response.setHeader("X-RateLimit-Remaining", "0");

            // Block the request from proceeding to the controller
            return false;
        }

        // --- ALLOWED ---
        // 7. Add the new request to the sorted set.
        // Value: We use nanoTime as a unique value to prevent collisions if two
        // requests arrive in the same millisecond.
        // Score: We use the current timestamp (now) so we can query by time.
        zSetOps.add(key, String.valueOf(System.nanoTime()), now);

        // 8. Set an expiration on the whole key.
        // This is a safety cleanup, so if a user is idle for a while,
        // Redis automatically deletes their key and saves memory.
        redisTemplate.expire(key, rateLimitProperties.getWindowSeconds(), TimeUnit.SECONDS);

        // 9. Add helpful headers to the successful response
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitProperties.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimitProperties.getLimit() - (currentRequestCount + 1)));

        // 10. Allow the request to proceed to the controller
        return true;
    }

    /**
     * Gets the client's IP address, handling proxies.
     * When deployed on a platform like Render, the real user IP is in the
     * "X-Forwarded-For" header, not in getRemoteAddr().
     */
    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            // If we're not behind a proxy (e.g., running locally)
            ipAddress = request.getRemoteAddr();
        } else {
            // The X-Forwarded-For header can be a list of IPs: "client, proxy1, proxy2"
            // We always want the first one in the list, which is the original client.
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }
}
