package org.example.ratelimiter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "api.ratelimit")
@Validated
public class RateLimitProperties {

    /**
     * The maximum number of requests allowed in the window.
     */
    private int limit = 10;

    /**
     * The time window in seconds.
     */
    private int windowSeconds = 60;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
}