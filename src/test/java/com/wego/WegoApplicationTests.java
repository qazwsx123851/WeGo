package com.wego;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for application context loading.
 *
 * @contract
 *   - pre: Test profile is active with H2 database
 *   - post: Spring application context loads successfully
 */
@SpringBootTest
@ActiveProfiles("test")
class WegoApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context loads without errors
    }
}
