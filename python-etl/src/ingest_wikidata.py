"""
WIKIDATA API Integration for Supply Chain Entity Extraction

This module interfaces with WIKIDATA to extract structured company information
for the 5G supply chain graph. It demonstrates the ETL pipeline capabilities
required for the Jefferies Securities Graph Data Engineer role.

Key Features:
- SPARQL query execution against WIKIDATA endpoint
- Entity resolution and data enrichment
- Async processing for performance
- Integration with Neo4j graph database
- Error handling and retry logic

Business Use Cases:
- Corporate intelligence gathering
- Entity resolution for financial analysis
- Supply chain mapping and risk assessment
- Competitive intelligence automation

Author: Graph Data Engineer Team
Version: 1.0.0
Date: 2025-10-31
"""

import asyncio
import aiohttp
import logging
from typing import Dict, List, Any, Optional, Tuple
from dataclasses import dataclass, asdict
import json
import time
from urllib.parse import quote
from retry import retry
import re

from neo4j_client import Neo4jClient, CompanyEntity

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class WikidataEntity:
    """Data class for entities extracted from WIKIDATA"""
    wikidata_id: str
    label: str
    description: Optional[str] = None
    industry: Optional[str] = None
    country: Optional[str] = None
    founded: Optional[str] = None
    headquarters: Optional[str] = None
    revenue: Optional[str] = None
    employees: Optional[str] = None
    website: Optional[str] = None
    stock_symbol: Optional[str] = None
    
    def to_company_entity(self, permid: int, match_score: float) -> CompanyEntity:
        """Convert WikidataEntity to CompanyEntity for Neo4j ingestion"""
        # Parse revenue if available
        revenue_value = None
        if self.revenue:
            # Extract numeric value from revenue string
            revenue_match = re.search(r'[\d,]+', str(self.revenue).replace(',', ''))
            if revenue_match:
                try:
                    revenue_value = int(revenue_match.group()) * 1000000  # Assume millions
                except ValueError:
                    pass
        
        # Determine if it's a final assembler based on industry/description
        is_final_assembler = self._is_final_assembler()
        
        return CompanyEntity(
            permid=permid,
            name=self.label,
            is_final_assembler=is_final_assembler,
            match_score=match_score,
            industry_sector=self.industry,
            country=self.country,
            market_cap=None,  # Not directly available in WIKIDATA
            revenue=revenue_value
        )
    
    def _is_final_assembler(self) -> bool:
        """Determine if company is a final assembler based on industry/description"""
        final_assembler_keywords = [
            'smartphone', 'mobile phone', 'electronics manufacturer',
            'consumer electronics', 'device manufacturer'
        ]
        
        description_text = (self.description or '').lower()
        industry_text = (self.industry or '').lower()
        combined_text = f"{description_text} {industry_text}"
        
        return any(keyword in combined_text for keyword in final_assembler_keywords)


