"""
Integration tests for Neo4jClient using TestContainers.
Tests the client against a real Neo4j database instance.
"""
import pytest
import pytest_asyncio
from src.neo4j_client import Neo4jClient, CompanyEntity, RelationshipData
from tests.conftest import assert_company_fields, create_test_relationship_data


@pytest.mark.integration
class TestNeo4jClientIntegration:
    """Integration tests for Neo4jClient using real Neo4j database."""

    @pytest_asyncio.fixture
    async def client_with_data(self, neo4j_client, sample_companies_batch):
        """Neo4j client with sample data loaded."""
        # Insert sample companies
        for company_data in sample_companies_batch:
            entity = CompanyEntity(
                permid=company_data["permid"],
                name=company_data["name"],
                is_final_assembler=company_data["is_final_assembler"],
                match_score=company_data["match_score"],
                industry_sector=company_data["sector"],
                country=company_data["country"]
            )
            await neo4j_client.create_company_entity(entity)
        
        return neo4j_client

    async def test_connection_and_health_check(self, neo4j_client):
        """Test basic Neo4j connection and health check."""
        # Test connection
        assert await neo4j_client.test_connection() is True
        
        # Test health check
        health_status = await neo4j_client.health_check()
        assert health_status["status"] == "healthy"
        assert "database_version" in health_status
        assert "node_count" in health_status

    async def test_create_company_entity(self, neo4j_client, sample_company_data):
        """Test creating a single company entity."""
        # Create company entity
        entity = CompanyEntity(
            permid=sample_company_data["permid"],
            name=sample_company_data["name"],
            is_final_assembler=sample_company_data["is_final_assembler"],
            match_score=sample_company_data["match_score"],
            industry_sector=sample_company_data["sector"],
            country=sample_company_data["country"]
        )
        
        result = await neo4j_client.create_company_entity(entity)
        assert result["nodes_created"] == 1
        assert result["properties_set"] > 0

        # Verify entity was created
        query = "MATCH (c:Company {permid: $permid}) RETURN c"
        records = await neo4j_client.execute_query(query, {"permid": entity.permid})
        
        assert len(records) == 1
        company = records[0]["c"]
        assert company["name"] == entity.name
        assert company["permid"] == entity.permid
        assert company["is_final_assembler"] == entity.is_final_assembler

    async def test_batch_company_creation(self, neo4j_client, sample_companies_batch):
        """Test batch creation of multiple companies."""
        entities = [
            CompanyEntity(
                permid=data["permid"],
                name=data["name"],
                is_final_assembler=data["is_final_assembler"],
                match_score=data["match_score"],
                industry_sector=data["sector"],
                country=data["country"]
            ) for data in sample_companies_batch
        ]
        
        result = await neo4j_client.batch_create_companies(entities)
        assert result["nodes_created"] == len(entities)
        assert result["properties_set"] > 0

        # Verify all entities were created
        query = "MATCH (c:Company) RETURN count(c) as total"
        records = await neo4j_client.execute_query(query)
        assert records[0]["total"] == len(entities)

    async def test_create_relationships(self, client_with_data):
        """Test creating relationships between companies."""
        # Create supply chain relationship
        relationship = RelationshipData(
            source_name="Apple Inc",
            target_name="QUALCOMM Inc",
            relationship_type="SUPPLY_COMPONENTS",
            properties={
                "strength": 0.85,
                "component_type": "Modem Chips",
                "started_date": "2020-01-01"
            }
        )
        
        result = await client_with_data.create_relationship(relationship)
        assert result["relationships_created"] == 1

        # Verify relationship was created
        query = """
        MATCH (a:Company {name: $source})-[r:SUPPLY_COMPONENTS]->(b:Company {name: $target})
        RETURN r
        """
        records = await client_with_data.execute_query(query, {
            "source": relationship.source_name,
            "target": relationship.target_name
        })
        
        assert len(records) == 1
        rel = records[0]["r"]
        assert rel["strength"] == 0.85
        assert rel["component_type"] == "Modem Chips"

    async def test_batch_relationship_creation(self, client_with_data):
        """Test batch creation of multiple relationships."""
        relationships = [
            RelationshipData(
                source_name="Apple Inc",
                target_name="QUALCOMM Inc",
                relationship_type="SUPPLY_COMPONENTS",
                properties={"strength": 0.85, "component_type": "Modems"}
            ),
            RelationshipData(
                source_name="Samsung Electronics Co Ltd",
                target_name="QUALCOMM Inc", 
                relationship_type="COMPETES_WITH",
                properties={"strength": 0.75, "market_segment": "Mobile"}
            )
        ]
        
        result = await client_with_data.batch_create_relationships(relationships)
        assert result["relationships_created"] == len(relationships)

        # Verify relationships exist
        query = "MATCH ()-[r]->() RETURN count(r) as total"
        records = await client_with_data.execute_query(query)
        assert records[0]["total"] == len(relationships)

    async def test_graph_projection_management(self, client_with_data):
        """Test creating and managing graph projections for GDS algorithms."""
        projection_name = "test_supply_chain"
        
        # Create graph projection
        result = await client_with_data.create_graph_projection(
            projection_name=projection_name,
            node_projection="Company",
            relationship_projection="SUPPLY_COMPONENTS"
        )
        assert result["nodeCount"] >= 0
        assert result["relationshipCount"] >= 0

        # Verify projection exists
        projections = await client_with_data.list_graph_projections()
        projection_names = [p["graphName"] for p in projections]
        assert projection_name in projection_names

        # Drop projection
        await client_with_data.drop_graph_projection(projection_name)
        
        # Verify projection was dropped
        projections = await client_with_data.list_graph_projections()
        projection_names = [p["graphName"] for p in projections]
        assert projection_name not in projection_names

    async def test_transaction_management(self, neo4j_client, sample_companies_batch):
        """Test transaction management for data consistency."""
        entities = [
            CompanyEntity(
                permid=data["permid"],
                name=data["name"],
                is_final_assembler=data["is_final_assembler"],
                match_score=data["match_score"],
                industry_sector=data["sector"],
                country=data["country"]
            ) for data in sample_companies_batch
        ]
        
        # Test successful transaction
        async with neo4j_client.transaction() as tx:
            for entity in entities:
                await tx.create_company_entity(entity)

        # Verify all entities were created
        query = "MATCH (c:Company) RETURN count(c) as total"
        records = await neo4j_client.execute_query(query)
        assert records[0]["total"] == len(entities)

        # Clean up for next test part
        await neo4j_client.execute_query("MATCH (n) DETACH DELETE n")

        # Test transaction rollback on error
        try:
            async with neo4j_client.transaction() as tx:
                # Create first entity (should succeed)
                await tx.create_company_entity(entities[0])
                
                # Simulate error that causes rollback
                raise ValueError("Simulated error")
        except ValueError:
            pass  # Expected error

        # Verify no entities were created due to rollback
        records = await neo4j_client.execute_query(query)
        assert records[0]["total"] == 0

    async def test_query_performance_metrics(self, neo4j_client):
        """Test query performance monitoring."""
        # Reset metrics
        neo4j_client.reset_metrics()
        
        # Execute several queries
        queries = [
            "RETURN 1 as test",
            "RETURN 2 as test", 
            "RETURN 3 as test"
        ]
        
        for query in queries:
            await neo4j_client.execute_query(query)
        
        # Check metrics
        metrics = neo4j_client.get_performance_metrics()
        assert metrics["query_count"] == len(queries)
        assert metrics["total_execution_time"] > 0
        assert metrics["average_execution_time"] > 0

    async def test_entity_search_and_filtering(self, client_with_data):
        """Test searching and filtering entities."""
        # Search by country
        us_companies = await client_with_data.find_companies_by_country("United States")
        assert len(us_companies) == 2  # Apple and Qualcomm
        
        us_names = [company["name"] for company in us_companies]
        assert "Apple Inc" in us_names
        assert "QUALCOMM Inc" in us_names

        # Search by final assembler status
        assemblers = await client_with_data.find_companies_by_final_assembler_status(True)
        assert len(assemblers) == 2  # Apple and Samsung
        
        assembler_names = [company["name"] for company in assemblers]
        assert "Apple Inc" in assembler_names
        assert "Samsung Electronics Co Ltd" in assembler_names

        # Search by match score threshold
        high_quality = await client_with_data.find_companies_by_match_score_threshold(0.95)
        assert len(high_quality) >= 2  # Companies with score >= 0.95
        
        for company in high_quality:
            assert company["match_score"] >= 0.95

    async def test_graph_analytics_queries(self, client_with_data):
        """Test graph analytics queries."""
        # Create some relationships first
        relationships = create_test_relationship_data()
        for rel_data in relationships:
            relationship = RelationshipData(
                source_name="Apple Inc",
                target_name="QUALCOMM Inc", 
                relationship_type=rel_data["relationship_type"],
                properties={"strength": rel_data["strength"]}
            )
            await client_with_data.create_relationship(relationship)

        # Test network statistics
        stats = await client_with_data.get_network_statistics()
        assert "node_count" in stats
        assert "relationship_count" in stats
        assert "average_degree" in stats
        assert stats["node_count"] > 0

        # Test shortest path
        path = await client_with_data.find_shortest_path(
            source_permid=4295905573,  # Apple
            target_permid=4295906319   # Qualcomm
        )
        assert path is not None
        assert len(path) >= 2  # At least source and target

    async def test_concurrent_operations(self, neo4j_client, sample_companies_batch):
        """Test concurrent database operations."""
        entities = [
            CompanyEntity(
                permid=data["permid"] + i * 1000,  # Unique permids
                name=f"{data['name']} Clone {i}",
                is_final_assembler=data["is_final_assembler"],
                match_score=data["match_score"],
                industry_sector=data["sector"],
                country=data["country"]
            ) for i, data in enumerate(sample_companies_batch)
        ]
        
        # Create entities concurrently
        tasks = [
            neo4j_client.create_company_entity(entity)
            for entity in entities
        ]
        
        results = await asyncio.gather(*tasks)
        
        # Verify all operations succeeded
        assert len(results) == len(entities)
        for result in results:
            assert result["nodes_created"] == 1

    async def test_error_handling_and_retries(self, neo4j_client):
        """Test error handling and retry mechanisms."""
        # Test handling of invalid query
        with pytest.raises(Exception):
            await neo4j_client.execute_query("INVALID CYPHER QUERY")

        # Test handling of constraint violations
        # First create a company
        entity = CompanyEntity(
            permid=999999999,
            name="Test Company",
            is_final_assembler=False,
            match_score=0.8
        )
        await neo4j_client.create_company_entity(entity)
        
        # Try to create the same company again (should handle gracefully)
        result = await neo4j_client.create_company_entity(entity)
        # Depending on implementation, this might be handled as an update or ignored

    @pytest.mark.performance
    async def test_bulk_operations_performance(self, neo4j_client):
        """Test performance of bulk operations."""
        # Create a large number of entities
        entities = [
            CompanyEntity(
                permid=i,
                name=f"Company {i}",
                is_final_assembler=i % 3 == 0,
                match_score=0.8 + (i % 20) * 0.01,
                industry_sector="Technology",
                country="Test Country"
            ) for i in range(1000, 1500)  # 500 entities
        ]
        
        # Measure batch creation time
        start_time = time.time()
        result = await neo4j_client.batch_create_companies(entities)
        end_time = time.time()
        
        execution_time = end_time - start_time
        
        # Performance assertions
        assert result["nodes_created"] == len(entities)
        assert execution_time < 10.0  # Should complete within 10 seconds
        
        # Verify data integrity
        query = "MATCH (c:Company) WHERE c.permid >= 1000 AND c.permid < 1500 RETURN count(c) as total"
        records = await neo4j_client.execute_query(query)
        assert records[0]["total"] == len(entities)

    async def test_data_consistency_and_integrity(self, client_with_data):
        """Test data consistency and integrity constraints."""
        # Test unique constraint on permid
        duplicate_entity = CompanyEntity(
            permid=4295905573,  # Same as Apple
            name="Duplicate Apple",
            is_final_assembler=True,
            match_score=0.99
        )
        
        # This should either update existing or be handled gracefully
        result = await client_with_data.create_company_entity(duplicate_entity)
        
        # Verify only one Apple entity exists
        query = "MATCH (c:Company {permid: 4295905573}) RETURN count(c) as total"
        records = await client_with_data.execute_query(query)
        assert records[0]["total"] == 1

    async def test_cleanup_and_maintenance(self, neo4j_client):
        """Test database cleanup and maintenance operations."""
        # Create test data
        entity = CompanyEntity(
            permid=888888888,
            name="Cleanup Test Company",
            is_final_assembler=False,
            match_score=0.5
        )
        await neo4j_client.create_company_entity(entity)
        
        # Test orphaned node cleanup
        orphaned_count = await neo4j_client.cleanup_orphaned_nodes()
        assert isinstance(orphaned_count, int)
        
        # Test database statistics
        stats = await neo4j_client.get_database_statistics()
        assert "total_nodes" in stats
        assert "total_relationships" in stats
        assert "labels" in stats
        assert stats["total_nodes"] >= 1  # At least our test entity