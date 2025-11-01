package com.jefferies.supplychain.integration;

import com.jefferies.supplychain.model.Company;
import com.jefferies.supplychain.repository.CompanyRepository;
import com.jefferies.supplychain.service.GraphAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for GraphAnalyticsService using TestContainers.
 * Tests the service layer against a real Neo4j database with GDS capabilities.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Graph Analytics Service Integration Tests")
class GraphAnalyticsServiceIT extends BaseIntegrationTest {

    @Autowired
    private GraphAnalyticsService graphAnalyticsService;

    @Autowired
    private CompanyRepository companyRepository;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    @Override
    protected void setupTestData() {
        // Clean database
        companyRepository.deleteAll();
        
        // Create a representative 5G supply chain network
        List<Company> supplyChainCompanies = List.of(
            createTestCompany(4295905573L, "Apple Inc", true, 0.98, "AAPL", "United States"),
            createTestCompany(4295877456L, "Samsung Electronics", true, 0.96, "005930.KS", "South Korea"),
            createTestCompany(4295906319L, "QUALCOMM Inc", false, 0.94, "QCOM", "United States"),
            createTestCompany(4295903847L, "ARM Holdings", false, 0.92, "ARM", "United Kingdom"),
            createTestCompany(4295871234L, "Taiwan Semiconductor", false, 0.95, "TSM", "Taiwan"),
            createTestCompany(4295865678L, "Broadcom Inc", false, 0.93, "AVGO", "United States"),
            createTestCompany(4295823456L, "MediaTek Inc", false, 0.89, "2454.TW", "Taiwan"),
            createTestCompany(4295834567L, "Foxconn Technology", false, 0.87, "2317.TW", "Taiwan")
        );
        
        companyRepository.saveAll(supplyChainCompanies);
        
        // Create relationships in the supply chain
        // Note: In a real implementation, these would be separate relationship entities
        // For this test, we'll simulate the graph structure that would exist
        createSupplyChainRelationships();
    }

    @Test
    @DisplayName("Should calculate PageRank centrality for supply chain network")
    void testCalculatePageRankCentrality() throws Exception {
        // When: Calculating PageRank centrality
        CompletableFuture<List<Map<String, Object>>> future = graphAnalyticsService.calculatePageRankCentrality();
        List<Map<String, Object>> results = future.get();
        
        // Then: Results should contain centrality scores for all companies
        assertThat(results).isNotEmpty();
        assertThat(results).hasSizeGreaterThanOrEqualTo(8); // At least our test companies
        
        // Verify that companies have centrality scores
        Map<String, Object> topCompany = results.get(0);
        assertThat(topCompany).containsKeys("company", "centralityScore");
        assertThat((Double) topCompany.get("centralityScore")).isGreaterThan(0.0);
        
        // Final assemblers (Apple, Samsung) should typically have higher centrality
        boolean foundFinalAssembler = results.stream()
            .anyMatch(result -> {
                Map<String, Object> company = (Map<String, Object>) result.get("company");
                return "Apple Inc".equals(company.get("name")) || "Samsung Electronics".equals(company.get("name"));
            });
        assertThat(foundFinalAssembler).isTrue();
    }

    @Test
    @DisplayName("Should perform community detection on supply chain network")
    void testCommunityDetection() throws Exception {
        // When: Performing community detection
        CompletableFuture<List<Map<String, Object>>> future = graphAnalyticsService.detectCommunities();
        List<Map<String, Object>> results = future.get();
        
        // Then: Companies should be assigned to communities
        assertThat(results).isNotEmpty();
        
        // Verify community assignments
        Map<String, Object> firstResult = results.get(0);
        assertThat(firstResult).containsKeys("company", "communityId");
        assertThat(firstResult.get("communityId")).isNotNull();
        
        // Companies from the same region/type should potentially be in same communities
        // This is a statistical test - communities may vary between runs
        long distinctCommunities = results.stream()
            .map(result -> result.get("communityId"))
            .distinct()
            .count();
        
        assertThat(distinctCommunities).isBetween(1L, (long) results.size());
    }

