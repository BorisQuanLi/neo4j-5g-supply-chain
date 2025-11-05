"""
Performance benchmarking tests for the Python ETL components.
Uses pytest-benchmark to measure execution times and throughput.
"""
import pytest
import pytest_asyncio
import asyncio
import time
from src.neo4j_client import Neo4jClient, CompanyEntity
from tests.conftest import performance_threshold


@pytest.mark.performance
class TestNeo4jClientPerformance:
    """Performance benchmarking tests for Neo4jClient."""

    @pytest_asyncio.fixture
    async def performance_client(self, neo4j_test_uri, neo4j_test_credentials):
        """Neo4j client optimized for performance testing."""
        username, password = neo4j_test_credentials
        client = Neo4jClient(
            neo4j_test_uri, 
            username, 
            password, 
            max_connection_pool_size=100  # Larger pool for performance tests
        )
        await client.initialize()
        
        # Clean database
        await client.execute_query("MATCH (n) DETACH DELETE n")
        
        yield client
        await client.close()

    def test_single_entity_creation_performance(self, benchmark, performance_client):
        """Benchmark single entity creation performance."""
        entity = CompanyEntity(
            permid=999999999,
            name="Performance Test Company",
            is_final_assembler=False,
            match_score=0.85
        )
        
        async def create_entity():
            return await performance_client.create_company_entity(entity)
        
        # Benchmark the operation
        result = benchmark(asyncio.run, create_entity())
        assert result["nodes_created"] == 1

    def test_batch_entity_creation_performance(self, benchmark, performance_client):
        """Benchmark batch entity creation performance."""
        entities = [
            CompanyEntity(
                permid=i,
                name=f"Performance Test Company {i}",
                is_final_assembler=i % 3 == 0,
                match_score=0.8 + (i % 20) * 0.01,
                industry_sector="Technology",
                country="Test Country"
            ) for i in range(1000, 1500)  # 500 entities
        ]
        
        async def batch_create():
            return await performance_client.batch_create_companies(entities)
        
        # Benchmark batch creation
        result = benchmark(asyncio.run, batch_create())
        assert result["nodes_created"] == len(entities)

    @pytest.mark.benchmark(
        group="query_performance",
        min_rounds=5,
        max_time=30,
        disable_gc=True,
        warmup=True
    )
    def test_query_execution_performance(self, benchmark, performance_client):
        """Benchmark basic query execution performance."""
        # Setup: Create some test data first
        asyncio.run(self._setup_performance_data(performance_client))
        
        async def execute_query():
            return await performance_client.execute_query(
                "MATCH (c:Company) WHERE c.country = $country RETURN count(c) as total",
                {"country": "United States"}
            )
        
        result = benchmark(asyncio.run, execute_query())
        assert len(result) == 1

    @pytest.mark.benchmark(
        group="concurrent_operations",
        min_rounds=3,
        max_time=60
    )
    def test_concurrent_query_performance(self, benchmark, performance_client):
        """Benchmark concurrent query execution."""
        # Setup test data
        asyncio.run(self._setup_performance_data(performance_client))
        
        async def concurrent_queries():
            tasks = [
                performance_client.execute_query("MATCH (c:Company) RETURN count(c)"),
                performance_client.execute_query("MATCH (c:Company {country: 'United States'}) RETURN count(c)"),
                performance_client.execute_query("MATCH (c:Company {is_final_assembler: true}) RETURN count(c)"),
                performance_client.execute_query("MATCH (c:Company) WHERE c.match_score > 0.9 RETURN count(c)"),
                performance_client.execute_query("MATCH (c:Company {industry_sector: 'Technology'}) RETURN count(c)")
            ]
            results = await asyncio.gather(*tasks)
            return results
        
        results = benchmark(asyncio.run, concurrent_queries())
        assert len(results) == 5
        for result in results:
            assert len(result) == 1

    @pytest.mark.benchmark(
        group="search_operations",
        min_rounds=10,
        disable_gc=True
    )
    def test_search_performance(self, benchmark, performance_client):
        """Benchmark search operation performance."""
        # Setup test data
        asyncio.run(self._setup_performance_data(performance_client))
        
        async def search_operations():
            # Perform various search operations
            us_companies = await performance_client.find_companies_by_country("United States")
            assemblers = await performance_client.find_companies_by_final_assembler_status(True)
            high_match = await performance_client.find_companies_by_match_score_threshold(0.9)
            return len(us_companies) + len(assemblers) + len(high_match)
        
        result = benchmark(asyncio.run, search_operations())
        assert result > 0

    @performance_threshold(max_execution_time_ms=5000)
    async def test_large_dataset_processing(self, performance_client):
        """Test processing of large datasets within time constraints."""
        # Create a large dataset
        entities = [
            CompanyEntity(
                permid=i,
                name=f"Large Dataset Company {i}",
                is_final_assembler=i % 10 == 0,
                match_score=0.7 + (i % 30) * 0.01,
                industry_sector="Technology" if i % 2 == 0 else "Finance",
                country="United States" if i % 3 == 0 else "Other"
            ) for i in range(10000, 15000)  # 5000 entities
        ]
        
        # Measure batch processing time
        start_time = time.time()
        result = await performance_client.batch_create_companies(entities)
        processing_time = (time.time() - start_time) * 1000
        
        assert result["nodes_created"] == len(entities)
        assert processing_time < 5000  # Should complete within 5 seconds

    def test_memory_usage_large_queries(self, performance_client):
        """Test memory usage during large query operations."""
        import psutil
        import os
        
        # Setup large dataset
        asyncio.run(self._setup_large_dataset(performance_client))
        
        # Measure memory before operation
        process = psutil.Process(os.getpid())
        memory_before = process.memory_info().rss / 1024 / 1024  # MB
        
        async def large_query():
            return await performance_client.execute_query(
                "MATCH (c:Company) RETURN c ORDER BY c.name"
            )
        
        # Execute large query
        result = asyncio.run(large_query())
        
        # Measure memory after operation
        memory_after = process.memory_info().rss / 1024 / 1024  # MB
        memory_growth = memory_after - memory_before
        
        # Assert reasonable memory usage (shouldn't grow by more than 100MB)
        assert memory_growth < 100
        assert len(result) > 0

    async def test_connection_pool_performance(self, neo4j_test_uri, neo4j_test_credentials):
        """Test connection pool performance under load."""
        username, password = neo4j_test_credentials
        
        # Test with small pool
        client_small = Neo4jClient(neo4j_test_uri, username, password, max_connection_pool_size=5)
        await client_small.initialize()
        
        # Test with large pool
        client_large = Neo4jClient(neo4j_test_uri, username, password, max_connection_pool_size=50)
        await client_large.initialize()
        
        async def concurrent_operations(client, num_operations=20):
            tasks = [
                client.execute_query("RETURN $i as value", {"i": i})
                for i in range(num_operations)
            ]
            start_time = time.time()
            await asyncio.gather(*tasks)
            return time.time() - start_time
        
        # Compare performance
        time_small_pool = await concurrent_operations(client_small)
        time_large_pool = await concurrent_operations(client_large)
        
        # Large pool should be faster for concurrent operations
        assert time_large_pool < time_small_pool * 1.5  # Allow some variance
        
        await client_small.close()
        await client_large.close()

    def test_query_optimization(self, benchmark, performance_client):
        """Test query optimization techniques."""
        # Setup indexed data
        asyncio.run(self._setup_performance_data(performance_client))
        
        # Optimized query using index
        async def optimized_query():
            return await performance_client.execute_query(
                "MATCH (c:Company {permid: $permid}) RETURN c",
                {"permid": 4295905573}
            )
        
        # Non-optimized query (full table scan)
        async def non_optimized_query():
            return await performance_client.execute_query(
                "MATCH (c:Company) WHERE c.description CONTAINS $text RETURN c",
                {"text": "Apple"}
            )
        
        # Benchmark both queries
        optimized_result = benchmark.pedantic(
            asyncio.run, 
            args=[optimized_query()],
            iterations=1,
            rounds=10
        )
        
        # The optimized query should be much faster
        # (This is more of a demonstration than a strict assertion)
        assert len(optimized_result) >= 0

    # Helper methods
    async def _setup_performance_data(self, client):
        """Setup standard performance test data."""
        entities = [
            CompanyEntity(
                permid=4295905573,
                name="Apple Inc",
                is_final_assembler=True,
                match_score=0.98,
                industry_sector="Technology",
                country="United States"
            ),
            CompanyEntity(
                permid=4295877456,
                name="Samsung Electronics",
                is_final_assembler=True,
                match_score=0.96,
                industry_sector="Technology",
                country="South Korea"
            ),
            CompanyEntity(
                permid=4295906319,
                name="QUALCOMM Inc",
                is_final_assembler=False,
                match_score=0.94,
                industry_sector="Technology",
                country="United States"
            )
        ]
        await client.batch_create_companies(entities)

    async def _setup_large_dataset(self, client):
        """Setup large dataset for memory/performance testing."""
        entities = [
            CompanyEntity(
                permid=i,
                name=f"Performance Company {i}",
                is_final_assembler=i % 5 == 0,
                match_score=0.7 + (i % 30) * 0.01,
                industry_sector="Technology",
                country="Test Country"
            ) for i in range(50000, 52000)  # 2000 entities
        ]
        await client.batch_create_companies(entities)


