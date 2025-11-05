"""
Unit tests for Neo4jClient using mocks.
Tests business logic without requiring a real database.
"""
import pytest
import pytest_asyncio
from unittest.mock import AsyncMock, Mock, patch, MagicMock
from src.neo4j_client import Neo4jClient, CompanyEntity, RelationshipData


@pytest.mark.unit
class TestNeo4jClientUnit:
    """Unit tests for Neo4jClient using mocks."""

    @pytest_asyncio.fixture
    async def mock_client(self):
        """Create a Neo4jClient with mocked driver."""
        with patch('src.neo4j_client.AsyncGraphDatabase') as mock_graph_db:
            mock_driver = AsyncMock()
            mock_session = AsyncMock()
            mock_graph_db.driver.return_value = mock_driver
            mock_driver.session.return_value = mock_session
            
            client = Neo4jClient(
                uri="bolt://test:7687",
                username="test_user",
                password="test_pass"
            )
            client.driver = mock_driver
            client.session = mock_session
            
            yield client

    def test_client_initialization(self):
        """Test Neo4jClient initialization with various parameters."""
        # Test with explicit parameters
        client = Neo4jClient(
            uri="bolt://localhost:7687",
            username="neo4j", 
            password="password",
            max_connection_pool_size=100
        )
        
        assert client.uri == "bolt://localhost:7687"
        assert client.username == "neo4j"
        assert client.password == "password"
        assert client.max_pool_size == 100
        assert client.query_count == 0
        assert client.total_execution_time == 0.0

    @patch.dict('os.environ', {
        'NEO4J_URI': 'bolt://env-host:7687',
        'NEO4J_USERNAME': 'env_user',
        'NEO4J_PASSWORD': 'env_pass'
    })
    def test_client_initialization_from_env(self):
        """Test Neo4jClient initialization from environment variables."""
        client = Neo4jClient()
        
        assert client.uri == "bolt://env-host:7687"
        assert client.username == "env_user"
        assert client.password == "env_pass"

    async def test_create_company_entity(self, mock_client):
        """Test creating a company entity."""
        # Mock the session response
        mock_result = Mock()
        mock_result.consume.return_value = Mock(counters=Mock(nodes_created=1, properties_set=5))
        mock_client.session.run.return_value = mock_result
        
        entity = CompanyEntity(
            permid=4295905573,
            name="Apple Inc",
            is_final_assembler=True,
            match_score=0.98,
            industry_sector="Technology",
            country="United States"
        )
        
        result = await mock_client.create_company_entity(entity)
        
        # Verify the result
        assert result["nodes_created"] == 1
        assert result["properties_set"] == 5
        
        # Verify the correct query was called
        mock_client.session.run.assert_called_once()
        call_args = mock_client.session.run.call_args
        assert "CREATE" in call_args[0][0] or "MERGE" in call_args[0][0]
        assert call_args[1]["permid"] == entity.permid

    async def test_batch_create_companies(self, mock_client):
        """Test batch creation of companies."""
        # Mock the session response
        mock_result = Mock()
        mock_result.consume.return_value = Mock(counters=Mock(nodes_created=3, properties_set=15))
        mock_client.session.run.return_value = mock_result
        
        entities = [
            CompanyEntity(permid=i, name=f"Company {i}", is_final_assembler=i % 2 == 0, match_score=0.8)
            for i in range(1, 4)
        ]
        
        result = await mock_client.batch_create_companies(entities)
        
        # Verify the result
        assert result["nodes_created"] == 3
        assert result["properties_set"] == 15
        
        # Verify batch query was called
        mock_client.session.run.assert_called_once()

    async def test_create_relationship(self, mock_client):
        """Test creating a relationship between companies."""
        # Mock the session response
        mock_result = Mock()
        mock_result.consume.return_value = Mock(counters=Mock(relationships_created=1, properties_set=3))
        mock_client.session.run.return_value = mock_result
        
        relationship = RelationshipData(
            source_name="Apple Inc",
            target_name="QUALCOMM Inc",
            relationship_type="SUPPLY_COMPONENTS",
            properties={"strength": 0.85, "component_type": "Modems"}
        )
        
        result = await mock_client.create_relationship(relationship)
        
        # Verify the result
        assert result["relationships_created"] == 1
        assert result["properties_set"] == 3
        
        # Verify relationship creation query
        mock_client.session.run.assert_called_once()
        call_args = mock_client.session.run.call_args
        assert "MATCH" in call_args[0][0]
        assert "CREATE" in call_args[0][0] or "MERGE" in call_args[0][0]
        assert relationship.relationship_type in call_args[0][0]

    async def test_execute_query_with_parameters(self, mock_client):
        """Test executing a parameterized query."""
        # Mock the session response
        mock_record = Mock()
        mock_record.__getitem__ = Mock(return_value="test_value")
        mock_result = Mock()
        mock_result.data.return_value = [{"test_key": "test_value"}]
        mock_client.session.run.return_value = mock_result
        
        query = "MATCH (c:Company {permid: $permid}) RETURN c"
        parameters = {"permid": 4295905573}
        
        result = await mock_client.execute_query(query, parameters)
        
        # Verify the query was executed
        mock_client.session.run.assert_called_once_with(query, parameters)
        assert len(result) == 1
        assert result[0]["test_key"] == "test_value"

    async def test_transaction_context_manager(self, mock_client):
        """Test transaction context manager."""
        mock_tx = AsyncMock()
        mock_client.session.begin_transaction.return_value = mock_tx
        
        async with mock_client.transaction() as tx:
            assert tx == mock_tx
            
        # Verify transaction lifecycle
        mock_client.session.begin_transaction.assert_called_once()
        mock_tx.commit.assert_called_once()

    async def test_transaction_rollback_on_exception(self, mock_client):
        """Test transaction rollback when exception occurs."""
        mock_tx = AsyncMock()
        mock_client.session.begin_transaction.return_value = mock_tx
        
        try:
            async with mock_client.transaction() as tx:
                raise ValueError("Test exception")
        except ValueError:
            pass
            
        # Verify transaction was rolled back
        mock_tx.rollback.assert_called_once()
        mock_tx.commit.assert_not_called()

    async def test_graph_projection_management(self, mock_client):
        """Test graph projection creation and management."""
        # Mock projection creation response
        mock_result = Mock()
        mock_result.single.return_value = {"nodeCount": 100, "relationshipCount": 250}
        mock_client.session.run.return_value = mock_result
        
        result = await mock_client.create_graph_projection(
            projection_name="test_projection",
            node_projection="Company",
            relationship_projection="SUPPLY_COMPONENTS"
        )
        
        assert result["nodeCount"] == 100
        assert result["relationshipCount"] == 250
        
        # Verify GDS projection query
        mock_client.session.run.assert_called()
        call_args = mock_client.session.run.call_args
        assert "gds.graph.project" in call_args[0][0]

    async def test_health_check(self, mock_client):
        """Test database health check."""
        # Mock health check responses
        mock_result = Mock()
        mock_result.single.return_value = {"version": "5.15.0"}
        mock_client.session.run.return_value = mock_result
        
        health_status = await mock_client.health_check()
        
        assert health_status["status"] == "healthy"
        assert "database_version" in health_status
        assert health_status["database_version"] == "5.15.0"

    async def test_performance_metrics_tracking(self, mock_client):
        """Test performance metrics tracking."""
        # Mock query execution
        mock_result = Mock()
        mock_result.data.return_value = []
        mock_client.session.run.return_value = mock_result
        
        # Execute some queries
        await mock_client.execute_query("RETURN 1")
        await mock_client.execute_query("RETURN 2") 
        await mock_client.execute_query("RETURN 3")
        
        # Check metrics
        metrics = mock_client.get_performance_metrics()
        assert metrics["query_count"] == 3
        assert metrics["total_execution_time"] >= 0

    def test_metrics_reset(self, mock_client):
        """Test resetting performance metrics."""
        # Set some initial metrics
        mock_client.query_count = 10
        mock_client.total_execution_time = 5.5
        
        # Reset metrics
        mock_client.reset_metrics()
        
        assert mock_client.query_count == 0
        assert mock_client.total_execution_time == 0.0

    async def test_error_handling(self, mock_client):
        """Test error handling in various scenarios."""
        from neo4j.exceptions import ServiceUnavailable, TransientError
        
        # Test ServiceUnavailable error
        mock_client.session.run.side_effect = ServiceUnavailable("Connection lost")
        
        with pytest.raises(ServiceUnavailable):
            await mock_client.execute_query("RETURN 1")

        # Test TransientError (should be retried in real implementation)
        mock_client.session.run.side_effect = TransientError("Temporary error")
        
        with pytest.raises(TransientError):
            await mock_client.execute_query("RETURN 1")

    async def test_connection_management(self, mock_client):
        """Test connection lifecycle management."""
        # Test initialization
        await mock_client.initialize()
        assert mock_client.driver is not None
        
        # Test connection test
        mock_client.driver.verify_connectivity = AsyncMock(return_value=True)
        connection_ok = await mock_client.test_connection()
        assert connection_ok is True
        
        # Test close
        await mock_client.close()
        mock_client.driver.close.assert_called_once()

    async def test_find_companies_by_criteria(self, mock_client):
        """Test finding companies by various criteria."""
        # Mock search results
        mock_result = Mock()
        mock_result.data.return_value = [
            {"c": {"permid": 1, "name": "Company 1", "country": "United States"}},
            {"c": {"permid": 2, "name": "Company 2", "country": "United States"}}
        ]
        mock_client.session.run.return_value = mock_result
        
        # Test find by country
        companies = await mock_client.find_companies_by_country("United States")
        assert len(companies) == 2
        assert all(company["country"] == "United States" for company in companies)
        
        # Test find by final assembler status
        companies = await mock_client.find_companies_by_final_assembler_status(True)
        assert len(companies) == 2
        
        # Test find by match score threshold
        companies = await mock_client.find_companies_by_match_score_threshold(0.9)
        assert len(companies) == 2

    async def test_network_statistics(self, mock_client):
        """Test network statistics calculation."""
        # Mock statistics results
        mock_result = Mock()
        mock_result.single.return_value = {
            "node_count": 100,
            "relationship_count": 250,
            "average_degree": 5.0,
            "density": 0.025
        }
        mock_client.session.run.return_value = mock_result
        
        stats = await mock_client.get_network_statistics()
        
        assert stats["node_count"] == 100
        assert stats["relationship_count"] == 250
        assert stats["average_degree"] == 5.0
        assert stats["density"] == 0.025

    async def test_shortest_path_finding(self, mock_client):
        """Test shortest path finding between entities."""
        # Mock path result
        mock_result = Mock()
        mock_result.data.return_value = [
            {
                "path": [
                    {"permid": 1, "name": "Company A"},
                    {"permid": 2, "name": "Company B"}
                ],
                "length": 2
            }
        ]
        mock_client.session.run.return_value = mock_result
        
        path = await mock_client.find_shortest_path(
            source_permid=1,
            target_permid=2
        )
        
        assert len(path) == 2
        assert path[0]["permid"] == 1
        assert path[1]["permid"] == 2

    async def test_input_validation(self, mock_client):
        """Test input validation for various methods."""
        # Test invalid entity creation
        with pytest.raises((ValueError, TypeError)):
            invalid_entity = CompanyEntity(
                permid=None,  # Invalid permid
                name="",      # Empty name
                is_final_assembler=True,
                match_score=0.8
            )
            await mock_client.create_company_entity(invalid_entity)

        # Test invalid relationship creation
        with pytest.raises((ValueError, TypeError)):
            invalid_relationship = RelationshipData(
                source_name="",  # Empty source
                target_name="Target",
                relationship_type="",  # Empty type
                properties={}
            )
            await mock_client.create_relationship(invalid_relationship)

    def test_data_model_validation(self):
        """Test data model classes validation."""
        # Test valid CompanyEntity
        entity = CompanyEntity(
            permid=4295905573,
            name="Apple Inc",
            is_final_assembler=True,
            match_score=0.98,
            industry_sector="Technology",
            country="United States",
            market_cap=3000000000000,
            revenue=394328000000
        )
        
        assert entity.permid == 4295905573
        assert entity.name == "Apple Inc"
        assert entity.is_final_assembler is True
        assert entity.match_score == 0.98
        assert entity.industry_sector == "Technology"
        assert entity.country == "United States"
        assert entity.market_cap == 3000000000000
        assert entity.revenue == 394328000000

        # Test valid RelationshipData
        relationship = RelationshipData(
            source_name="Apple Inc",
            target_name="QUALCOMM Inc",
            relationship_type="SUPPLY_COMPONENTS",
            properties={"strength": 0.85, "component_type": "Modems"}
        )
        
        assert relationship.source_name == "Apple Inc"
        assert relationship.target_name == "QUALCOMM Inc"
        assert relationship.relationship_type == "SUPPLY_COMPONENTS"
        assert relationship.properties["strength"] == 0.85
        assert relationship.properties["component_type"] == "Modems"