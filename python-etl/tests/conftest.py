"""
Pytest configuration and shared fixtures for the Python ETL testing suite.
"""
import asyncio
import os
import pytest
import pytest_asyncio
from testcontainers.neo4j import Neo4jContainer
from unittest.mock import AsyncMock, Mock
import aiohttp

from src.neo4j_client import Neo4jClient


@pytest.fixture(scope="session")
def event_loop():
    """Create an instance of the default event loop for the test session."""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest.fixture(scope="session")
def neo4j_container():
    """
    Shared Neo4j container for integration tests.
    Reuses the same container across all tests for performance.
    """
    container = Neo4jContainer("neo4j:5.15.0-enterprise") \
        .with_env("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes") \
        .with_env("NEO4J_AUTH", "neo4j/testpassword") \
        .with_env("NEO4J_PLUGINS", '["graph-data-science"]') \
        .with_env("NEO4J_dbms_security_procedures_unrestricted", "gds.*") \
        .with_env("NEO4J_dbms_security_procedures_allowlist", "gds.*")
    
    container.start()
    yield container
    container.stop()


@pytest.fixture(scope="session")
def neo4j_test_uri(neo4j_container):
    """Get the Neo4j connection URI for the test container."""
    return neo4j_container.get_connection_url()


@pytest.fixture(scope="session")
def neo4j_test_credentials():
    """Get the Neo4j test credentials."""
    return ("neo4j", "testpassword")


@pytest_asyncio.fixture
async def neo4j_client(neo4j_test_uri, neo4j_test_credentials):
    """
    Create a Neo4j client connected to the test container.
    """
    username, password = neo4j_test_credentials
    client = Neo4jClient(neo4j_test_uri, username, password)
    await client.initialize()
    
    # Clean the database before each test
    await client.execute_query("MATCH (n) DETACH DELETE n")
    
    yield client
    
    # Clean up after the test
    await client.execute_query("MATCH (n) DETACH DELETE n")
    await client.close()


@pytest_asyncio.fixture
async def mock_aiohttp_session():
    """Mock aiohttp session for external API testing."""
    mock_session = AsyncMock(spec=aiohttp.ClientSession)
    mock_response = AsyncMock()
    mock_response.json = AsyncMock()
    mock_response.text = AsyncMock()
    mock_response.status = 200
    mock_session.get.return_value.__aenter__.return_value = mock_response
    mock_session.post.return_value.__aenter__.return_value = mock_response
    return mock_session


@pytest.fixture
def sample_company_data():
    """Sample company data for testing."""
    return {
        "permid": 4295905573,
        "name": "Apple Inc",
        "ticker": "AAPL",
        "country": "United States",
        "sector": "Technology",
        "business_type": "Consumer Electronics",
        "is_final_assembler": True,
        "match_score": 0.98
    }


@pytest.fixture
def sample_companies_batch():
    """Sample batch of companies for testing bulk operations."""
    return [
        {
            "permid": 4295905573,
            "name": "Apple Inc",
            "ticker": "AAPL",
            "country": "United States",
            "sector": "Technology",
            "business_type": "Consumer Electronics",
            "is_final_assembler": True,
            "match_score": 0.98
        },
        {
            "permid": 4295877456,
            "name": "Samsung Electronics Co Ltd",
            "ticker": "005930.KS",
            "country": "South Korea",
            "sector": "Technology",
            "business_type": "Semiconductors",
            "is_final_assembler": True,
            "match_score": 0.96
        },
        {
            "permid": 4295906319,
            "name": "QUALCOMM Inc",
            "ticker": "QCOM",
            "country": "United States",
            "sector": "Technology",
            "business_type": "Semiconductors",
            "is_final_assembler": False,
            "match_score": 0.94
        }
    ]


