package com.jefferies.supplychain.integration;

import com.jefferies.supplychain.TestConfiguration;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests.
 * Provides common configuration and setup for tests that require a full Spring context
 * and a real Neo4j database via TestContainers.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestConfiguration.class
)
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {
    
    /**
     * Common test data setup method.
     * Subclasses can override this to set up specific test data.
     */
    protected void setupTestData() {
        // Base implementation - can be overridden by subclasses
    }
    
    /**
     * Common test data cleanup method.
     * Subclasses can override this to clean up specific test data.
     */
    protected void cleanupTestData() {
        // Base implementation - can be overridden by subclasses
    }
}