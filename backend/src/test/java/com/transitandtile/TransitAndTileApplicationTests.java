package com.transitandtile;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: verifies that the Spring application context loads successfully.
 *
 * @TestPropertySource overrides the datasource to avoid requiring a live MySQL
 * connection during the standard Maven build.  The override uses an in-memory
 * H2 datasource ONLY for this context-load check.
 *
 * IMPORTANT: H2 cannot prove MySQL/InnoDB locking semantics. All MySQL-specific
 * integration tests (overlap queries, pessimistic locking, concurrent bookings)
 * must run against a real MySQL instance as described in the testing plan.
 *
 * To run the full context test against MySQL, remove @TestPropertySource and
 * supply the DB_* environment variables as documented in README.md.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.sql.init.mode=never",
    "DB_USERNAME=sa",
    "DB_PASSWORD="
})
class TransitAndTileApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, the Spring application context starts cleanly.
        // No business logic is tested here — that happens in phase-specific tests.
    }
}