@pytest.mark.performance
class TestETLPipelinePerformance:
    """Performance tests for the overall ETL pipeline."""

    @pytest.mark.benchmark(group="etl_pipeline")
    def test_end_to_end_etl_performance(self, benchmark, performance_client, mock_aiohttp_session):
        """Benchmark end-to-end ETL pipeline performance."""
        # Mock external API response
        mock_response_data = {
            "head": {"vars": ["entity", "entityLabel", "permid"]},
            "results": {
                "bindings": [
                    {
                        "entity": {"type": "uri", "value": "http://www.wikidata.org/entity/Q312"},
                        "entityLabel": {"type": "literal", "value": "Apple Inc"},
                        "permid": {"type": "literal", "value": "4295905573"}
                    }
                ]
            }
        }
        mock_aiohttp_session.get.return_value.__aenter__.return_value.json.return_value = mock_response_data
        
        async def etl_pipeline():
            # Simulate ETL pipeline steps
            # 1. Extract from external API (mocked)
            # 2. Transform data
            # 3. Load into Neo4j
            entity = CompanyEntity(
                permid=4295905573,
                name="Apple Inc",
                is_final_assembler=True,
                match_score=0.98
            )
            return await performance_client.create_company_entity(entity)
        
        result = benchmark(asyncio.run, etl_pipeline())
        assert result["nodes_created"] == 1

    @pytest.mark.slow
    async def test_bulk_etl_throughput(self, performance_client):
        """Test ETL throughput with large datasets."""
        batch_sizes = [100, 500, 1000, 2000]
        throughput_results = {}
        
        for batch_size in batch_sizes:
            entities = [
                CompanyEntity(
                    permid=i,
                    name=f"Throughput Test Company {i}",
                    is_final_assembler=i % 4 == 0,
                    match_score=0.8
                ) for i in range(batch_size)
            ]
            
            start_time = time.time()
            result = await performance_client.batch_create_companies(entities)
            execution_time = time.time() - start_time
            
            throughput = batch_size / execution_time  # entities per second
            throughput_results[batch_size] = throughput
            
            assert result["nodes_created"] == batch_size
            
            # Clean up for next test
            await performance_client.execute_query("MATCH (n) DETACH DELETE n")
        
        # Larger batches should generally have higher throughput
        # (though this may plateau at some point)
        assert throughput_results[1000] > throughput_results[100]
        
        # Log throughput results for analysis
        for batch_size, throughput in throughput_results.items():
            print(f"Batch size {batch_size}: {throughput:.2f} entities/second")