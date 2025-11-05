"""
Neo4j Graph Database Client for Supply Chain ETL Pipeline

This module provides a robust Neo4j client wrapper for the Python ETL pipeline,
supporting the ingestion of supply chain data from external APIs (WIKIDATA, PermID.org)
and graph construction for financial services analytics.

Key Features:
- Async Neo4j driver support for high-performance ETL
- Transaction management for data consistency
- Graph projection management for GDS algorithms
- Error handling and retry logic
- Integration with GraphRAG for LLM applications

Author: Graph Data Engineer Team
Version: 1.0.0
Date: 2025-10-31
"""

import asyncio
import logging
from typing import Dict, List, Any, Optional, Union
from contextlib import asynccontextmanager
import time
from dataclasses import dataclass
from neo4j import GraphDatabase, AsyncGraphDatabase
from neo4j.exceptions import Neo4jError, ServiceUnavailable, TransientError
from retry import retry
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class CompanyEntity:
    """Data class for company entities to be ingested into Neo4j"""
    permid: int
    name: str
    is_final_assembler: bool
    match_score: float
    industry_sector: Optional[str] = None
    country: Optional[str] = None
    market_cap: Optional[int] = None
    revenue: Optional[int] = None


@dataclass
class RelationshipData:
    """Data class for relationships between companies"""
    source_name: str
    target_name: str
    relationship_type: str
    properties: Dict[str, Any]