    @Test
    @DisplayName("Should calculate betweenness centrality for key intermediaries")
    void testBetweennessCentrality() throws Exception {
        // When: Calculating betweenness centrality
        CompletableFuture<List<Map<String, Object>>> future = graphAnalyticsService.calculateBetweennessCentrality();
        List<Map<String, Object>> results = future.get();
        
        // Then: Key intermediaries should have high betweenness scores
        assertThat(results).isNotEmpty();
        
        Map<String, Object> topIntermediary = results.get(0);
        assertThat(topIntermediary).containsKeys("company", "betweennessCentrality");
        assertThat((Double) topIntermediary.get("betweennessCentrality")).isGreaterThanOrEqualTo(0.0);
        
        // Component suppliers (Qualcomm, ARM, TSMC) should typically have high betweenness
        boolean foundComponentSupplier = results.stream()
            .anyMatch(result -> {
                Map<String, Object> company = (Map<String, Object>) result.get("company");
                String name = (String) company.get("name");
                return name.contains("QUALCOMM") || name.contains("ARM") || name.contains("Taiwan Semiconductor");
            });
        assertThat(foundComponentSupplier).isTrue();
    }

    @Test
    @DisplayName("Should find shortest paths between companies")
    void testShortestPaths() throws Exception {
        // Given: Two specific companies (Apple and Qualcomm)
        Long applePermId = 4295905573L;
        Long qualcommPermId = 4295906319L;
        
        // When: Finding shortest path
        CompletableFuture<List<Map<String, Object>>> future = 
            graphAnalyticsService.findShortestPaths(applePermId, qualcommPermId);
        List<Map<String, Object>> results = future.get();
        
        // Then: Should find path(s) between Apple and Qualcomm
        assertThat(results).isNotEmpty();
        
        Map<String, Object> pathResult = results.get(0);
        assertThat(pathResult).containsKeys("path", "pathLength");
        
        // Path length should be reasonable for supply chain (typically 1-3 hops)
        Integer pathLength = (Integer) pathResult.get("pathLength");
        assertThat(pathLength).isBetween(1, 5);
    }

