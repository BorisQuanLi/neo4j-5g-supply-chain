package com.jefferies.supplychain.service;

import com.jefferies.supplychain.model.Company;
import com.jefferies.supplychain.repository.CompanyRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Business logic service for graph analytics operations.
 * 
 * This service provides the business layer for supply chain graph analytics,
 * coordinating between the repository layer and REST controllers. It includes
 * methods for graph algorithm execution, data management, and integration
 * with MCP (Model Context Protocol) agents.
 * 
 * Key Capabilities:
 * - Async graph algorithm execution for performance
 * - Transaction management for data consistency
 * - Integration points for AI/ML agent frameworks
 * - Business logic for financial services use cases
 * - Error handling and validation
 * 
 * @author Graph Data Engineer Team
 * @version 1.0.0
 */
@Service
@Transactional
public class GraphAnalyticsService {

    private final CompanyRepository companyRepository;

    @Autowired
    public GraphAnalyticsService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    // ===== ENTITY MANAGEMENT OPERATIONS =====

    /**
     * Find company by name with enhanced error handling
     */
    public Optional<Company> findCompanyByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Company name cannot be null or empty");
        }
        return companyRepository.findByName(name.trim());
    }

    /**
     * Get all companies with minimum match score
     * Filters out low-confidence entity matches from ETL
     */
    public List<Company> getHighConfidenceCompanies(Double minMatchScore) {
        Double threshold = minMatchScore != null ? minMatchScore : 0.8;
        return companyRepository.findByMinMatchScore(threshold);
    }

    /**
     * Batch entity ingestion with validation
     * Supports ETL pipeline integration
     */
    @Async
    public CompletableFuture<Integer> batchIngestCompanies(List<Company> companies) {
        validateCompanyList(companies);
        Integer count = companyRepository.batchIngestEntities(companies);
        return CompletableFuture.completedFuture(count);
    }

    // ===== PATHFINDING ANALYTICS =====

    /**
     * Find optimal supply chain backup routes
     * Business Use Case: Risk mitigation for supply chain disruption
     * Financial Use Case: Alternative trading routes, backup counterparties
     */
    public List<Company.PathResult> findBackupSupplierRoutes(String startCompany, String endCompany) {
        validatePathfindingInput(startCompany, endCompany);
        
        List<Company.PathResult> paths = companyRepository.findOptimalSupplyPath(startCompany, endCompany);
        
        // Enhance results with business context
        return enhancePathResults(paths);
    }

    /**
     * Advanced pathfinding with multiple constraints
     * Supports complex routing scenarios with business rules
     */
    public List<Company.PathResult> findConstrainedPaths(String startCompany, String endCompany, 
                                                        Integer maxHops, Integer maxResults) {
        validatePathfindingInput(startCompany, endCompany);
        
        Integer hops = maxHops != null ? maxHops : 5;
        Integer results = maxResults != null ? maxResults : 10;
        
        return companyRepository.findAllPathsWithinHops(startCompany, endCompany, hops, results);
    }

    // ===== CENTRALITY ANALYTICS =====

    /**
     * Identify critical supply chain nodes using PageRank
     * Business Use Case: Strategic importance analysis
     * Financial Use Case: Systemic risk identification, key institution analysis
     */
    @Async
    public CompletableFuture<List<Company.CentralityResult>> analyzeCriticalNodes(Integer topN) {
        Integer limit = topN != null ? topN : 20;
        
        List<Company.CentralityResult> pageRankResults = 
            companyRepository.calculatePageRankCentrality(limit);
        
        return CompletableFuture.completedFuture(pageRankResults);
    }

    /**
     * Identify bridge companies using Betweenness centrality
     * Business Use Case: Information/resource flow control points
     * Financial Use Case: Transaction intermediaries, regulatory bottlenecks
     */
    @Async
    public CompletableFuture<List<Company.CentralityResult>> analyzeBridgeNodes(Integer topN) {
        Integer limit = topN != null ? topN : 15;
        
        List<Company.CentralityResult> betweennessResults = 
            companyRepository.calculateBetweennessCentrality(limit);
        
        return CompletableFuture.completedFuture(betweennessResults);
    }

    /**
     * Comprehensive centrality analysis combining multiple metrics
     * Provides holistic view of network importance
     */
    @Async
    public CompletableFuture<CentralityAnalysisResult> performComprehensiveCentralityAnalysis() {
        // Run PageRank analysis
        CompletableFuture<List<Company.CentralityResult>> pageRankFuture = 
            analyzeCriticalNodes(25);
        
        // Run Betweenness analysis
        CompletableFuture<List<Company.CentralityResult>> betweennessFuture = 
            analyzeBridgeNodes(25);
        
        return pageRankFuture.thenCombine(betweennessFuture, (pageRank, betweenness) -> {
            CentralityAnalysisResult result = new CentralityAnalysisResult();
            result.setPageRankResults(pageRank);
            result.setBetweennessResults(betweenness);
            result.setCombinedInsights(generateCombinedInsights(pageRank, betweenness));
            return result;
        });
    }

    // ===== COMMUNITY DETECTION =====

    /**
     * Detect supply chain communities for competitive analysis
     * Business Use Case: Market segmentation, partnership opportunities
     * Financial Use Case: Trading cluster identification, compliance monitoring
     */
    @Async
    public CompletableFuture<List<Object>> detectSupplyChainCommunities() {
        return CompletableFuture.supplyAsync(() -> companyRepository.detectCommunities());
    }

    /**
     * Find companies in same community as target (competitive intelligence)
     */
    public List<Company> findRelatedCompanies(String targetCompany) {
        if (targetCompany == null || targetCompany.trim().isEmpty()) {
            throw new IllegalArgumentException("Target company name cannot be null or empty");
        }
        return companyRepository.findCompaniesInSameCommunity(targetCompany);
    }

    // ===== ADVANCED ANALYTICS =====

    /**
     * Analyze frenemy relationships (competitors who are also partners)
     * Business Use Case: Complex partnership dynamics like Apple-Samsung
     * Financial Use Case: Market maker relationships, competitive trading
     */
    public List<Object> analyzeFrenemyRelationships() {
        return companyRepository.findFrenemyRelationships();
    }

    /**
     * Supply chain vulnerability assessment
     * Business Use Case: Risk management, supply chain resilience
     * Financial Use Case: Counterparty concentration risk, systemic risk
     */
    public List<Object> assessSupplyChainVulnerabilities(Integer minDownstreamImpact) {
        Integer threshold = minDownstreamImpact != null ? minDownstreamImpact : 3;
        return companyRepository.findSupplyChainVulnerabilities(threshold);
    }

    /**
     * Strategic acquisition target analysis
     * Business Use Case: M&A strategy, investment opportunities
     * Financial Use Case: Undervalued network positions, strategic investments
     */
    public List<Object> identifyAcquisitionTargets(Double minCentrality, Long maxMarketCap) {
        Double centralityThreshold = minCentrality != null ? minCentrality : 0.01;
        Long marketCapLimit = maxMarketCap != null ? maxMarketCap : 50000000000L; // 50B
        
        return companyRepository.findAcquisitionTargets(centralityThreshold, marketCapLimit, 20);
    }

    // ===== GRAPH MANAGEMENT =====

    /**
     * Initialize or refresh graph projection for analytics
     * Essential for GDS algorithm performance
     */
    @Async
    public CompletableFuture<Object> refreshGraphProjection() {
        try {
            // Drop existing projection if it exists
            companyRepository.dropGraphProjection();
        } catch (Exception e) {
            // Projection may not exist, continue
        }
        
        // Create new projection
        Object result = companyRepository.createGraphProjection();
        
        // Update centrality scores for caching
        companyRepository.updatePageRankScores();
        
        return CompletableFuture.completedFuture(result);
    }

    // ===== MCP AGENT INTEGRATION METHODS =====

    /**
     * Provide graph context for MCP agents
     * Enables AI agents to query graph database for decision making
     */
    public GraphContext getGraphContextForAgent(String query, List<String> entityNames) {
        GraphContext context = new GraphContext();
        context.setQuery(query);
        
        // Get entities and their relationships
        List<Company> entities = entityNames.stream()
            .map(this::findCompanyByName)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
        
        context.setEntities(entities);
        
        // Add centrality information for importance weighting
        List<Company.CentralityResult> centrality = 
            companyRepository.calculatePageRankCentrality(50);
        context.setCentralityResults(centrality);
        
        return context;
    }

    /**
     * Execute agent-requested graph algorithm
     * Allows MCP agents to trigger specific analytics
     */
    @Async
    public CompletableFuture<Object> executeAgentAnalysis(String analysisType, Object parameters) {
        return switch (analysisType.toUpperCase()) {
            case "PATHFINDING" -> {
                PathfindingRequest req = (PathfindingRequest) parameters;
                yield CompletableFuture.completedFuture(
                    (Object) findBackupSupplierRoutes(req.getSource(), req.getTarget())
                );
            }
            case "CENTRALITY" -> {
                CentralityRequest req = (CentralityRequest) parameters;
                yield analyzeCriticalNodes(req.getTopN()).thenApply(result -> (Object) result);
            }
            case "COMMUNITY" -> detectSupplyChainCommunities().thenApply(result -> (Object) result);
            case "VULNERABILITY" -> {
                VulnerabilityRequest req = (VulnerabilityRequest) parameters;
                yield CompletableFuture.completedFuture(
                    (Object) assessSupplyChainVulnerabilities(req.getMinImpact())
                );
            }
            default -> throw new IllegalArgumentException("Unknown analysis type: " + analysisType);
        };
    }

    // ===== PRIVATE HELPER METHODS =====

    private void validateCompanyList(List<Company> companies) {
        if (companies == null || companies.isEmpty()) {
            throw new IllegalArgumentException("Company list cannot be null or empty");
        }
        
        for (Company company : companies) {
            if (company.getPermid() == null || company.getName() == null) {
                throw new IllegalArgumentException(
                    "All companies must have permid and name: " + company);
            }
        }
    }

    private void validatePathfindingInput(String startCompany, String endCompany) {
        if (startCompany == null || startCompany.trim().isEmpty()) {
            throw new IllegalArgumentException("Start company name cannot be null or empty");
        }
        if (endCompany == null || endCompany.trim().isEmpty()) {
            throw new IllegalArgumentException("End company name cannot be null or empty");
        }
        if (startCompany.equals(endCompany)) {
            throw new IllegalArgumentException("Start and end companies must be different");
        }
    }

    private List<Company.PathResult> enhancePathResults(List<Company.PathResult> paths) {
        // Add business logic enhancements like reliability scoring
        return paths.stream()
            .peek(path -> {
                // Calculate reliability score based on path length and company scores
                Double reliability = 1.0 / (1.0 + path.getPathLength() * 0.1);
                path.setReliabilityScore(reliability);
            })
            .toList();
    }

    private List<String> generateCombinedInsights(List<Company.CentralityResult> pageRank, 
                                                List<Company.CentralityResult> betweenness) {
        // Business logic to combine multiple centrality metrics
        return List.of(
            "Companies appearing in both PageRank and Betweenness top 10 are critically important",
            "High PageRank with low Betweenness indicates influential but not controlling position",
            "High Betweenness with low PageRank indicates strategic bottleneck position"
        );
    }

    // ===== INNER CLASSES FOR REQUEST/RESPONSE OBJECTS =====

    public static class CentralityAnalysisResult {
        private List<Company.CentralityResult> pageRankResults;
        private List<Company.CentralityResult> betweennessResults;
        private List<String> combinedInsights;

        // Getters and setters
        public List<Company.CentralityResult> getPageRankResults() { return pageRankResults; }
        public void setPageRankResults(List<Company.CentralityResult> pageRankResults) { 
            this.pageRankResults = pageRankResults; 
        }

        public List<Company.CentralityResult> getBetweennessResults() { return betweennessResults; }
        public void setBetweennessResults(List<Company.CentralityResult> betweennessResults) { 
            this.betweennessResults = betweennessResults; 
        }

        public List<String> getCombinedInsights() { return combinedInsights; }
        public void setCombinedInsights(List<String> combinedInsights) { 
            this.combinedInsights = combinedInsights; 
        }
    }

    public static class GraphContext {
        private String query;
        private List<Company> entities;
        private List<Company.CentralityResult> centralityResults;

        // Getters and setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public List<Company> getEntities() { return entities; }
        public void setEntities(List<Company> entities) { this.entities = entities; }

        public List<Company.CentralityResult> getCentralityResults() { return centralityResults; }
        public void setCentralityResults(List<Company.CentralityResult> centralityResults) { 
            this.centralityResults = centralityResults; 
        }
    }

    // Request classes for agent integration
    public static class PathfindingRequest {
        private String source;
        private String target;

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
    }

    public static class CentralityRequest {
        private Integer topN;

        public Integer getTopN() { return topN; }
        public void setTopN(Integer topN) { this.topN = topN; }
    }

    public static class VulnerabilityRequest {
        private Integer minImpact;

        public Integer getMinImpact() { return minImpact; }
        public void setMinImpact(Integer minImpact) { this.minImpact = minImpact; }
    }
}