class Neo4jClient:
    """
    Async Neo4j client for supply chain graph ETL operations.
    
    This client provides methods for:
    - Entity ingestion and relationship creation
    - Graph projection management for GDS algorithms
    - Transaction management for data consistency
    - Error handling and connection management
    """
    
    def __init__(self, 
                 uri: str = None, 
                 username: str = None, 
                 password: str = None,
                 max_connection_pool_size: int = 50):
        """
        Initialize Neo4j client with connection parameters.
        
        Args:
            uri: Neo4j connection URI (defaults to environment variable)
            username: Neo4j username (defaults to environment variable)
            password: Neo4j password (defaults to environment variable)
            max_connection_pool_size: Maximum connection pool size
        """
        self.uri = uri or os.getenv('NEO4J_URI', 'bolt://localhost:7687')
        self.username = username or os.getenv('NEO4J_USERNAME', 'neo4j')
        self.password = password or os.getenv('NEO4J_PASSWORD', 'password')
        
        self.driver = None
        self.session = None
        self.max_pool_size = max_connection_pool_size
        
        # Performance metrics
        self.query_count = 0
        self.total_execution_time = 0.0
        
    async def __aenter__(self):
        """Async context manager entry"""
        await self.connect()
        return self
        
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit"""
        await self.close()
        
    async def connect(self):
        """Establish connection to Neo4j database"""
        try:
            self.driver = AsyncGraphDatabase.driver(
                self.uri,
                auth=(self.username, self.password),
                max_connection_pool_size=self.max_pool_size,
                connection_timeout=30,
                max_transaction_retry_time=30
            )
            
            # Verify connectivity
            await self.driver.verify_connectivity()
            logger.info(f"Connected to Neo4j at {self.uri}")
            
        except Exception as e:
            logger.error(f"Failed to connect to Neo4j: {e}")
            raise
            
    async def close(self):
        """Close Neo4j connection"""
        if self.driver:
            await self.driver.close()
            logger.info("Neo4j connection closed")
            
    @asynccontextmanager
    async def session_scope(self, database: str = "neo4j"):
        """Async context manager for Neo4j sessions"""
        session = self.driver.session(database=database)
        try:
            yield session
        finally:
            await session.close()
            
    @retry(exceptions=(ServiceUnavailable, TransientError), tries=3, delay=2, backoff=2)
    async def execute_query(self, 
                          query: str, 
                          parameters: Dict[str, Any] = None,
                          database: str = "neo4j") -> List[Dict[str, Any]]:
        """
        Execute a Cypher query with retry logic and performance tracking.
        
        Args:
            query: Cypher query string
            parameters: Query parameters
            database: Target database name
            
        Returns:
            List of query results as dictionaries
        """
        if parameters is None:
            parameters = {}
            
        start_time = time.time()
        
        try:
            async with self.session_scope(database) as session:
                result = await session.run(query, parameters)
                records = [record.data() async for record in result]
                
                # Update performance metrics
                execution_time = time.time() - start_time
                self.query_count += 1
                self.total_execution_time += execution_time
                
                logger.debug(f"Query executed in {execution_time:.3f}s, returned {len(records)} records")
                return records
                
        except Neo4jError as e:
            logger.error(f"Neo4j query failed: {e}")
            raise
        except Exception as e:
            logger.error(f"Unexpected error executing query: {e}")
            raise
            
    async def execute_write_transaction(self, 
                                      queries: List[tuple],
                                      database: str = "neo4j") -> List[Any]:
        """
        Execute multiple queries in a single write transaction.
        
        Args:
            queries: List of (query, parameters) tuples
            database: Target database name
            
        Returns:
            List of query results
        """
        async def tx_function(tx):
            results = []
            for query, parameters in queries:
                result = await tx.run(query, parameters or {})
                records = [record.data() async for record in result]
                results.append(records)
            return results
            
        try:
            async with self.session_scope(database) as session:
                results = await session.execute_write(tx_function)
                return results
                
        except Neo4jError as e:
            logger.error(f"Transaction failed: {e}")
            raise
            
    # ===== ENTITY INGESTION METHODS =====
    
    async def ingest_company_entity(self, company: CompanyEntity) -> Dict[str, Any]:
        """
        Ingest a single company entity with idempotent upsert.
        
        Args:
            company: CompanyEntity object to ingest
            
        Returns:
            Dictionary with ingestion result
        """
        query = """
        MERGE (c:Company {permid: $permid})
        ON CREATE SET c.name = $name,
                      c.is_final_assembler = $is_final_assembler,
                      c.match_score = $match_score,
                      c.industry_sector = $industry_sector,
                      c.country = $country,
                      c.market_cap = $market_cap,
                      c.revenue = $revenue,
                      c.ingestion_date = datetime(),
                      c.created_by = 'python_etl'
        ON MATCH SET c.match_score = CASE WHEN $match_score > c.match_score 
                                          THEN $match_score 
                                          ELSE c.match_score END,
                     c.last_updated = datetime()
        RETURN c.permid AS permid, c.name AS name, 'SUCCESS' AS status
        """
        
        parameters = {
            'permid': company.permid,
            'name': company.name,
            'is_final_assembler': company.is_final_assembler,
            'match_score': company.match_score,
            'industry_sector': company.industry_sector,
            'country': company.country,
            'market_cap': company.market_cap,
            'revenue': company.revenue
        }
        
        try:
            results = await self.execute_query(query, parameters)
            return results[0] if results else {'status': 'FAILED'}
            
        except Exception as e:
            logger.error(f"Failed to ingest company {company.name}: {e}")
            return {'status': 'ERROR', 'error': str(e)}
            
    async def batch_ingest_companies(self, companies: List[CompanyEntity]) -> Dict[str, Any]:
        """
        Batch ingest multiple companies for optimal performance.
        
        Args:
            companies: List of CompanyEntity objects
            
        Returns:
            Dictionary with batch ingestion statistics
        """
        if not companies:
            return {'success_count': 0, 'error_count': 0, 'total': 0}
            
        # Convert companies to parameter format
        entities_data = []
        for company in companies:
            entities_data.append({
                'permid': company.permid,
                'name': company.name,
                'is_final_assembler': company.is_final_assembler,
                'match_score': company.match_score,
                'industry_sector': company.industry_sector,
                'country': company.country,
                'market_cap': company.market_cap,
                'revenue': company.revenue
            })
            
        query = """
        UNWIND $entities AS entity
        MERGE (c:Company {permid: entity.permid})
        ON CREATE SET c.name = entity.name,
                      c.is_final_assembler = entity.is_final_assembler,
                      c.match_score = entity.match_score,
                      c.industry_sector = entity.industry_sector,
                      c.country = entity.country,
                      c.market_cap = entity.market_cap,
                      c.revenue = entity.revenue,
                      c.ingestion_date = datetime(),
                      c.created_by = 'python_etl_batch'
        ON MATCH SET c.match_score = CASE WHEN entity.match_score > c.match_score 
                                          THEN entity.match_score 
                                          ELSE c.match_score END,
                     c.last_updated = datetime()
        RETURN count(c) AS ingested_count
        """
        
        try:
            results = await self.execute_query(query, {'entities': entities_data})
            success_count = results[0]['ingested_count'] if results else 0
            
            return {
                'success_count': success_count,
                'error_count': 0,
                'total': len(companies),
                'status': 'SUCCESS'
            }
            
        except Exception as e:
            logger.error(f"Batch ingestion failed: {e}")
            return {
                'success_count': 0,
                'error_count': len(companies),
                'total': len(companies),
                'status': 'ERROR',
                'error': str(e)
            }
            
    # ===== RELATIONSHIP CREATION METHODS =====
    
    async def create_relationship(self, relationship: RelationshipData) -> Dict[str, Any]:
        """
        Create a relationship between two companies.
        
        Args:
            relationship: RelationshipData object defining the relationship
            
        Returns:
            Dictionary with relationship creation result
        """
        query = f"""
        MATCH (source:Company {{name: $source_name}})
        MATCH (target:Company {{name: $target_name}})
        MERGE (source)-[r:{relationship.relationship_type}]->(target)
        ON CREATE SET r += $properties, r.created_date = datetime()
        ON MATCH SET r += $properties, r.last_updated = datetime()
        RETURN source.name AS source, target.name AS target, type(r) AS relationship_type
        """
        
        parameters = {
            'source_name': relationship.source_name,
            'target_name': relationship.target_name,
            'properties': relationship.properties
        }
        
        try:
            results = await self.execute_query(query, parameters)
            return results[0] if results else {'status': 'FAILED'}
            
        except Exception as e:
            logger.error(f"Failed to create relationship: {e}")
            return {'status': 'ERROR', 'error': str(e)}
            
    async def create_supply_chain_relationships(self, 
                                              supplier_customer_pairs: List[tuple]) -> Dict[str, Any]:
        """
        Create multiple supply chain relationships efficiently.
        
        Args:
            supplier_customer_pairs: List of (supplier_name, customer_name) tuples
            
        Returns:
            Dictionary with batch relationship creation statistics
        """
        queries = []
        for supplier, customer in supplier_customer_pairs:
            query = """
            MATCH (supplier:Company {name: $supplier_name})
            MATCH (customer:Company {name: $customer_name})
            MERGE (supplier)-[:SUPPLY_COMPONENTS {
                created_date: datetime(),
                confidence: 0.9,
                relationship_source: 'etl_pipeline'
            }]->(customer)
            """
            queries.append((query, {'supplier_name': supplier, 'customer_name': customer}))
            
        try:
            await self.execute_write_transaction(queries)
            return {
                'relationships_created': len(supplier_customer_pairs),
                'status': 'SUCCESS'
            }
            
        except Exception as e:
            logger.error(f"Failed to create supply chain relationships: {e}")
            return {'status': 'ERROR', 'error': str(e)}
            
    # ===== GRAPH PROJECTION MANAGEMENT =====
    
    async def create_graph_projection(self, projection_name: str = "supply_chain_graph") -> Dict[str, Any]:
        """
        Create or update graph projection for GDS algorithms.
        
        Args:
            projection_name: Name of the graph projection
            
        Returns:
            Dictionary with projection creation result
        """
        # First, try to drop existing projection
        try:
            await self.drop_graph_projection(projection_name)
        except:
            pass  # Projection may not exist
            
        query = """
        CALL gds.graph.project(
            $projection_name,
            'Company',
            {
                SUPPLY_COMPONENTS: {
                    orientation: 'NATURAL',
                    properties: ['confidence']
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
                nodeProperties: ['match_score', 'market_cap', 'revenue']
            }
        )
        YIELD graphName, nodeCount, relationshipCount
        RETURN graphName, nodeCount, relationshipCount
        """
        
        try:
            results = await self.execute_query(query, {'projection_name': projection_name})
            return results[0] if results else {'status': 'FAILED'}
            
        except Exception as e:
            logger.error(f"Failed to create graph projection: {e}")
            return {'status': 'ERROR', 'error': str(e)}
            
    async def drop_graph_projection(self, projection_name: str = "supply_chain_graph"):
        """Drop an existing graph projection."""
        query = "CALL gds.graph.drop($projection_name, false)"
        await self.execute_query(query, {'projection_name': projection_name})
        
    # ===== ANALYTICS AND REPORTING =====
    
    async def get_ingestion_statistics(self) -> Dict[str, Any]:
        """Get statistics about ingested data."""
        query = """
        MATCH (c:Company)
        OPTIONAL MATCH (c)-[r]-()
        WITH c, count(r) AS relationship_count
        RETURN 
            count(c) AS total_companies,
            avg(c.match_score) AS avg_match_score,
            min(c.match_score) AS min_match_score,
            max(c.match_score) AS max_match_score,
            avg(relationship_count) AS avg_relationships_per_company,
            sum(relationship_count)/2 AS total_relationships
        """
        
        try:
            results = await self.execute_query(query)
            stats = results[0] if results else {}
            
            # Add client performance metrics
            stats.update({
                'client_query_count': self.query_count,
                'client_total_execution_time': self.total_execution_time,
                'client_avg_query_time': (self.total_execution_time / self.query_count 
                                        if self.query_count > 0 else 0)
            })
            
            return stats
            
        except Exception as e:
            logger.error(f"Failed to get statistics: {e}")
            return {'error': str(e)}
            
    async def validate_graph_consistency(self) -> Dict[str, Any]:
        """Validate graph data consistency and integrity."""
        validation_queries = {
            'companies_without_permid': "MATCH (c:Company) WHERE c.permid IS NULL RETURN count(c) AS count",
            'companies_without_names': "MATCH (c:Company) WHERE c.name IS NULL OR c.name = '' RETURN count(c) AS count",
            'duplicate_permids': """
                MATCH (c:Company) 
                WITH c.permid AS permid, collect(c) AS companies 
                WHERE size(companies) > 1 
                RETURN count(permid) AS count
            """,
            'orphaned_companies': """
                MATCH (c:Company) 
                WHERE NOT (c)-[]-() 
                RETURN count(c) AS count
            """,
            'relationships_without_dates': """
                MATCH ()-[r]-() 
                WHERE r.created_date IS NULL 
                RETURN count(r) AS count
            """
        }
        
        validation_results = {}
        for check_name, query in validation_queries.items():
            try:
                results = await self.execute_query(query)
                validation_results[check_name] = results[0]['count'] if results else 0
            except Exception as e:
                validation_results[check_name] = f"ERROR: {e}"
                
        return validation_results


# ===== UTILITY FUNCTIONS =====

async def create_sample_data(client: Neo4jClient) -> Dict[str, Any]:
    """
    Create sample 5G supply chain data for testing and demonstration.
    Based on the original 2020 supply chain research data.
    """
    # Sample companies from the original research
    sample_companies = [
        CompanyEntity(
            permid=4295905573,
            name="Apple Inc",
            is_final_assembler=True,
            match_score=0.92,
            industry_sector="Technology",
            country="United States",
            market_cap=3000000000000,  # 3T
            revenue=394000000000  # 394B
        ),
        CompanyEntity(
            permid=4295907706,
            name="Samsung Electronics Co",
            is_final_assembler=True,
            match_score=0.92,
            industry_sector="Technology",
            country="South Korea",
            market_cap=350000000000,  # 350B
            revenue=245000000000  # 245B
        ),
        CompanyEntity(
            permid=4295906830,
            name="QCOM (Qualcomm Inc)",
            is_final_assembler=False,
            match_score=0.92,
            industry_sector="Technology",
            country="United States",
            market_cap=150000000000,  # 150B
            revenue=35000000000  # 35B
        ),
        CompanyEntity(
            permid=4295908001,
            name="ARM Holdings",
            is_final_assembler=False,
            match_score=0.89,
            industry_sector="Technology",
            country="United Kingdom",
            market_cap=60000000000,  # 60B
            revenue=3000000000  # 3B
        ),
        CompanyEntity(
            permid=4295908002,
            name="MediaTek",
            is_final_assembler=False,
            match_score=0.87,
            industry_sector="Technology",
            country="Taiwan",
            market_cap=45000000000,  # 45B
            revenue=18000000000  # 18B
        ),
        CompanyEntity(
            permid=4295908003,
            name="Foxconn",
            is_final_assembler=True,
            match_score=0.85,
            industry_sector="Manufacturing",
            country="Taiwan",
            market_cap=40000000000,  # 40B
            revenue=180000000000  # 180B
        ),
        CompanyEntity(
            permid=4295908005,
            name="Xiaomi Corporation",
            is_final_assembler=True,
            match_score=0.88,
            industry_sector="Technology",
            country="China",
            market_cap=45000000000,  # 45B
            revenue=42000000000  # 42B
        ),
        CompanyEntity(
            permid=4295871234,
            name="TSM (Taiwan Semiconductor Manufacturing Co Ltd)",
            is_final_assembler=False,
            match_score=0.95,
            industry_sector="Technology",
            country="Taiwan",
            market_cap=500000000000,  # 500B
            revenue=70000000000  # 70B
        )
    ]
    
    # Ingest companies
    company_result = await client.batch_ingest_companies(sample_companies)
    
    # Create supply chain relationships
    supply_relationships = [
        ("ARM Holdings", "Qualcomm Inc"),
        ("ARM Holdings", "MediaTek"),
        ("Qualcomm Inc", "Apple Inc"),
        ("Qualcomm Inc", "Xiaomi Corporation"),
        ("MediaTek", "Samsung Electronics Co"),
        ("MediaTek", "Xiaomi Corporation"),
        ("Foxconn", "Apple Inc"),
        ("Foxconn", "Xiaomi Corporation"),
        ("Samsung Electronics Co", "Apple Inc")  # Components supplier
    ]
    
    relationship_result = await client.create_supply_chain_relationships(supply_relationships)
    
    # Create competition relationships
    competition_relationships = [
        RelationshipData("Apple Inc", "Samsung Electronics Co", "COMPETES_WITH", 
                        {"strength": 0.9, "market_segment": "smartphones"}),
        RelationshipData("Samsung Electronics Co", "Xiaomi Corporation", "COMPETES_WITH", 
                        {"strength": 0.8, "market_segment": "smartphones"}),
        RelationshipData("Qualcomm Inc", "MediaTek", "COMPETES_WITH", 
                        {"strength": 0.8, "market_segment": "mobile_chips"})
    ]
    
    for rel in competition_relationships:
        await client.create_relationship(rel)
        
    # Create graph projection
    projection_result = await client.create_graph_projection()
    
    return {
        'companies_ingested': company_result,
        'relationships_created': relationship_result,
        'graph_projection': projection_result
    }


if __name__ == "__main__":
    async def main():
        """Example usage of the Neo4j client"""
        async with Neo4jClient() as client:
            # Create sample data
            logger.info("Creating sample supply chain data...")
            sample_result = await create_sample_data(client)
            logger.info(f"Sample data creation result: {sample_result}")
            
            # Get statistics
            stats = await client.get_ingestion_statistics()
            logger.info(f"Graph statistics: {stats}")
            
            # Validate consistency
            validation = await client.validate_graph_consistency()
            logger.info(f"Validation results: {validation}")
            
    # Run the example
    asyncio.run(main())