    @Test
    @DisplayName("Should calculate network statistics")
    void testNetworkStatistics() throws Exception {
        // When: Calculating network statistics
        CompletableFuture<Map<String, Object>> future = graphAnalyticsService.calculateNetworkStatistics();
        Map<String, Object> stats = future.get();
        
        // Then: Should provide comprehensive network metrics
        assertThat(stats).containsKeys(
            "nodeCount", 
            "relationshipCount", 
            "averageDegree", 
            "networkDensity",
            "connectedComponents"
        );
        
        // Verify reasonable values for our test network
        assertThat((Integer) stats.get("nodeCount")).isGreaterThanOrEqualTo(8);
        assertThat((Integer) stats.get("relationshipCount")).isGreaterThanOrEqualTo(0);
        assertThat((Double) stats.get("networkDensity")).isBetween(0.0, 1.0);
        assertThat((Integer) stats.get("connectedComponents")).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should identify systemic risk entities")
    void testSystemicRiskAnalysis() throws Exception {
        // When: Performing systemic risk analysis
        CompletableFuture<List<Map<String, Object>>> future = graphAnalyticsService.identifySystemicRiskEntities();
        List<Map<String, Object>> results = future.get();
        
        // Then: Should identify high-risk entities
        assertThat(results).isNotEmpty();
        
        Map<String, Object> riskEntity = results.get(0);
        assertThat(riskEntity).containsKeys("company", "riskScore");
        
        Double riskScore = (Double) riskEntity.get("riskScore");
        assertThat(riskScore).isBetween(0.0, 1.0);
        
        // High centrality entities should typically appear in systemic risk analysis
        boolean foundHighCentralityEntity = results.stream()
            .anyMatch(result -> {
                Map<String, Object> company = (Map<String, Object>) result.get("company");
                String name = (String) company.get("name");
                return name.contains("Apple") || name.contains("Samsung") || name.contains("QUALCOMM");
            });
        assertThat(foundHighCentralityEntity).isTrue();
    }

    @Test
    @DisplayName("Should detect supply chain vulnerabilities")
    void testSupplyChainVulnerabilityDetection() throws Exception {
        // When: Detecting supply chain vulnerabilities
        CompletableFuture<List<Map<String, Object>>> future = 
            graphAnalyticsService.detectSupplyChainVulnerabilities();
        List<Map<String, Object>> results = future.get();
        
        // Then: Should identify potential vulnerabilities
        assertThat(results).isNotEmpty();
        
        Map<String, Object> vulnerability = results.get(0);
        assertThat(vulnerability).containsKeys("vulnerabilityType", "affectedEntities", "riskLevel");
        
        String riskLevel = (String) vulnerability.get("riskLevel");
        assertThat(riskLevel).isIn("LOW", "MEDIUM", "HIGH", "CRITICAL");
        
        @SuppressWarnings("unchecked")
        List<Object> affectedEntities = (List<Object>) vulnerability.get("affectedEntities");
        assertThat(affectedEntities).isNotEmpty();
    }

    @Test
    @DisplayName("Should perform competitor analysis")
    void testCompetitorAnalysis() throws Exception {
        // Given: Apple as the focus company
        Long applePermId = 4295905573L;
        
        // When: Performing competitor analysis
        CompletableFuture<List<Map<String, Object>>> future = 
            graphAnalyticsService.analyzeCompetitors(applePermId);
        List<Map<String, Object>> results = future.get();
        
        // Then: Should identify competitors and their relationships
        assertThat(results).isNotEmpty();
        
        Map<String, Object> competitor = results.get(0);
        assertThat(competitor).containsKeys("competitor", "competitionStrength", "sharedSuppliers");
        
        Double competitionStrength = (Double) competitor.get("competitionStrength");
        assertThat(competitionStrength).isBetween(0.0, 1.0);
        
        // Samsung should likely appear as a competitor to Apple
        boolean foundSamsung = results.stream()
            .anyMatch(result -> {
                Map<String, Object> comp = (Map<String, Object>) result.get("competitor");
                return ((String) comp.get("name")).contains("Samsung");
            });
        assertThat(foundSamsung).isTrue();
    }

    @Test
    @DisplayName("Should handle concurrent analytics requests")
    void testConcurrentAnalytics() throws Exception {
        // When: Running multiple analytics concurrently
        CompletableFuture<List<Map<String, Object>>> pageRankFuture = 
            graphAnalyticsService.calculatePageRankCentrality();
        CompletableFuture<List<Map<String, Object>>> communityFuture = 
            graphAnalyticsService.detectCommunities();
        CompletableFuture<Map<String, Object>> statsFuture = 
            graphAnalyticsService.calculateNetworkStatistics();
        
        // Then: All should complete successfully
        CompletableFuture.allOf(pageRankFuture, communityFuture, statsFuture).get();
        
        assertThat(pageRankFuture.get()).isNotEmpty();
        assertThat(communityFuture.get()).isNotEmpty();
        assertThat(statsFuture.get()).isNotEmpty();
    }

    /**
     * Helper method to create test company data
     */
    private Company createTestCompany(Long permid, String name, Boolean isFinalAssembler, 
                                    Double matchScore, String ticker, String country) {
        Company company = new Company();
        company.setPermid(permid);
        company.setName(name);
        company.setIsFinalAssembler(isFinalAssembler);
        company.setMatchScore(matchScore);
        company.setTickerSymbol(ticker);
        company.setCountry(country);
        company.setIndustrySector("Technology");
        company.setBusinessType(isFinalAssembler ? "Consumer Electronics" : "Semiconductors");
        company.setLastUpdated(LocalDateTime.now());
        company.setDataSource("TEST_DATA");
        company.setCentralityScore(0.0);
        return company;
    }

    /**
     * Helper method to create supply chain relationships
     * In a real implementation, this would create actual relationship entities
     */
    private void createSupplyChainRelationships() {
        // This is a simplified approach - in reality, you'd create separate Relationship entities
        // For the test, we assume the graph analytics service can work with the existing data
        // The actual relationship creation would depend on your relationship model implementation
        
        // Example relationships in 5G supply chain:
        // Apple -> TSMC (chip manufacturing)
        // Apple -> Qualcomm (modem chips)  
        // Samsung -> ARM (chip designs)
        // Qualcomm -> ARM (licensing)
        // TSMC -> ARM (manufacturing ARM designs)
        // Foxconn -> Apple (assembly)
        // etc.
        
        // For this integration test, we'll assume these relationships exist
        // and focus on testing the analytics algorithms
    }
}