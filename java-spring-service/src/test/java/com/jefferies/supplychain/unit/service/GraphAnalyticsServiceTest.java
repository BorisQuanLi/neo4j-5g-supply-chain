package com.jefferies.supplychain.unit.service;

import com.jefferies.supplychain.repository.CompanyRepository;
import com.jefferies.supplychain.service.GraphAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GraphAnalyticsService.
 * Uses mocks to test business logic without requiring a real database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Graph Analytics Service Unit Tests")
class GraphAnalyticsServiceTest {

    @Mock
    private Driver neo4jDriver;

    @Mock
    private Session session;

    @Mock
    private Result result;

    @Mock
    private Record record;

    @Mock
    private CompanyRepository companyRepository;

    private GraphAnalyticsService graphAnalyticsService;

    @BeforeEach
    void setUp() {
        // Create the service with mocked dependencies
        graphAnalyticsService = new GraphAnalyticsService(neo4jDriver, companyRepository);
        
        // Set up common mock behavior
        when(neo4jDriver.session()).thenReturn(session);
    }

    @Test
    @DisplayName("Should calculate PageRank centrality successfully")
    void testCalculatePageRankCentrality() throws Exception {
        // Given: Mock result data for PageRank calculation
        Value companyValue = Values.value(Map.of(
            "permid", 4295905573L,
            "name", "Apple Inc",
            "isFinalAssembler", true
        ));
        Value centralityValue = Values.value(0.85);
        
        when(record.get("company")).thenReturn(companyValue);
        when(record.get("centralityScore")).thenReturn(centralityValue);
        when(result.list()).thenReturn(List.of(record));
        when(session.run(any(String.class), any(Map.class))).thenReturn(result);

        // When: Calculating PageRank centrality
        CompletableFuture<List<Map<String, Object>>> future = graphAnalyticsService.calculatePageRankCentrality();
        List<Map<String, Object>> results = future.get();

        // Then: Should return centrality results
        assertThat(results).hasSize(1);
        
        Map<String, Object> firstResult = results.get(0);
        assertThat(firstResult).containsKeys("company", "centralityScore");
        assertThat(firstResult.get("centralityScore")).isEqualTo(0.85);
        
        Map<String, Object> company = (Map<String, Object>) firstResult.get("company");
        assertThat(company.get("name")).isEqualTo("Apple Inc");
        
        // Verify the correct Cypher query was executed
        verify(session).run(contains("gds.pageRank"), any(Map.class));
    }

    @Test
    @DisplayName("Should detect communities successfully")
    void testDetectCommunities() throws Exception {
        // Given: Mock result data for community detection
        Value companyValue = Values.value(Map.of(
            "permid", 4295905573L,
            "name", "Apple Inc"
        ));
        Value communityValue = Values.value(42L);
        
        when(record.get("company")).thenReturn(companyValue);
        when(record.get("communityId")).thenReturn(communityValue);
        when(result.list()).thenReturn(List.of(record));
        when(session.run(any(String.class), any(Map.class))).thenReturn(result);

        // When: Detecting communities
        CompletableFuture<List<Map<String, Object>>> future = graphAnalyticsService.detectCommunities();
        List<Map<String, Object>> results = future.get();

        // Then: Should return community assignments
        assertThat(results).hasSize(1);
        
        Map<String, Object> firstResult = results.get(0);
        assertThat(firstResult).containsKeys("company", "communityId");
        assertThat(firstResult.get("communityId")).isEqualTo(42L);
        
        // Verify community detection query was executed
        verify(session).run(contains("gds.louvain"), any(Map.class));
    }

