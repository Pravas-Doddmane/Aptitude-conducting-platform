package com.PassFamilyDoddmane.QuizeBackend.dto.stats;

public record UserStatsResponse(
        long totalAttempts,
        double averageScore,
        Integer bestScore,
        long completedAttempts
) {
}
