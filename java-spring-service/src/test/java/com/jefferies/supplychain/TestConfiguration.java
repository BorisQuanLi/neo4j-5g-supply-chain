package com.jefferies.supplychain;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration for setting up TestContainers and shared test infrastructure.
 * This configuration provides a shared Neo4j container for all integration tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestConfiguration {
    
    /**
     * Shared Neo4j container for all integration tests.
     * Using @ServiceConnection automatically configures Spring Boot's Neo4j properties.
     */
    @Container
    @ServiceConnection
    @Bean
    static Neo4jContainer<?> neo4jContainer() {
        return new Neo4jContainer<>(DockerImageName.parse("neo4j:5.15.0-enterprise"))
                .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
                .withEnv("NEO4J_AUTH", "neo4j/testpassword")
                .withEnv("NEO4J_PLUGINS", "[\"graph-data-science\"]")
                .withEnv("NEO4J_dbms_security_procedures_unrestricted", "gds.*")
                .withEnv("NEO4J_dbms_security_procedures_allowlist", "gds.*")
                .withReuse(true); // Reuse container across test runs for faster execution
    }
}