class WikidataIngester:
    """
    WIKIDATA API client for extracting and ingesting supply chain entities.
    
    This class provides methods for:
    - SPARQL query execution against WIKIDATA
    - Entity extraction and data enrichment
    - Integration with Neo4j for graph construction
    - Batch processing for performance optimization
    """
    
    def __init__(self, neo4j_client: Neo4jClient = None):
        """
        Initialize WIKIDATA ingester.
        
        Args:
            neo4j_client: Optional Neo4j client for direct ingestion
        """
        self.wikidata_endpoint = "https://query.wikidata.org/sparql"
        self.neo4j_client = neo4j_client
        
        # Performance tracking
        self.query_count = 0
        self.total_entities_processed = 0
        
        # Rate limiting
        self.request_delay = 1.0  # Seconds between requests
        self.last_request_time = 0
        
    async def __aenter__(self):
        """Async context manager entry"""
        return self
        
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit"""
        pass
        
    @retry(tries=3, delay=2, backoff=2)
    async def execute_sparql_query(self, query: str) -> List[Dict[str, Any]]:
        """
        Execute SPARQL query against WIKIDATA endpoint with retry logic.
        
        Args:
            query: SPARQL query string
            
        Returns:
            List of query results
        """
        # Rate limiting
        current_time = time.time()
        if current_time - self.last_request_time < self.request_delay:
            await asyncio.sleep(self.request_delay - (current_time - self.last_request_time))
        
        headers = {
            'User-Agent': 'SupplyChainGraphETL/1.0 (https://jefferies.com) Neo4jGraphDataEngineer',
            'Accept': 'application/sparql-results+json'
        }
        
        params = {
            'query': query,
            'format': 'json'
        }
        
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(
                    self.wikidata_endpoint, 
                    headers=headers, 
                    params=params,
                    timeout=aiohttp.ClientTimeout(total=30)
                ) as response:
                    
                    if response.status == 200:
                        data = await response.json()
                        self.query_count += 1
                        self.last_request_time = time.time()
                        
                        return data.get('results', {}).get('bindings', [])
                    else:
                        logger.error(f"WIKIDATA query failed with status {response.status}")
                        return []
                        
        except Exception as e:
            logger.error(f"Error executing SPARQL query: {e}")
            raise
            
    async def search_technology_companies(self, 
                                        limit: int = 100) -> List[WikidataEntity]:
        """
        Search for technology companies relevant to 5G supply chain.
        
        Args:
            limit: Maximum number of companies to retrieve
            
        Returns:
            List of WikidataEntity objects
        """
        # SPARQL query to find technology companies
        sparql_query = f"""
        SELECT DISTINCT ?company ?companyLabel ?description ?industry ?industryLabel 
                       ?country ?countryLabel ?founded ?headquarters ?headquartersLabel
                       ?revenue ?employees ?website ?stockSymbol WHERE {{
          ?company wdt:P31/wdt:P279* wd:Q4830453 .  # Instance of business enterprise
          ?company wdt:P452 ?industry .  # Industry
          
          # Filter for technology-related industries
          FILTER(
            ?industry IN (
              wd:Q11650,      # Electronics
              wd:Q18633,      # Information technology  
              wd:Q178038,     # Semiconductor
              wd:Q7397,       # Software
              wd:Q28738,      # Telecommunications
              wd:Q818575      # Consumer electronics
            )
          )
          
          # Optional properties
          OPTIONAL {{ ?company wdt:P17 ?country . }}
          OPTIONAL {{ ?company wdt:P571 ?founded . }}
          OPTIONAL {{ ?company wdt:P159 ?headquarters . }}
          OPTIONAL {{ ?company wdt:P2139 ?revenue . }}
          OPTIONAL {{ ?company wdt:P1128 ?employees . }}
          OPTIONAL {{ ?company wdt:P856 ?website . }}
          OPTIONAL {{ ?company wdt:P414 ?stockSymbol . }}
          
          # Get labels and descriptions
          SERVICE wikibase:label {{ 
            bd:serviceParam wikibase:language "en" . 
            ?company rdfs:label ?companyLabel .
            ?company schema:description ?description .
            ?industry rdfs:label ?industryLabel .
            ?country rdfs:label ?countryLabel .
            ?headquarters rdfs:label ?headquartersLabel .
          }}
          
          # Filter for companies likely in 5G supply chain
          FILTER(REGEX(?companyLabel, "(Apple|Samsung|Qualcomm|ARM|MediaTek|Broadcom|Intel|TSMC|Foxconn|Xiaomi|Nokia|Ericsson)", "i"))
        }}
        ORDER BY ?companyLabel
        LIMIT {limit}
        """
        
        try:
            results = await self.execute_sparql_query(sparql_query)
            entities = []
            
            for result in results:
                # Extract values from SPARQL result
                entity = WikidataEntity(
                    wikidata_id=self._extract_wikidata_id(result.get('company', {})),
                    label=self._extract_value(result.get('companyLabel', {})),
                    description=self._extract_value(result.get('description', {})),
                    industry=self._extract_value(result.get('industryLabel', {})),
                    country=self._extract_value(result.get('countryLabel', {})),
                    founded=self._extract_value(result.get('founded', {})),
                    headquarters=self._extract_value(result.get('headquartersLabel', {})),
                    revenue=self._extract_value(result.get('revenue', {})),
                    employees=self._extract_value(result.get('employees', {})),
                    website=self._extract_value(result.get('website', {})),
                    stock_symbol=self._extract_value(result.get('stockSymbol', {}))
                )
                
                if entity.label:  # Only add entities with valid labels
                    entities.append(entity)
                    
            logger.info(f"Retrieved {len(entities)} technology companies from WIKIDATA")
            self.total_entities_processed += len(entities)
            
            return entities
            
        except Exception as e:
            logger.error(f"Error searching technology companies: {e}")
            return []
            
    async def search_specific_companies(self, 
                                      company_names: List[str]) -> List[WikidataEntity]:
        """
        Search for specific companies by name.
        
        Args:
            company_names: List of company names to search for
            
        Returns:
            List of WikidataEntity objects
        """
        entities = []
        
        for company_name in company_names:
            sparql_query = f"""
            SELECT DISTINCT ?company ?companyLabel ?description ?industry ?industryLabel 
                           ?country ?countryLabel ?founded ?headquarters ?headquartersLabel
                           ?revenue ?employees ?website ?stockSymbol WHERE {{
              ?company wdt:P31/wdt:P279* wd:Q4830453 .  # Instance of business enterprise
              ?company rdfs:label ?companyLabel .
              
              FILTER(REGEX(?companyLabel, "{re.escape(company_name)}", "i"))
              
              # Optional properties
              OPTIONAL {{ ?company wdt:P452 ?industry . }}
              OPTIONAL {{ ?company wdt:P17 ?country . }}
              OPTIONAL {{ ?company wdt:P571 ?founded . }}
              OPTIONAL {{ ?company wdt:P159 ?headquarters . }}
              OPTIONAL {{ ?company wdt:P2139 ?revenue . }}
              OPTIONAL {{ ?company wdt:P1128 ?employees . }}
              OPTIONAL {{ ?company wdt:P856 ?website . }}
              OPTIONAL {{ ?company wdt:P414 ?stockSymbol . }}
              
              # Get labels and descriptions
              SERVICE wikibase:label {{ 
                bd:serviceParam wikibase:language "en" . 
                ?company rdfs:label ?companyLabel .
                ?company schema:description ?description .
                ?industry rdfs:label ?industryLabel .
                ?country rdfs:label ?countryLabel .
                ?headquarters rdfs:label ?headquartersLabel .
              }}
            }}
            LIMIT 5
            """
            
            try:
                results = await self.execute_sparql_query(sparql_query)
                
                for result in results:
                    entity = WikidataEntity(
                        wikidata_id=self._extract_wikidata_id(result.get('company', {})),
                        label=self._extract_value(result.get('companyLabel', {})),
                        description=self._extract_value(result.get('description', {})),
                        industry=self._extract_value(result.get('industryLabel', {})),
                        country=self._extract_value(result.get('countryLabel', {})),
                        founded=self._extract_value(result.get('founded', {})),
                        headquarters=self._extract_value(result.get('headquartersLabel', {})),
                        revenue=self._extract_value(result.get('revenue', {})),
                        employees=self._extract_value(result.get('employees', {})),
                        website=self._extract_value(result.get('website', {})),
                        stock_symbol=self._extract_value(result.get('stockSymbol', {}))
                    )
                    
                    if entity.label:
                        entities.append(entity)
                        
            except Exception as e:
                logger.error(f"Error searching for company '{company_name}': {e}")
                
        logger.info(f"Retrieved {len(entities)} specific companies from WIKIDATA")
        self.total_entities_processed += len(entities)
        
        return entities
        
    async def ingest_to_neo4j(self, 
                            entities: List[WikidataEntity],
                            base_permid: int = 5000000000) -> Dict[str, Any]:
        """
        Ingest WIKIDATA entities into Neo4j graph database.
        
        Args:
            entities: List of WikidataEntity objects to ingest
            base_permid: Base PERMID for generating unique identifiers
            
        Returns:
            Dictionary with ingestion statistics
        """
        if not self.neo4j_client:
            raise ValueError("Neo4j client not configured for ingestion")
            
        # Convert WikidataEntity objects to CompanyEntity objects
        companies = []
        for i, entity in enumerate(entities):
            # Generate synthetic PERMID (in real implementation, would use PermID API)
            permid = base_permid + i
            
            # Calculate match score based on data completeness
            match_score = self._calculate_match_score(entity)
            
            company = entity.to_company_entity(permid, match_score)
            companies.append(company)
            
        # Batch ingest into Neo4j
        try:
            result = await self.neo4j_client.batch_ingest_companies(companies)
            
            logger.info(f"Ingested {result.get('success_count', 0)} companies from WIKIDATA")
            
            return {
                'source': 'WIKIDATA',
                'entities_processed': len(entities),
                'companies_ingested': result.get('success_count', 0),
                'errors': result.get('error_count', 0),
                'status': result.get('status', 'UNKNOWN')
            }
            
        except Exception as e:
            logger.error(f"Error ingesting WIKIDATA entities to Neo4j: {e}")
            return {
                'source': 'WIKIDATA',
                'entities_processed': len(entities),
                'companies_ingested': 0,
                'errors': len(entities),
                'status': 'ERROR',
                'error_message': str(e)
            }
            
    def get_performance_metrics(self) -> Dict[str, Any]:
        """Get performance metrics for monitoring and optimization"""
        return {
            'total_queries_executed': self.query_count,
            'total_entities_processed': self.total_entities_processed,
            'average_entities_per_query': (self.total_entities_processed / self.query_count 
                                         if self.query_count > 0 else 0),
            'request_delay_seconds': self.request_delay
        }
        
    # ===== PRIVATE HELPER METHODS =====
    
    def _extract_wikidata_id(self, uri_data: Dict[str, Any]) -> str:
        """Extract Wikidata ID from URI"""
        uri = uri_data.get('value', '')
        if 'wikidata.org/entity/' in uri:
            return uri.split('/')[-1]
        return ''
        
    def _extract_value(self, data: Dict[str, Any]) -> Optional[str]:
        """Extract value from SPARQL result binding"""
        return data.get('value') if data else None
        
    def _calculate_match_score(self, entity: WikidataEntity) -> float:
        """Calculate match score based on data completeness and quality"""
        score = 0.5  # Base score
        
        # Add score for each available field
        if entity.description: score += 0.1
        if entity.industry: score += 0.1
        if entity.country: score += 0.1
        if entity.founded: score += 0.05
        if entity.headquarters: score += 0.05
        if entity.revenue: score += 0.1
        if entity.employees: score += 0.05
        if entity.website: score += 0.05
        if entity.stock_symbol: score += 0.05
        
        return min(score, 1.0)  # Cap at 1.0


# ===== SAMPLE USAGE AND TESTING =====

async def run_wikidata_ingestion_pipeline():
    """
    Example pipeline for running WIKIDATA ingestion.
    Demonstrates the complete ETL process from external API to Neo4j.
    """
    # Initialize clients
    async with Neo4jClient() as neo4j_client:
        async with WikidataIngester(neo4j_client) as wikidata_client:
            
            logger.info("Starting WIKIDATA ingestion pipeline...")
            
            # Step 1: Search for technology companies
            logger.info("Searching for technology companies...")
            tech_companies = await wikidata_client.search_technology_companies(limit=50)
            
            # Step 2: Search for specific 5G supply chain companies
            specific_companies = [
                "Apple Inc", "Samsung Electronics", "Qualcomm", "ARM Holdings",
                "MediaTek", "Broadcom", "Intel", "TSMC", "Foxconn", "Xiaomi"
            ]
            
            logger.info("Searching for specific supply chain companies...")
            specific_entities = await wikidata_client.search_specific_companies(specific_companies)
            
            # Step 3: Combine and deduplicate entities
            all_entities = tech_companies + specific_entities
            unique_entities = []
            seen_labels = set()
            
            for entity in all_entities:
                if entity.label not in seen_labels:
                    unique_entities.append(entity)
                    seen_labels.add(entity.label)
                    
            logger.info(f"Found {len(unique_entities)} unique entities")
            
            # Step 4: Ingest into Neo4j
            if unique_entities:
                ingestion_result = await wikidata_client.ingest_to_neo4j(unique_entities)
                logger.info(f"Ingestion result: {ingestion_result}")
                
                # Step 5: Create graph projection for analytics
                projection_result = await neo4j_client.create_graph_projection()
                logger.info(f"Graph projection result: {projection_result}")
                
                # Step 6: Get final statistics
                stats = await neo4j_client.get_ingestion_statistics()
                logger.info(f"Final graph statistics: {stats}")
                
                # Step 7: Get performance metrics
                performance = wikidata_client.get_performance_metrics()
                logger.info(f"WIKIDATA client performance: {performance}")
                
                return {
                    'ingestion_result': ingestion_result,
                    'projection_result': projection_result,
                    'graph_statistics': stats,
                    'performance_metrics': performance
                }
            else:
                logger.warning("No entities found to ingest")
                return {'status': 'NO_DATA'}


if __name__ == "__main__":
    # Run the ingestion pipeline
    result = asyncio.run(run_wikidata_ingestion_pipeline())
    print(f"Pipeline completed with result: {json.dumps(result, indent=2, default=str)}")