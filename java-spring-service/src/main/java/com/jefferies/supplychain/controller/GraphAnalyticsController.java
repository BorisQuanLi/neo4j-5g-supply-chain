package com.jefferies.supplychain.controller;

import com.jefferies.supplychain.model.Company;
import com.jefferies.supplychain.service.GraphAnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * REST API Controller for Supply Chain Graph Analytics
 * 
 * This controller provides REST endpoints for graph-based analytics on supply chain data,
 * demonstrating the capabilities required for a Graph Data Engineer role at Jefferies Securities.
 * The APIs support both direct business use cases and integration with MCP (Model Context Protocol) agents.
 * 
 * Key Features:
 * - Pathfinding algorithms for backup supplier analysis
 * - Centrality analysis for critical node identification  
 * - Community detection for market segmentation
 * - Complex relationship analysis (frenemy dynamics)
 * - Vulnerability assessment for risk management
 * - Async processing for performance at scale
 * - Integration points for AI/ML agent frameworks
 * 
 * @author Graph Data Engineer Team
 * @version 1.0.0
 * @since 2025-10-31
 */
@RestController
@RequestMapping("/api/v1/graph-analytics")
@Tag(name = "Graph Analytics", description = "Supply Chain Graph Analytics APIs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GraphAnalyticsController {

    private final GraphAnalyticsService graphAnalyticsService;

    @Autowired
    public GraphAnalyticsController(GraphAnalyticsService graphAnalyticsService) {
        this.graphAnalyticsService = graphAnalyticsService;
    }

    // ===== HEALTH AND STATUS ENDPOINTS =====

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint", description = "Verify service availability")
    public ResponseEntity<Object> healthCheck() {
        return ResponseEntity.ok().body(new HealthResponse("Graph Analytics Service is running", 
                                                          System.currentTimeMillis()));
    }

    @PostMapping("/graph/refresh")
    @Operation(summary = "Refresh graph projection", 
               description = "Rebuild graph projection for optimal analytics performance")
    public CompletableFuture<ResponseEntity<Object>> refreshGraphProjection() {
        return graphAnalyticsService.refreshGraphProjection()
            .thenApply(result -> ResponseEntity.ok().body(result));
    }

    // ===== ENTITY MANAGEMENT ENDPOINTS =====

    @GetMapping("/companies/{name}")
    @Operation(summary = "Find company by name", description = "Retrieve company information by exact name match")
    public ResponseEntity<Company> getCompanyByName(
            @Parameter(description = "Company name", example = "Apple Inc")
            @PathVariable String name) {
        
        Optional<Company> company = graphAnalyticsService.findCompanyByName(name);
        return company.map(c -> ResponseEntity.ok().body(c))
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/companies")
    @Operation(summary = "Get high-confidence companies", 
               description = "Retrieve companies with match score above threshold")
    public ResponseEntity<List<Company>> getHighConfidenceCompanies(
            @Parameter(description = "Minimum match score", example = "0.9")
            @RequestParam(defaultValue = "0.8") Double minMatchScore) {
        
        List<Company> companies = graphAnalyticsService.getHighConfidenceCompanies(minMatchScore);
        return ResponseEntity.ok().body(companies);
    }

    @PostMapping("/companies/batch")
    @Operation(summary = "Batch ingest companies", 
               description = "Ingest multiple companies for ETL pipeline integration")
    public CompletableFuture<ResponseEntity<BatchIngestResponse>> batchIngestCompanies(
            @RequestBody List<Company> companies) {
        
        return graphAnalyticsService.batchIngestCompanies(companies)
            .thenApply(count -> ResponseEntity.ok().body(
                new BatchIngestResponse(count, "Successfully ingested companies")));
    }

    // ===== PATHFINDING ANALYTICS ENDPOINTS =====

    @GetMapping("/pathfinding/backup-supplier")
    @Operation(summary = "Find backup supplier routes", 
               description = "Discover optimal alternative supply paths for risk mitigation")
    public ResponseEntity<List<Company.PathResult>> findBackupSupplierRoutes(
            @Parameter(description = "Start company name", example = "Apple Inc")
            @RequestParam String startCompany,
            @Parameter(description = "End company name", example = "MediaTek")
            @RequestParam String endCompany) {
        
        List<Company.PathResult> paths = graphAnalyticsService.findBackupSupplierRoutes(startCompany, endCompany);
        return ResponseEntity.ok().body(paths);
    }

    @GetMapping("/pathfinding/constrained")
    @Operation(summary = "Find paths with constraints", 
               description = "Discover paths within hop limits and result constraints")
    public ResponseEntity<List<Company.PathResult>> findConstrainedPaths(
            @Parameter(description = "Start company name", example = "Apple Inc")
            @RequestParam String startCompany,
            @Parameter(description = "End company name", example = "Samsung Electronics Co")
            @RequestParam String endCompany,
            @Parameter(description = "Maximum hops allowed", example = "4")
            @RequestParam(defaultValue = "5") Integer maxHops,
            @Parameter(description = "Maximum results to return", example = "10")
            @RequestParam(defaultValue = "10") Integer maxResults) {
        
        List<Company.PathResult> paths = graphAnalyticsService.findConstrainedPaths(
            startCompany, endCompany, maxHops, maxResults);
        return ResponseEntity.ok().body(paths);
    }

    // ===== CENTRALITY ANALYSIS ENDPOINTS =====

    @GetMapping("/centrality/critical-nodes")
    @Operation(summary = "Identify critical nodes", 
               description = "Find most influential companies using PageRank centrality")
    public CompletableFuture<ResponseEntity<List<Company.CentralityResult>>> analyzeCriticalNodes(
            @Parameter(description = "Number of top results", example = "20")
            @RequestParam(defaultValue = "20") Integer topN) {
        
        return graphAnalyticsService.analyzeCriticalNodes(topN)
            .thenApply(results -> ResponseEntity.ok().body(results));
    }

    @GetMapping("/centrality/bridge-nodes")
    @Operation(summary = "Identify bridge nodes", 
               description = "Find companies that control information/resource flow using Betweenness centrality")
    public CompletableFuture<ResponseEntity<List<Company.CentralityResult>>> analyzeBridgeNodes(
            @Parameter(description = "Number of top results", example = "15")
            @RequestParam(defaultValue = "15") Integer topN) {
        
        return graphAnalyticsService.analyzeBridgeNodes(topN)
            .thenApply(results -> ResponseEntity.ok().body(results));
    }

    @GetMapping("/centrality/comprehensive")
    @Operation(summary = "Comprehensive centrality analysis", 
               description = "Combined PageRank and Betweenness analysis with insights")
    public CompletableFuture<ResponseEntity<GraphAnalyticsService.CentralityAnalysisResult>> 
        performComprehensiveCentralityAnalysis() {
        
        return graphAnalyticsService.performComprehensiveCentralityAnalysis()
            .thenApply(result -> ResponseEntity.ok().body(result));
    }

    // ===== COMMUNITY DETECTION ENDPOINTS =====

    @GetMapping("/communities/detect")
    @Operation(summary = "Detect supply chain communities", 
               description = "Identify clusters of closely related companies using Louvain algorithm")
    public CompletableFuture<ResponseEntity<List<Object>>> detectSupplyChainCommunities() {
        return graphAnalyticsService.detectSupplyChainCommunities()
            .thenApply(communities -> ResponseEntity.ok().body(communities));
    }

    @GetMapping("/communities/related/{targetCompany}")
    @Operation(summary = "Find related companies", 
               description = "Get companies in the same community as target company")
    public ResponseEntity<List<Company>> findRelatedCompanies(
            @Parameter(description = "Target company name", example = "ARM Holdings")
            @PathVariable String targetCompany) {
        
        List<Company> relatedCompanies = graphAnalyticsService.findRelatedCompanies(targetCompany);
        return ResponseEntity.ok().body(relatedCompanies);
    }

    // ===== ADVANCED ANALYTICS ENDPOINTS =====

    @GetMapping("/analytics/frenemy-relationships")
    @Operation(summary = "Analyze frenemy relationships", 
               description = "Find competitors who are also partners (like Apple-Samsung dynamics)")
    public ResponseEntity<List<Object>> analyzeFrenemyRelationships() {
        List<Object> relationships = graphAnalyticsService.analyzeFrenemyRelationships();
        return ResponseEntity.ok().body(relationships);
    }

    @GetMapping("/analytics/vulnerabilities")
    @Operation(summary = "Assess supply chain vulnerabilities", 
               description = "Identify single points of failure in supply chain networks")
    public ResponseEntity<List<Object>> assessSupplyChainVulnerabilities(
            @Parameter(description = "Minimum downstream impact threshold", example = "3")
            @RequestParam(defaultValue = "3") Integer minDownstreamImpact) {
        
        List<Object> vulnerabilities = graphAnalyticsService.assessSupplyChainVulnerabilities(minDownstreamImpact);
        return ResponseEntity.ok().body(vulnerabilities);
    }

    @GetMapping("/analytics/acquisition-targets")
    @Operation(summary = "Identify acquisition targets", 
               description = "Find companies with high network value but lower market cap")
    public ResponseEntity<List<Object>> identifyAcquisitionTargets(
            @Parameter(description = "Minimum centrality score", example = "0.01")
            @RequestParam(defaultValue = "0.01") Double minCentrality,
            @Parameter(description = "Maximum market cap in USD", example = "50000000000")
            @RequestParam(defaultValue = "50000000000") Long maxMarketCap) {
        
        List<Object> targets = graphAnalyticsService.identifyAcquisitionTargets(minCentrality, maxMarketCap);
        return ResponseEntity.ok().body(targets);
    }

    // ===== MCP AGENT INTEGRATION ENDPOINTS =====

    @PostMapping("/mcp/graph-context")
    @Operation(summary = "Get graph context for MCP agents", 
               description = "Provide structured graph data for AI agent decision making")
    public ResponseEntity<GraphAnalyticsService.GraphContext> getGraphContextForAgent(
            @RequestBody AgentContextRequest request) {
        
        GraphAnalyticsService.GraphContext context = graphAnalyticsService.getGraphContextForAgent(
            request.getQuery(), request.getEntityNames());
        return ResponseEntity.ok().body(context);
    }

    @PostMapping("/mcp/execute-analysis")
    @Operation(summary = "Execute agent-requested analysis", 
               description = "Allow MCP agents to trigger specific graph algorithms")
    public CompletableFuture<ResponseEntity<Object>> executeAgentAnalysis(
            @RequestBody AgentAnalysisRequest request) {
        
        return graphAnalyticsService.executeAgentAnalysis(request.getAnalysisType(), request.getParameters())
            .thenApply(result -> ResponseEntity.ok().body(result));
    }

    // ===== FINANCIAL SERVICES USE CASE ENDPOINTS =====

    @GetMapping("/financial/fraud-patterns")
    @Operation(summary = "Detect potential fraud patterns", 
               description = "Apply graph analytics to identify suspicious relationship patterns")
    public CompletableFuture<ResponseEntity<Object>> detectFraudPatterns(
            @Parameter(description = "Analysis time window", example = "30d")
            @RequestParam(defaultValue = "30d") String timeWindow,
            @Parameter(description = "Minimum risk score threshold", example = "0.7")
            @RequestParam(defaultValue = "0.7") Double minRiskScore) {
        
        // Simulate fraud detection using existing graph algorithms
        return graphAnalyticsService.analyzeCriticalNodes(10)
            .thenApply(nodes -> {
                FraudAnalysisResult result = new FraudAnalysisResult();
                result.setTimeWindow(timeWindow);
                result.setMinRiskScore(minRiskScore);
                result.setSuspiciousEntities(nodes.stream()
                    .filter(node -> node.getCentralityScore() > minRiskScore)
                    .map(node -> "Suspicious pattern detected for: " + node.getCompanyName())
                    .toList());
                return ResponseEntity.ok().body(result);
            });
    }

    @GetMapping("/financial/trading-intelligence")
    @Operation(summary = "Generate trading intelligence", 
               description = "Analyze market relationships for trading strategy insights")
    public ResponseEntity<Object> generateTradingIntelligence(
            @Parameter(description = "Target sector", example = "Technology")
            @RequestParam(defaultValue = "Technology") String sector,
            @Parameter(description = "Analysis depth", example = "3")
            @RequestParam(defaultValue = "3") Integer analysisDepth) {
        
        // Combine multiple analytics for trading intelligence
        List<Object> frenemyRelationships = graphAnalyticsService.analyzeFrenemyRelationships();
        List<Object> acquisitionTargets = graphAnalyticsService.identifyAcquisitionTargets(0.01, 100000000000L);
        
        TradingIntelligenceResult result = new TradingIntelligenceResult();
        result.setSector(sector);
        result.setAnalysisDepth(analysisDepth);
        result.setCompetitiveRelationships(frenemyRelationships);
        result.setInvestmentOpportunities(acquisitionTargets);
        result.setInsights(List.of(
            "Complex competitor relationships indicate market consolidation opportunities",
            "High centrality companies with low market cap present strategic investment potential",
            "Supply chain vulnerabilities create both risks and opportunities"
        ));
        
        return ResponseEntity.ok().body(result);
    }

    // ===== EXCEPTION HANDLING =====

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        ErrorResponse error = new ErrorResponse("INVALID_REQUEST", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", 
            "An unexpected error occurred: " + e.getMessage());
        return ResponseEntity.internalServerError().body(error);
    }

    // ===== RESPONSE DTOs =====

    public static class HealthResponse {
        private String status;
        private Long timestamp;

        public HealthResponse(String status, Long timestamp) {
            this.status = status;
            this.timestamp = timestamp;
        }

        public String getStatus() { return status; }
        public Long getTimestamp() { return timestamp; }
    }

    public static class BatchIngestResponse {
        private Integer count;
        private String message;

        public BatchIngestResponse(Integer count, String message) {
            this.count = count;
            this.message = message;
        }

        public Integer getCount() { return count; }
        public String getMessage() { return message; }
    }

    public static class AgentContextRequest {
        private String query;
        private List<String> entityNames;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public List<String> getEntityNames() { return entityNames; }
        public void setEntityNames(List<String> entityNames) { this.entityNames = entityNames; }
    }

    public static class AgentAnalysisRequest {
        private String analysisType;
        private Object parameters;

        public String getAnalysisType() { return analysisType; }
        public void setAnalysisType(String analysisType) { this.analysisType = analysisType; }

        public Object getParameters() { return parameters; }
        public void setParameters(Object parameters) { this.parameters = parameters; }
    }

    public static class FraudAnalysisResult {
        private String timeWindow;
        private Double minRiskScore;
        private List<String> suspiciousEntities;

        public String getTimeWindow() { return timeWindow; }
        public void setTimeWindow(String timeWindow) { this.timeWindow = timeWindow; }

        public Double getMinRiskScore() { return minRiskScore; }
        public void setMinRiskScore(Double minRiskScore) { this.minRiskScore = minRiskScore; }

        public List<String> getSuspiciousEntities() { return suspiciousEntities; }
        public void setSuspiciousEntities(List<String> suspiciousEntities) { 
            this.suspiciousEntities = suspiciousEntities; 
        }
    }

    public static class TradingIntelligenceResult {
        private String sector;
        private Integer analysisDepth;
        private List<Object> competitiveRelationships;
        private List<Object> investmentOpportunities;
        private List<String> insights;

        public String getSector() { return sector; }
        public void setSector(String sector) { this.sector = sector; }

        public Integer getAnalysisDepth() { return analysisDepth; }
        public void setAnalysisDepth(Integer analysisDepth) { this.analysisDepth = analysisDepth; }

        public List<Object> getCompetitiveRelationships() { return competitiveRelationships; }
        public void setCompetitiveRelationships(List<Object> competitiveRelationships) { 
            this.competitiveRelationships = competitiveRelationships; 
        }

        public List<Object> getInvestmentOpportunities() { return investmentOpportunities; }
        public void setInvestmentOpportunities(List<Object> investmentOpportunities) { 
            this.investmentOpportunities = investmentOpportunities; 
        }

        public List<String> getInsights() { return insights; }
        public void setInsights(List<String> insights) { this.insights = insights; }
    }

    public static class ErrorResponse {
        private String errorCode;
        private String message;

        public ErrorResponse(String errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }

        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
    }
}