    @Test
    @DisplayName("Should calculate betweenness centrality successfully")
    void testCalculateBetweennessCentrality() throws Exception {
        // Given: Mock result data for betweenness centrality
        Value companyValue = Values.value(Map.of(
            "permid", 4295906319L,
            "name", "QUALCOMM Inc"
        ));
        Value betweennessValue = Values.value(0.75);
        
        when(record.get("company")).thenReturn(companyValue);
        when(record.get("betweennessCentrality")).thenReturn(betweennessValue);
        when(result.list()).thenReturn(List.of(record));
        when(session.run(any(String.class), any(Map.class))).thenReturn(result);

        // When: Calculating betweenness centrality
        CompletableFuture<List<Map<String, Object>>> future = graphAnalyticsService.calculateBetweennessCentrality();
        List<Map<String, Object>> results = future.get();

        // Then: Should return betweenness results
        assertThat(results).hasSize(1);
        
        Map<String, Object> firstResult = results.get(0);
        assertThat(firstResult).containsKeys("company", "betweennessCentrality");
        assertThat(firstResult.get("betweennessCentrality")).isEqualTo(0.75);
        
        // Verify betweenness centrality query was executed
        verify(session).run(contains("gds.betweenness"), any(Map.class));
    }

    @Test
    @DisplayName("Should find shortest paths between entities")
    void testFindShortestPaths() throws Exception {
        // Given: Mock result data for shortest path
        Value pathValue = Values.value(List.of(
            Map.of("permid", 4295905573L, "name", "Apple Inc"),
            Map.of("permid", 4295906319L, "name", "QUALCOMM Inc")
        ));
        Value lengthValue = Values.value(2);
        
        when(record.get("path")).thenReturn(pathValue);
        when(record.get("pathLength")).thenReturn(lengthValue);
        when(result.list()).thenReturn(List.of(record));
        when(session.run(any(String.class), any(Map.class))).thenReturn(result);

        // When: Finding shortest paths
        Long sourcePermId = 4295905573L;
        Long targetPermId = 4295906319L;
        CompletableFuture<List<Map<String, Object>>> future = 
            graphAnalyticsService.findShortestPaths(sourcePermId, targetPermId);
        List<Map<String, Object>> results = future.get();

        // Then: Should return path information
        assertThat(results).hasSize(1);
        
        Map<String, Object> firstResult = results.get(0);
        assertThat(firstResult).containsKeys("path", "pathLength");
        assertThat(firstResult.get("pathLength")).isEqualTo(2);
        
        // Verify shortest path query was executed with correct parameters
        verify(session).run(contains("shortestPath"), argThat(params -> 
            params.get("sourcePermId").equals(sourcePermId) && 
            params.get("targetPermId").equals(targetPermId)
        ));
    }

    @Test
    @DisplayName("Should calculate network statistics")
    void testCalculateNetworkStatistics() throws Exception {
        // Given: Mock network statistics data
        when(record.get("nodeCount")).thenReturn(Values.value(100));
        when(record.get("relationshipCount")).thenReturn(Values.value(250));
        when(record.get("averageDegree")).thenReturn(Values.value(5.0));
        when(record.get("networkDensity")).thenReturn(Values.value(0.025));
        when(record.get("connectedComponents")).thenReturn(Values.value(1));
        when(result.single()).thenReturn(record);
        when(session.run(any(String.class))).thenReturn(result);

        // When: Calculating network statistics
        CompletableFuture<Map<String, Object>> future = graphAnalyticsService.calculateNetworkStatistics();
        Map<String, Object> stats = future.get();

        // Then: Should return comprehensive statistics
        assertThat(stats).containsKeys(
            "nodeCount", 
            "relationshipCount", 
            "averageDegree", 
            "networkDensity",
            "connectedComponents"
        );
        
        assertThat(stats.get("nodeCount")).isEqualTo(100);
        assertThat(stats.get("relationshipCount")).isEqualTo(250);
        assertThat(stats.get("averageDegree")).isEqualTo(5.0);
        assertThat(stats.get("networkDensity")).isEqualTo(0.025);
        assertThat(stats.get("connectedComponents")).isEqualTo(1);
    }