@pytest.fixture
def mock_wikidata_response():
    """Mock WIKIDATA SPARQL response."""
    return {
        "head": {
            "vars": ["entity", "entityLabel", "permid", "ticker", "country", "countryLabel"]
        },
        "results": {
            "bindings": [
                {
                    "entity": {"type": "uri", "value": "http://www.wikidata.org/entity/Q312"},
                    "entityLabel": {"type": "literal", "value": "Apple Inc"},
                    "permid": {"type": "literal", "value": "4295905573"},
                    "ticker": {"type": "literal", "value": "AAPL"},
                    "country": {"type": "uri", "value": "http://www.wikidata.org/entity/Q30"},
                    "countryLabel": {"type": "literal", "value": "United States of America"}
                },
                {
                    "entity": {"type": "uri", "value": "http://www.wikidata.org/entity/Q20718"},
                    "entityLabel": {"type": "literal", "value": "Samsung Electronics"},
                    "permid": {"type": "literal", "value": "4295877456"},
                    "ticker": {"type": "literal", "value": "005930.KS"},
                    "country": {"type": "uri", "value": "http://www.wikidata.org/entity/Q884"},
                    "countryLabel": {"type": "literal", "value": "South Korea"}
                }
            ]
        }
    }


@pytest.fixture
def environment_variables():
    """Set up test environment variables."""
    original_env = os.environ.copy()
    
    # Set test environment variables
    test_env = {
        "NEO4J_URI": "bolt://localhost:7687",
        "NEO4J_USERNAME": "neo4j",
        "NEO4J_PASSWORD": "testpassword",
        "WIKIDATA_ENDPOINT": "https://query.wikidata.org/sparql",
        "LOG_LEVEL": "DEBUG",
        "BATCH_SIZE": "100",
        "MAX_RETRIES": "3",
        "RATE_LIMIT_REQUESTS": "10",
        "RATE_LIMIT_PERIOD": "1"
    }
    
    for key, value in test_env.items():
        os.environ[key] = value
    
    yield test_env
    
    # Restore original environment
    os.environ.clear()
    os.environ.update(original_env)


@pytest.fixture
def mock_logger():
    """Mock logger for testing logging behavior."""
    return Mock()


# Utility functions for tests
def assert_company_fields(company_dict, expected_fields=None):
    """Assert that a company dictionary has the expected fields."""
    if expected_fields is None:
        expected_fields = ["permid", "name", "country", "sector"]
    
    for field in expected_fields:
        assert field in company_dict, f"Missing field: {field}"
        assert company_dict[field] is not None, f"Field {field} is None"


def create_test_relationship_data():
    """Create test relationship data for supply chain tests."""
    return [
        {
            "source_permid": 4295905573,  # Apple
            "target_permid": 4295906319,  # Qualcomm
            "relationship_type": "SUPPLY_COMPONENTS",
            "strength": 0.8,
            "last_updated": "2024-01-01T00:00:00Z"
        },
        {
            "source_permid": 4295905573,  # Apple
            "target_permid": 4295871234,  # TSMC
            "relationship_type": "MANUFACTURING_PARTNER",
            "strength": 0.9,
            "last_updated": "2024-01-01T00:00:00Z"
        }
    ]


# Async test utilities
async def wait_for_condition(condition_func, timeout=10, interval=0.1):
    """
    Wait for a condition to become true within a timeout period.
    Useful for testing async operations.
    """
    import time
    start_time = time.time()
    
    while time.time() - start_time < timeout:
        if await condition_func():
            return True
        await asyncio.sleep(interval)
    
    return False


# Performance test utilities
def performance_threshold(max_execution_time_ms=1000):
    """
    Decorator to assert that a test completes within a time threshold.
    """
    def decorator(func):
        import time
        import functools
        
        @functools.wraps(func)
        async def wrapper(*args, **kwargs):
            start_time = time.time()
            result = await func(*args, **kwargs)
            execution_time = (time.time() - start_time) * 1000
            
            assert execution_time < max_execution_time_ms, \
                f"Test exceeded performance threshold: {execution_time:.2f}ms > {max_execution_time_ms}ms"
            
            return result
        
        return wrapper
    return decorator