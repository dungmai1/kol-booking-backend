package kolbooking.datn.common.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Dev-only Flyway strategy: run {@code repair()} before {@code migrate()} so that
 * locally-edited migrations (especially seed scripts the developer iterates on)
 * realign their checksums in {@code flyway_schema_history} on each boot instead
 * of failing validation with a checksum mismatch.
 *
 * <p>Intentionally scoped to the {@code dev} profile only — prod must never
 * silently rewrite history rows.
 */
@Configuration
@Profile("dev")
public class FlywayDevConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
