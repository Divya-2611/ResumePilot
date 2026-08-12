package com.resumepilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the AI Resume Optimizer backend.
 *
 * <p>Architecture layering (Controller → Service → Repository → Database) follows
 * clean architecture principles: controllers only handle HTTP concerns, services
 * contain business logic, repositories abstract persistence.</p>
 */
@SpringBootApplication
@EnableJpaAuditing
public class ResumePilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumePilotApplication.class, args);
    }
}
