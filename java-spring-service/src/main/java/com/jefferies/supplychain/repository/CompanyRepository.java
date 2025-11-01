package com.jefferies.supplychain.repository;

import com.jefferies.supplychain.model.Company;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Neo4j Repository for Company entities with advanced graph analytics capabilities.
 * 
 * This repository provides data access methods for the supply chain graph,
 * including ETL operations, graph algorithms, and complex relationship queries
 * suitable for financial services analytics at Jefferies Securities.
 * 
 * Key Capabilities:
 * - Entity ingestion and relationship creation (ETL support)
 * - Graph Data Science (GDS) algorithm integration
 * - Pathfinding for supply chain backup analysis
 * - Centrality analysis for critical node identification
 * - Community detection for relationship clustering
 * - Complex multi-hop relationship queries
 * 
 * @author Graph Data Engineer Team
 * @version 1.0.0
 */
@Repository
public interface CompanyRepository extends Neo4jRepository<Company, Long> {

    // ===== BASIC CRUD AND SEARCH OPERATIONS =====

    /**
     * Find company by exact name match
     */
    Optional<Company> findByName(String name);

    /**
     * Find companies by industry sector
     */
    List<Company> findByIndustrySector(String sector);

    /**
     * Find companies by country
     */
    List<Company> findByCountry(String country);

    /**
     * Find companies with match score above threshold
     */
    @Query("MATCH (c:Company) WHERE c.match_score >= $minScore RETURN c ORDER BY c.match_score DESC")
    List<Company> findByMinMatchScore(Double minScore);

    // ===== ETL AND DATA INGESTION OPERATIONS =====

    /**
     * Idempotent entity ingestion with relationship creation.
     * Supports the Python ETL pipeline by providing atomic upsert operations.
     * 
     * @param permid Unique PermID identifier
     * @param name Company name
     * @param isFinalAssembler Whether company does final assembly
     * @param matchScore Entity resolution confidence score
     * @param supplierName Name of supplier to create relationship with
     */
    @Query("""
        MERGE (c:Company {permid: $permid})
        ON CREATE SET c.name = $name,
                      c.is_final_assembler = $isFinalAssembler,
                      c.match_score = $matchScore,
                      c.ingestion_date = datetime()
        ON MATCH SET c.match_score = CASE WHEN $matchScore > c.match_score 
                                          THEN $matchScore 
                                          ELSE c.match_score END
        WITH c
        OPTIONAL MATCH (supplier:Company {name: $supplierName})
        FOREACH (_ IN CASE WHEN supplier IS NOT NULL THEN [1] ELSE [] END |
            MERGE (supplier)-[:SUPPLY_COMPONENTS {
                created_date: datetime(),
                confidence: $matchScore
            }]->(c)
        )
        RETURN c
    """)
    Company ingestEntityAndRelationship(Long permid, String name, Boolean isFinalAssembler, 
                                       Double matchScore, String supplierName);

    /**
     * Batch ingestion for multiple entities
     * Optimized for ETL pipeline performance
     */
    @Query("""
        UNWIND $entities AS entity
        MERGE (c:Company {permid: entity.permid})
        ON CREATE SET c.name = entity.name,
                      c.is_final_assembler = entity.isFinalAssembler,
                      c.match_score = entity.matchScore,
                      c.industry_sector = entity.industrySector,
                      c.country = entity.country,
                      c.ingestion_date = datetime()
        ON MATCH SET c.match_score = CASE WHEN entity.matchScore > c.match_score 
                                          THEN entity.matchScore 
                                          ELSE c.match_score END
        RETURN count(c) as ingestedCount
    """)
    Integer batchIngestEntities(List<Company> entities);

    /**
     * Create competition relationships (frenemy analysis like Apple-Samsung)
     */
    @Query("""
        MATCH (c1:Company {name: $company1Name}), (c2:Company {name: $company2Name})
        MERGE (c1)-[:COMPETES_WITH {
            relationship_type: $relationshipType,
            strength: $strength,
            created_date: datetime()
        }]->(c2)
        MERGE (c2)-[:COMPETES_WITH {
            relationship_type: $relationshipType,
            strength: $strength,
            created_date: datetime()
        }]->(c1)
    """)
    void createCompetitionRelationship(String company1Name, String company2Name, 
                                     String relationshipType, Double strength);

    // ===== GRAPH ANALYTICS: PATHFINDING =====