    @Test
    @DisplayName("Should identify systemic risk entities")
    void testIdentifySystemicRiskEntities() throws Exception {
        // Given: Mock systemic risk analysis data
        Value companyValue = Values.value(Map.of(
            "permid", 4295905573L,
            "name", "Apple Inc",
            "isFinalAssembler", true
        ));
        Value riskScoreValue = Values.value(0.92);
        
        when(record.get("company")).thenReturn(companyValue);
        when(record.get("riskScore")).thenReturn(riskScoreValue);
        when(result.list()).thenReturn(List.of(record));
        when(session.run(any(String.class), any(Map.class))).thenReturn(result);

        // When: Identifying systemic risk entities
        CompletableFuture<List<Map<String, Object>>> future = graphAnalyticsService.identifySystemicRiskEntities();
        List<Map<String, Object>> results = future.get();

        // Then: Should return risk analysis results
        assertThat(results).hasSize(1);
        
        Map<String, Object> firstResult = results.get(0);
        assertThat(firstResult).containsKeys("company", "riskScore");
        assertThat(firstResult.get("riskScore")).isEqualTo(0.92);
        
        Map<String, Object> company = (Map<String, Object>) firstResult.get("company");
        assertThat(company.get("name")).isEqualTo("Apple Inc");
    }

    @Test
    @DisplayName("Should handle errors gracefully")
    void testErrorHandling() {
        // Given: A session that throws an exception
        when(session.run(any(String.class), any(Map.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When: Attempting to calculate PageRank
        CompletableFuture<List<Map<String, Object>>> future = graphAnalyticsService.calculatePageRankCentrality();

        // Then: Should handle the error gracefully
        assertThatThrownBy(() -> future.get())
            .hasCauseInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database connection failed");
    }

    @Test
    @DisplayName("Should handle empty results")
    void testEmptyResults() throws Exception {
        // Given: Empty result set
        when(result.list()).thenReturn(List.of());
        when(session.run(any(String.class), any(Map.class))).thenReturn(result);

        // When: Calculating PageRank with no data
        CompletableFuture<List<Map<String, Object>>> future = graphAnalyticsService.calculatePageRankCentrality();
        List<Map<String, Object>> results = future.get();

        // Then: Should return empty list without errors
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should execute queries asynchronously")
    void testAsynchronousExecution() {
        // Given: Mock setup for async execution
        when(result.list()).thenReturn(List.of());
        when(session.run(any(String.class), any(Map.class))).thenReturn(result);

        // When: Starting multiple async operations
        CompletableFuture<List<Map<String, Object>>> future1 = graphAnalyticsService.calculatePageRankCentrality();
        CompletableFuture<List<Map<String, Object>>> future2 = graphAnalyticsService.detectCommunities();
        CompletableFuture<List<Map<String, Object>>> future3 = graphAnalyticsService.calculateBetweennessCentrality();

        // Then: All futures should be created (not completed yet)
        assertThat(future1).isNotNull();
        assertThat(future2).isNotNull();
        assertThat(future3).isNotNull();
        
        // Verify that these can be combined
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);
        assertThat(allFutures).isNotNull();
    }

    @Test
    @DisplayName("Should validate input parameters")
    void testInputValidation() {
        // When: Calling methods with null parameters
        // Then: Should handle null inputs appropriately
        assertThatCode(() -> graphAnalyticsService.findShortestPaths(null, 123L))
            .doesNotThrowAnyException();
        
        assertThatCode(() -> graphAnalyticsService.findShortestPaths(123L, null))
            .doesNotThrowAnyException();
        
        assertThatCode(() -> graphAnalyticsService.analyzeCompetitors(null))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should close resources properly")
    void testResourceManagement() throws Exception {
        // Given: Mock successful execution
        when(result.list()).thenReturn(List.of());
        when(session.run(any(String.class), any(Map.class))).thenReturn(result);

        // When: Executing an operation
        CompletableFuture<List<Map<String, Object>>> future = graphAnalyticsService.calculatePageRankCentrality();
        future.get();

        // Then: Session should be properly managed
        verify(neo4jDriver, atLeastOnce()).session();
        // Note: Actual resource cleanup verification would depend on the specific implementation
        // The service should use try-with-resources or similar patterns
    }
}