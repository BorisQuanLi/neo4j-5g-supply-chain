package com.jefferies.supplychain.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Neo4j Node entity representing a Company in the supply chain graph.
 * 
 * This model represents entities like Apple, Samsung, Qualcomm, ARM Holdings
 * from the original 5G supply chain analysis, with properties and relationships
 * suitable for financial services graph analytics.
 * 
 * Key Graph Properties:
 * - permid: Unique identifier from PermID.org (Thomson Reuters)
 * - matchScore: Confidence score from entity resolution (e.g., 92% for Apple Inc)
 * - centralityScore: PageRank or betweenness centrality for importance analysis
 * - communityId: Cluster ID from community detection algorithms
 * 
 * Relationships modeled:
 * - SUPPLY_COMPONENTS: Supply chain relationships
 * - COMPETES_WITH: Market competition relationships  
 * - DESIGN_CHIPS_FOR: Design/licensing relationships
 * - PARTNER_WITH: Strategic partnerships
 */
@Node("Company")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Company {

    @Id
    private Long permid;

    @Property("name")
    private String name;

    @Property("is_final_assembler")
    private Boolean isFinalAssembler;

    @Property("match_score")
    private Double matchScore;

    @Property("ingestion_date")
    private LocalDateTime ingestionDate;

    @Property("industry_sector")
    private String industrySector;

    @Property("country")
    private String country;

    @Property("market_cap")
    private Long marketCap;

    @Property("revenue")
    private Long revenue;

    // Graph Analytics Properties (calculated by GDS algorithms)
    @Property("pagerank_score")
    private Double pagerankScore;

    @Property("betweenness_centrality")
    private Double betweennessCentrality;

    @Property("community_id")
    private Long communityId;

    @Property("clustering_coefficient")
    private Double clusteringCoefficient;

    // Supply Chain Relationships (outgoing)
    @Relationship(type = "SUPPLY_COMPONENTS", direction = Relationship.Direction.OUTGOING)
    private List<Company> suppliers;

    @Relationship(type = "COMPETES_WITH", direction = Relationship.Direction.OUTGOING)
    private List<Company> competitors;

    @Relationship(type = "DESIGN_CHIPS_FOR", direction = Relationship.Direction.OUTGOING)
    private List<Company> designPartners;

    @Relationship(type = "PARTNER_WITH", direction = Relationship.Direction.OUTGOING)
    private List<Company> partners;

    // Constructors
    public Company() {}

    public Company(Long permid, String name, Boolean isFinalAssembler, Double matchScore) {
        this.permid = permid;
        this.name = name;
        this.isFinalAssembler = isFinalAssembler;
        this.matchScore = matchScore;
        this.ingestionDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getPermid() {
        return permid;
    }

    public void setPermid(Long permid) {
        this.permid = permid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsFinalAssembler() {
        return isFinalAssembler;
    }

    public void setIsFinalAssembler(Boolean isFinalAssembler) {
        this.isFinalAssembler = isFinalAssembler;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }

    public LocalDateTime getIngestionDate() {
        return ingestionDate;
    }

    public void setIngestionDate(LocalDateTime ingestionDate) {
        this.ingestionDate = ingestionDate;
    }

    public String getIndustrySector() {
        return industrySector;
    }

    public void setIndustrySector(String industrySector) {
        this.industrySector = industrySector;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Long getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(Long marketCap) {
        this.marketCap = marketCap;
    }

    public Long getRevenue() {
        return revenue;
    }

    public void setRevenue(Long revenue) {
        this.revenue = revenue;
    }

    public Double getPagerankScore() {
        return pagerankScore;
    }

    public void setPagerankScore(Double pagerankScore) {
        this.pagerankScore = pagerankScore;
    }

    public Double getBetweennessCentrality() {
        return betweennessCentrality;
    }

    public void setBetweennessCentrality(Double betweennessCentrality) {
        this.betweennessCentrality = betweennessCentrality;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public Double getClusteringCoefficient() {
        return clusteringCoefficient;
    }

    public void setClusteringCoefficient(Double clusteringCoefficient) {
        this.clusteringCoefficient = clusteringCoefficient;
    }

    public List<Company> getSuppliers() {
        return suppliers;
    }

    public void setSuppliers(List<Company> suppliers) {
        this.suppliers = suppliers;
    }

    public List<Company> getCompetitors() {
        return competitors;
    }

    public void setCompetitors(List<Company> competitors) {
        this.competitors = competitors;
    }

    public List<Company> getDesignPartners() {
        return designPartners;
    }

    public void setDesignPartners(List<Company> designPartners) {
        this.designPartners = designPartners;
    }

    public List<Company> getPartners() {
        return partners;
    }

    public void setPartners(List<Company> partners) {
        this.partners = partners;
    }

    // Business Logic Methods

    /**
     * Determines if this company is a critical node in the supply chain
     * based on centrality scores and relationship counts
     */
    public boolean isCriticalNode() {
        return (pagerankScore != null && pagerankScore > 0.1) ||
               (betweennessCentrality != null && betweennessCentrality > 0.05);
    }

    /**
     * Gets the total relationship count for network density analysis
     */
    public int getTotalRelationshipCount() {
        int count = 0;
        if (suppliers != null) count += suppliers.size();
        if (competitors != null) count += competitors.size();
        if (designPartners != null) count += designPartners.size();
        if (partners != null) count += partners.size();
        return count;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Company company = (Company) o;
        return Objects.equals(permid, company.permid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permid);
    }

    @Override
    public String toString() {
        return "Company{" +
                "permid=" + permid +
                ", name='" + name + '\'' +
                ", isFinalAssembler=" + isFinalAssembler +
                ", matchScore=" + matchScore +
                ", pagerankScore=" + pagerankScore +
                ", communityId=" + communityId +
                '}';
    }

    // Inner Classes for Complex Query Results

    /**
     * DTO for pathfinding algorithm results
     * Used for supply chain backup route analysis
     */
    public static class PathResult {
        private String source;
        private String target;
        private Double totalCost;
        private List<String> pathNames;
        private Integer pathLength;
        private Double reliabilityScore;

        // Constructors
        public PathResult() {}

        public PathResult(String source, String target, Double totalCost, 
                         List<String> pathNames, Integer pathLength) {
            this.source = source;
            this.target = target;
            this.totalCost = totalCost;
            this.pathNames = pathNames;
            this.pathLength = pathLength;
        }

        // Getters and Setters
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }

        public Double getTotalCost() { return totalCost; }
        public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }

        public List<String> getPathNames() { return pathNames; }
        public void setPathNames(List<String> pathNames) { this.pathNames = pathNames; }

        public Integer getPathLength() { return pathLength; }
        public void setPathLength(Integer pathLength) { this.pathLength = pathLength; }

        public Double getReliabilityScore() { return reliabilityScore; }
        public void setReliabilityScore(Double reliabilityScore) { this.reliabilityScore = reliabilityScore; }
    }

    /**
     * DTO for centrality analysis results
     * Used for identifying critical supply chain nodes
     */
    public static class CentralityResult {
        private String companyName;
        private Long permid;
        private Double centralityScore;
        private String centralityType;
        private Integer rank;
        private String criticality;

        // Constructors
        public CentralityResult() {}

        public CentralityResult(String companyName, Long permid, Double centralityScore, 
                              String centralityType, Integer rank) {
            this.companyName = companyName;
            this.permid = permid;
            this.centralityScore = centralityScore;
            this.centralityType = centralityType;
            this.rank = rank;
            this.criticality = determineCriticality(centralityScore);
        }

        private String determineCriticality(Double score) {
            if (score > 0.1) return "CRITICAL";
            if (score > 0.05) return "HIGH";
            if (score > 0.01) return "MEDIUM";
            return "LOW";
        }

        // Getters and Setters
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }

        public Long getPermid() { return permid; }
        public void setPermid(Long permid) { this.permid = permid; }

        public Double getCentralityScore() { return centralityScore; }
        public void setCentralityScore(Double centralityScore) { this.centralityScore = centralityScore; }

        public String getCentralityType() { return centralityType; }
        public void setCentralityType(String centralityType) { this.centralityType = centralityType; }

        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }

        public String getCriticality() { return criticality; }
        public void setCriticality(String criticality) { this.criticality = criticality; }
    }
}