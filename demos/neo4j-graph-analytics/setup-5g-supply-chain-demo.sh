#!/bin/bash

# Quick Demo Setup Script for Jefferies Securities Interview
# Loads sample data and provides demo URLs

set -e
set -o pipefail

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
output=$(cat demos/neo4j-graph-analytics/load-demo-data.cypher | docker exec -i supply-chain-neo4j cypher-shell -u neo4j -p password --database neo4j --format plain)

# Parse the output to extract counts
company_count=$(echo "$output" | tail -1 | cut -d',' -f2 | tr -d ' ')
rel_count_raw=$(echo "$output" | tail -1 | cut -d',' -f3 | tr -d ' ')
rel_count=$((rel_count_raw / 2))

echo "✅ Demo data loaded: $company_count companies, $rel_count relationships"


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
echo "📋 Full demo script: demos/neo4j-graph-analytics/5g-supply-chain-knowledge-graph.cypher"
echo "📖 Demo guide: demos/README.md"
echo ""
echo "🎪 Ready to explore graph database analytics!"