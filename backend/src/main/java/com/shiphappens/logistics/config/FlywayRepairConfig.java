package com.shiphappens.logistics.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Always runs Flyway repair before migrate.
 *
 * This clears any FAILED migration entries from flyway_schema_history so that
 * a fixed migration script can be re-applied on the next deploy without manual
 * intervention. Safe to leave in permanently — repair is a no-op when there
 * are no failed migrations.
 */
@Configuration
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
