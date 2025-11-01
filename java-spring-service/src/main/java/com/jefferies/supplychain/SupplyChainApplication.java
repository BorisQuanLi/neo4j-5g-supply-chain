package com.jefferies.supplychain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot Application for the Supply Chain Graph Analytics Service
 * 
 * This application provides REST APIs for graph-based analytics on supply chain data,
 * demonstrating Neo4j integration patterns suitable for financial services use cases
 * at Jefferies Securities.
 * 
 * Key Features:
 * - Neo4j graph database integration with Spring Data Neo4j
 * - Graph analytics endpoints (pathfinding, centrality, community detection)
 * - Async processing for long-running graph algorithms
 * - Integration points for MCP (Model Context Protocol) agents
 * - Production-ready monitoring and observability
 * 
 * @author Graph Data Engineer Team
 * @version 1.0.0
 * @since 2025-10-31
 */
@SpringBootApplication
@EnableNeo4jRepositories(basePackages = "com.jefferies.supplychain.repository")
@EnableAsync
@EnableTransactionManagement
public class SupplyChainApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupplyChainApplication.class, args);
    }
}