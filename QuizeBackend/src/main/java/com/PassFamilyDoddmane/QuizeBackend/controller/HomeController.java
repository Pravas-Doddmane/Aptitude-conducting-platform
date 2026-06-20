package com.PassFamilyDoddmane.QuizeBackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    @GetMapping("/")
    public String healthCheck() {
        return "Quiz backend is running";
    }
}
