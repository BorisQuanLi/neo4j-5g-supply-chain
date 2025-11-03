#!/bin/bash

# Quick Demo Setup Script for Jefferies Securities Interview
# Loads sample data and provides demo URLs

set -e

echo "🚀 GRAPH ANALYTICS PLATFORM DEMO"
echo "================================="

# Check if Neo4j is running
if ! curl -s http://localhost:7474 > /dev/null; then
    echo "Starting Neo4j services..."
    docker-compose up -d neo4j
    
    echo "Waiting for Neo4j to be ready..."
    sleep 10
    
    # Wait for health check
    while ! curl -s http://localhost:7474 > /dev/null; do
        echo "Waiting for Neo4j..."
        sleep 2
    done
fi

echo "✅ Neo4j is running"

# Load demo data using cypher-shell
echo "📊 Loading demo data..."
docker exec supply-chain-neo4j cypher-shell -u neo4j -p password -f /dev/stdin < demos/neo4j-graph-analytics/load-demo-data.cypher
echo "✅ Demo data loaded: 6 companies, 8 relationships"

echo ""
echo "🎯 DEMO READY!"
echo "=============="
echo ""
echo "Neo4j Browser: http://localhost:7474"
echo "Login: neo4j / password"
echo ""
echo "📝 Try these queries, or any inspired by your curiosity:"
echo ""
echo "1. Complete Network:"
echo "   MATCH (n:Company) OPTIONAL MATCH (n)-[r]-(m:Company) RETURN n, r, m;"
echo ""
echo "2. Top 2 Market Leaders:"
echo "   MATCH (apple:Company {name: 'Apple Inc'}), (samsung:Company {name: 'Samsung Electronics Co Ltd'}) RETURN apple, samsung;"
echo ""
echo "3. Samsung-Apple Frenemy Dynamics:"
echo "   MATCH (samsung:Company {name: 'Samsung Electronics Co Ltd'})-[r]-(apple:Company {name: 'Apple Inc'}) RETURN samsung, r, apple;"
echo ""
echo "4. Market Share Analysis:"
echo "   MATCH (c:Company) WHERE c.is_final_assembler = true RETURN c.name, c.market_cap, c.country ORDER BY c.market_cap DESC;"
echo ""
echo "5. Supply Chain Dependencies:"
echo "   MATCH path = shortestPath((apple:Company {name: 'Apple Inc'})-[*]-(tsmc:Company {name: 'TSM (Taiwan Semiconductor Manufacturing Co Ltd)'})) RETURN path;"
echo ""
echo "📋 Full demo script: demos/neo4j-graph-analytics/neo4j-browser-demo.cypher"
echo "📖 Demo guide: demos/README.md"
echo ""
echo "🎪 Ready to explore graph database analytics!"