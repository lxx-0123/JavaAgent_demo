package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TestController {
    
    @GetMapping("/test1")
    public String test1(@RequestParam String name) {
        return "Hello, " + name;
    }
    
    @PostMapping("/test2")
    public String test2(@RequestBody String body) {
        return "Received: " + body;
    }
} 