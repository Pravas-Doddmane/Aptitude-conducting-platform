package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.dto.stats.UserStatsResponse;
import com.PassFamilyDoddmane.QuizeBackend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AnalyticsService analyticsService;

    @GetMapping("/me/stats")
    public ResponseEntity<UserStatsResponse> myStats() {
        return ResponseEntity.ok(analyticsService.myStats());
    }
}