    /**
     * Find shortest path between two companies for backup supplier analysis.
     * Uses Dijkstra algorithm with relationship weights for optimal route discovery.
     * 
     * Financial Use Case: Similar to finding alternative trading routes or 
     * backup counterparty relationships in securities trading.
     */
    @Query("""
        MATCH (start:Company {name: $startName}), (end:Company {name: $endName})
        CALL gds.shortestPath.dijkstra.stream('supply_chain_graph', {
            sourceNode: id(start),
            targetNode: id(end),
            relationshipWeightProperty: 'reliability_score'
        })
        YIELD index, sourceNodeId, targetNodeId, totalCost, nodeIds, costs, path
        RETURN gds.util.asNode(sourceNodeId).name AS source,
               gds.util.asNode(targetNodeId).name AS target,
               totalCost,
               size(nodeIds) AS pathLength,
               [nodeId IN nodeIds | gds.util.asNode(nodeId).name] AS pathNames,
               costs
        ORDER BY totalCost ASC
        LIMIT 10
    """)
    List<Company.PathResult> findOptimalSupplyPath(String startName, String endName);

    /**
     * Find all paths between two companies within hop limit
     * Useful for risk analysis and alternative route discovery
     */
    @Query("""
        MATCH path = (start:Company {name: $startName})-[*1..$maxHops]-(end:Company {name: $endName})
        WHERE start <> end
        WITH path, length(path) as pathLength,
             [node IN nodes(path) | node.name] as pathNames,
             reduce(cost = 0.0, rel IN relationships(path) | 
                cost + coalesce(rel.reliability_score, 1.0)) as totalCost
        RETURN pathNames, pathLength, totalCost
        ORDER BY pathLength ASC, totalCost ASC
        LIMIT $maxResults
    """)
    List<Company.PathResult> findAllPathsWithinHops(String startName, String endName, 
                                                   Integer maxHops, Integer maxResults);

    // ===== GRAPH ANALYTICS: CENTRALITY ANALYSIS =====

    /**
     * Calculate PageRank centrality to identify the most influential companies.
     * High PageRank indicates critical supply chain nodes (like ARM Holdings).
     * 
     * Financial Use Case: Identify systemically important institutions or 
     * key market makers in trading networks.
     */
    @Query("""
        CALL gds.pageRank.stream('supply_chain_graph', {
            maxIterations: 20,
            dampingFactor: 0.85,
            tolerance: 0.0000001
        })
        YIELD nodeId, score
        WITH gds.util.asNode(nodeId) AS company, score
        RETURN company.name AS companyName,
               company.permid AS permid,
               score AS centralityScore,
               'PAGERANK' AS centralityType,
               row_number() OVER (ORDER BY score DESC) AS rank
        ORDER BY score DESC
        LIMIT $topN
    """)
    List<Company.CentralityResult> calculatePageRankCentrality(Integer topN);

    /**
     * Calculate Betweenness centrality to identify bridge companies.
     * High betweenness indicates companies that control information/resource flow.
     * 
     * Financial Use Case: Identify key intermediaries in transaction networks
     * or critical nodes in regulatory reporting chains.
     */
    @Query("""
        CALL gds.betweenness.stream('supply_chain_graph')
        YIELD nodeId, score
        WITH gds.util.asNode(nodeId) AS company, score
        WHERE score > 0
        RETURN company.name AS companyName,
               company.permid AS permid,
               score AS centralityScore,
               'BETWEENNESS' AS centralityType,
               row_number() OVER (ORDER BY score DESC) AS rank
        ORDER BY score DESC
        LIMIT $topN
    """)
    List<Company.CentralityResult> calculateBetweennessCentrality(Integer topN);

    /**
     * Update centrality scores in the graph for future queries
     * Supports batch analytics and caching strategies
     */
    @Query("""
        CALL gds.pageRank.mutate('supply_chain_graph', {
            mutateProperty: 'pagerank_score',
            maxIterations: 20,
            dampingFactor: 0.85
        })
        YIELD nodePropertiesWritten, ranIterations
        RETURN nodePropertiesWritten, ranIterations
    """)
    void updatePageRankScores();

    // ===== GRAPH ANALYTICS: COMMUNITY DETECTION =====

    /**
     * Detect communities using Louvain algorithm for supply chain clustering.
     * Identifies groups of companies that work closely together.
     * 
     * Financial Use Case: Detect trading clusters, identify potential 
     * market manipulation groups, or segment customer bases.
     */
    @Query("""
        CALL gds.louvain.stream('supply_chain_graph', {
            includeIntermediateCommunities: true,
            maxIterations: 10,
            tolerance: 0.0001
        })
        YIELD nodeId, communityId, intermediateCommunityIds
        WITH gds.util.asNode(nodeId) AS company, communityId
        RETURN communityId,
               collect({
                   name: company.name,
                   permid: company.permid,
                   is_final_assembler: company.is_final_assembler
               }) AS members,
               count(company) AS communitySize
        ORDER BY communitySize DESC
    """)
    List<Object> detectCommunities();

