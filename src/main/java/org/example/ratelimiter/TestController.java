package org.example.ratelimiter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello! Your request was successful.";
    }

    @GetMapping("/world")
    public String sayWorld() {
        return "Hello World! This is another limited endpoint.";
    }
}