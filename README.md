Mini API Rate Limiter (Java / Spring Boot)

This is a backend service built with Java and Spring Boot that satisfies the "Mini API Rate Limiter" assignment.

The service uses a Redis Sorted Set to implement a true sliding time window algorithm, limiting requests by IP address. The rate-limiting logic is modular, using a Spring HandlerInterceptor to keep it separate from the main business logic.

Live URL: [https://your-service-name.onrender.com/api/hello](https://redisratelimiter.onrender.com/api/hello)

Features

True Sliding Window: Uses a Redis Sorted Set (ZSET) to store request timestamps, providing a precise sliding window.

Modular Code: All rate-limiting logic is contained in RateLimitInterceptor.java, which is applied to routes without cluttering controllers.

Configurable Limits: The request limit and window duration can be easily changed in the application.properties file.

Production Ready: Correctly identifies user IPs behind a proxy (like Render) by checking the X-Forwarded-For header.

Clean Error Responses: Returns a proper 429 Too Many Requests status with a JSON error body.

API Endpoints (for Testing)

The rate limiter is applied to all routes under /api/.

GET /api/hello

GET /api/world

Test with curl:

# This will show the X-RateLimit headers
curl -v http://localhost:8080/api/hello


On Success: You will see headers like X-RateLimit-Limit: 10 and X-RateLimit-Remaining: 9.

On Failure: You will receive an HTTP 429 with a JSON body.

Configuration

You can configure the rate limiter in src/main/resources/application.properties:

# The maximum number of requests allowed in the window
api.ratelimit.limit=10

# The time window in seconds
api.ratelimit.window-seconds=60


How to Run (Locally)

Prerequisites

Java 21 (or 17)

Maven

Redis server running on localhost:6379. (The app will fail to start if it cannot connect to Redis).

Steps

Clone the repository:

git clone [https://github.com/shubh1978/RedisRateLimiter.git](https://github.com/shubh1978/RedisRateLimiter.git)
cd RedisRateLimiter


Ensure Redis is running:

# If installed with Homebrew
brew services start redis


Run the application with Maven:

mvn spring-boot:run


The application will be available at http://localhost:8080.

How to Run (Production on Render)

This project is configured for deployment on Render using Docker.

Create a "Key Value" service on Render for your Redis instance.

Create a "Web Service" on Render and connect this GitHub repository.

Set the runtime to Docker. Render will automatically find and use the Dockerfile.

Add the following Environment Variables:

PORT: 8080

SPRING_DATA_REDIS_URL: (Paste the Internal Redis URL from your Render "Key Value" service).