    /**
     * Get companies in the same community as a target company
     * Useful for competitive analysis and market segmentation
     */
    @Query("""
        MATCH (target:Company {name: $targetCompany})
        WHERE target.community_id IS NOT NULL
        MATCH (related:Company)
        WHERE related.community_id = target.community_id AND related <> target
        RETURN related
        ORDER BY related.pagerank_score DESC
    """)
    List<Company> findCompaniesInSameCommunity(String targetCompany);

    // ===== COMPLEX RELATIONSHIP QUERIES =====

    /**
     * Find competitors who are also suppliers (frenemy analysis)
     * Models complex business relationships like Apple-Samsung dynamics
     */
    @Query("""
        MATCH (c1:Company)-[:COMPETES_WITH]->(c2:Company)
        MATCH (c1)-[:SUPPLY_COMPONENTS|PARTNER_WITH]->(c2)
        RETURN c1.name AS company1,
               c2.name AS company2,
               'FRENEMY' AS relationshipType,
               c1.pagerank_score + c2.pagerank_score AS combinedInfluence
        ORDER BY combinedInfluence DESC
    """)
    List<Object> findFrenemyRelationships();

    /**
     * Analyze supply chain vulnerability by finding single points of failure
     * Identifies companies with high outgoing supply relationships but few alternatives
     */
    @Query("""
        MATCH (supplier:Company)-[:SUPPLY_COMPONENTS]->(customer:Company)
        WITH customer, collect(supplier) AS suppliers, count(supplier) AS supplierCount
        WHERE supplierCount = 1
        MATCH (customer)-[:SUPPLY_COMPONENTS]->(downstream:Company)
        WITH customer, suppliers[0] AS singleSupplier, count(downstream) AS downstreamCount
        WHERE downstreamCount > $minDownstream
        RETURN customer.name AS vulnerableCustomer,
               singleSupplier.name AS criticalSupplier,
               downstreamCount AS impactSize,
               customer.pagerank_score AS customerImportance
        ORDER BY downstreamCount DESC, customerImportance DESC
    """)
    List<Object> findSupplyChainVulnerabilities(Integer minDownstream);

    /**
     * Advanced graph pattern: Find potential acquisition targets
     * Companies with high centrality but low market cap (undervalued network position)
     */
    @Query("""
        MATCH (c:Company)
        WHERE c.pagerank_score IS NOT NULL 
          AND c.market_cap IS NOT NULL
          AND c.pagerank_score > $minCentrality
          AND c.market_cap < $maxMarketCap
        OPTIONAL MATCH (c)-[r:SUPPLY_COMPONENTS|COMPETES_WITH|PARTNER_WITH]-()
        WITH c, count(r) AS relationshipCount
        RETURN c.name AS companyName,
               c.permid AS permid,
               c.pagerank_score AS networkImportance,
               c.market_cap AS marketCap,
               relationshipCount AS networkSize,
               c.pagerank_score / (c.market_cap / 1000000.0) AS valueEfficiency
        ORDER BY valueEfficiency DESC
        LIMIT $maxResults
    """)
    List<Object> findAcquisitionTargets(Double minCentrality, Long maxMarketCap, Integer maxResults);

    // ===== GRAPH PROJECTION MANAGEMENT =====

    /**
     * Create or update the graph projection for GDS algorithms
     * Essential for performance optimization of graph analytics
     */
    @Query("""
        CALL gds.graph.project(
            'supply_chain_graph',
            'Company',
            {
                SUPPLY_COMPONENTS: {
                    orientation: 'NATURAL',
                    properties: ['reliability_score', 'confidence']
                },
                COMPETES_WITH: {
                    orientation: 'UNDIRECTED',
                    properties: ['strength']
                },
                DESIGN_CHIPS_FOR: {
                    orientation: 'NATURAL'
                },
                PARTNER_WITH: {
                    orientation: 'UNDIRECTED'
                }
            },
            {
                nodeProperties: ['pagerank_score', 'match_score', 'market_cap']
            }
        )
        YIELD graphName, nodeCount, relationshipCount
        RETURN graphName, nodeCount, relationshipCount
    """)
    Object createGraphProjection();

    /**
     * Drop existing graph projection to refresh with new data
     */
    @Query("CALL gds.graph.drop('supply_chain_graph', false)")
    void dropGraphProjection();
}