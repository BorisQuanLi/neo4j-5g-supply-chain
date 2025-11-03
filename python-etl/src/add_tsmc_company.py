"""
Add TSMC (Taiwan Semiconductor Manufacturing Company) to the 5G Supply Chain Graph

TSMC is a critical player in the semiconductor supply chain, manufacturing chips for
Apple, NVIDIA, AMD, and other major technology companies.
"""

import asyncio
from neo4j_client import Neo4jClient, CompanyEntity

async def add_tsmc_to_graph():
    """Add TSMC as a company node with relationships to major customers"""
    
    # TSMC company data based on Yahoo Finance (TSM)
    tsmc = CompanyEntity(
        permid=4295908004,  # Synthetic PERMID
        name="Taiwan Semiconductor Manufacturing Company",
        is_final_assembler=False,  # TSMC is a foundry, not final assembler
        match_score=0.95,
        industry_sector="Semiconductors",
        country="Taiwan",
        market_cap=500000000000,  # ~500B USD market cap
        revenue=70000000000  # ~70B USD revenue
    )
    
    async with Neo4jClient() as client:
        # Ingest TSMC
        result = await client.ingest_company_entity(tsmc)
        print(f"TSMC ingestion result: {result}")
        
        # Create supply relationships (TSMC supplies chips to these companies)
        supply_relationships = [
            ("Taiwan Semiconductor Manufacturing Company", "Apple Inc"),
            ("Taiwan Semiconductor Manufacturing Company", "NVIDIA Corporation"),
            ("Taiwan Semiconductor Manufacturing Company", "AMD Inc"),
            ("Taiwan Semiconductor Manufacturing Company", "Broadcom Inc")
        ]
        
        relationship_result = await client.create_supply_chain_relationships(supply_relationships)
        print(f"Supply relationships result: {relationship_result}")
        
        # Add TSMC node properties for financial services integration
        await client.execute_query("""
            MATCH (c:Company {name: 'Taiwan Semiconductor Manufacturing Company'})
            SET c.stock_symbol = 'TSM',
                c.exchange = 'NYSE',
                c.sector = 'Technology',
                c.subsector = 'Semiconductor Foundry',
                c.founded_year = 1987,
                c.employees = 73000
        """)
        
        # Add ARM Holdings node properties
        await client.execute_query("""
            MATCH (c:Company {name: 'ARM Holdings'})
            SET c.stock_symbol = 'ARM',
                c.exchange = 'NASDAQ',
                c.founded_year = 1990,
                c.employees = 6500,
                c.business_model = 'IP_Licensing'
        """)
        
        # Create TSMC-ARM manufacturing relationship with properties
        await client.execute_query("""
            MATCH (tsmc:Company {name: 'Taiwan Semiconductor Manufacturing Company'})
            MATCH (arm:Company {name: 'ARM Holdings'})
            MERGE (tsmc)-[r:MANUFACTURES_DESIGNS_FOR]->(arm)
            SET r.relationship_type = 'foundry_customer',
                r.process_nodes = ['3nm', '5nm', '7nm'],
                r.annual_volume_millions = 500,
                r.partnership_since = 2010,
                r.strategic_importance = 'HIGH'
        """)
        
        return {"tsmc_added": True, "relationships_created": len(supply_relationships) + 1}

if __name__ == "__main__":
    result = asyncio.run(add_tsmc_to_graph())
    print(f"TSMC addition completed: {result}")