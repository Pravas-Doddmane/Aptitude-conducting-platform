package com.PassFamilyDoddmane.QuizeBackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class QuizSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureQuizAttemptLimitColumn() {
        jdbcTemplate.execute("""
                ALTER TABLE quizzes
                ADD COLUMN IF NOT EXISTS max_attempts_per_user INTEGER DEFAULT 0
                """);
        jdbcTemplate.execute("""
                ALTER TABLE quizzes
                ADD COLUMN IF NOT EXISTS available_from timestamptz NULL
                """);
        jdbcTemplate.execute("""
                ALTER TABLE quizzes
                ADD COLUMN IF NOT EXISTS available_to timestamptz NULL
                """);
        jdbcTemplate.execute("""
                ALTER TABLE quizzes
                ALTER COLUMN max_attempts_per_user SET DEFAULT 0
                """);
        jdbcTemplate.execute("""
                ALTER TABLE quizzes
                ALTER COLUMN max_attempts_per_user DROP NOT NULL
                """);
        jdbcTemplate.execute("""
                UPDATE quizzes
                SET max_attempts_per_user = 0
                WHERE max_attempts_per_user IS NULL
                """);
    }
}
