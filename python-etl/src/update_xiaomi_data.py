"""
Update dataset: Replace Huawei with Xiaomi based on Q4 2023-Q2 2025 market share data
"""

import asyncio
from neo4j_client import Neo4jClient, CompanyEntity, RelationshipData

async def update_to_xiaomi():
    """Replace Huawei with Xiaomi and add current market properties"""
    
    async with Neo4jClient() as client:
        # Remove Huawei if exists
        await client.execute_query("MATCH (c:Company {name: 'Huawei'}) DETACH DELETE c")
        
        # Add Xiaomi with current market data
        xiaomi = CompanyEntity(
            permid=4295908005,
            name="Xiaomi Corporation",
            is_final_assembler=True,
            match_score=0.88,
            industry_sector="Technology",
            country="China",
            market_cap=45000000000,
            revenue=42000000000
        )
        
        result = await client.ingest_company_entity(xiaomi)
        
        # Add Xiaomi properties
        await client.execute_query("""
            MATCH (c:Company {name: 'Xiaomi Corporation'})
            SET c.stock_symbol = '1810.HK',
                c.exchange = 'HKEX',
                c.founded_year = 2010,
                c.market_share_2024 = 0.14,
                c.global_rank = 3
        """)
        
        # Create Xiaomi relationships
        relationships = [
            ("Qualcomm Inc", "Xiaomi Corporation"),
            ("MediaTek", "Xiaomi Corporation"),
            ("Foxconn", "Xiaomi Corporation")
        ]
        
        await client.create_supply_chain_relationships(relationships)
        
        # Add competition with Samsung
        competition = RelationshipData(
            "Samsung Electronics Co", "Xiaomi Corporation", "COMPETES_WITH",
            {"strength": 0.8, "market_segment": "smartphones", "geographic_focus": "global"}
        )
        
        await client.create_relationship(competition)
        
        return {"xiaomi_updated": True, "huawei_removed": True}

if __name__ == "__main__":
    result = asyncio.run(update_to_xiaomi())
    print(f"Dataset updated: {result}")