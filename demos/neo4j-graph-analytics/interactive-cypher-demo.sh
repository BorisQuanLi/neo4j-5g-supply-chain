#!/bin/bash

# Interactive Cypher Demo for Jefferies Securities Interview
# Allows live querying of the 5G supply chain graph

set -e

echo "🎯 INTERACTIVE NEO4J GRAPH ANALYTICS DEMO"
echo "=========================================="
echo "Jefferies Securities - Graph Data Engineer Position"
echo ""

# Ensure Neo4j is running and data is loaded
if ! curl -s http://localhost:7474 > /dev/null; then
    echo "⚠️  Neo4j not running. Starting services..."
    docker-compose up -d neo4j
    sleep 10
fi

# Load demo data
echo "📊 Loading 5G supply chain data..."
docker exec supply-chain-neo4j cypher-shell -u neo4j -p password -f /dev/stdin < demos/neo4j-graph-analytics/load-demo-data.cypher > /dev/null
echo "✅ Data loaded: 6 companies, 8 relationships"
echo ""

# Interactive menu
while true; do
    echo "🔍 SELECT A GRAPH ANALYTICS DEMO:"
    echo "================================="
    echo "1. Complete Supply Chain Network Visualization"
    echo "2. Frenemy Analysis (Apple ↔ Samsung Competition + Partnership)"
    echo "3. Supply Chain Pathfinding (Apple → ARM Holdings)"
    echo "4. Critical Suppliers (High-Value Contracts > $5B)"
    echo "5. Market Concentration Risk Analysis"
    echo "6. Geographic Distribution Analysis"
    echo "7. Custom Cypher Query (Advanced)"
    echo "8. Open Neo4j Browser"
    echo "9. Exit Demo"
    echo ""
    read -p "Enter your choice (1-9): " choice

    case $choice in
        1)
            echo ""
            echo "🌐 COMPLETE SUPPLY CHAIN NETWORK"
            echo "Query: MATCH (n:Company) OPTIONAL MATCH (n)-[r]-(m:Company) RETURN n, r, m;"
            echo ""
            docker exec supply-chain-neo4j cypher-shell -u neo4j -p password \
                "MATCH (n:Company) OPTIONAL MATCH (n)-[r]-(m:Company) RETURN n.name, type(r), m.name ORDER BY n.name"
            ;;
        2)
            echo ""
            echo "⚔️  FRENEMY ANALYSIS: Apple ↔ Samsung"
            echo "Query: Complex business relationships (competition + partnership)"
            echo ""
            docker exec supply-chain-neo4j cypher-shell -u neo4j -p password \
                "MATCH (samsung:Company {name: 'Samsung Electronics Co Ltd'})-[r]-(apple:Company {name: 'Apple Inc'}) 
                 RETURN samsung.name AS Company1, type(r) AS Relationship, apple.name AS Company2, 
                        r.component_type AS Component, r.market_segment AS Market"
            ;;
        3)
            echo ""
            echo "🛤️  SUPPLY CHAIN PATHFINDING"
            echo "Query: Shortest path from Apple to ARM Holdings"
            echo ""
            docker exec supply-chain-neo4j cypher-shell -u neo4j -p password \
                "MATCH path = shortestPath((apple:Company {name: 'Apple Inc'})-[*]-(arm:Company {name: 'ARM Holdings plc'}))
                 UNWIND nodes(path) AS node
                 RETURN node.name AS Company, node.country AS Country"
            ;;
        4)
            echo ""
            echo "💰 CRITICAL SUPPLIERS (High-Value Contracts)"
            echo "Query: Contracts > $5 billion (systemic risk analysis)"
            echo ""
            docker exec supply-chain-neo4j cypher-shell -u neo4j -p password \
                "MATCH (supplier:Company)-[r:SUPPLY_COMPONENTS|MANUFACTURING_PARTNER]-(customer:Company)
                 WHERE r.contract_value > 5000000000
                 RETURN supplier.name AS Supplier, customer.name AS Customer, 
                        r.contract_value AS ContractValue, r.component_type AS Component
                 ORDER BY r.contract_value DESC"
            ;;
        5)
            echo ""
            echo "📊 MARKET CONCENTRATION RISK"
            echo "Query: Final assembler market share analysis"
            echo ""
            docker exec supply-chain-neo4j cypher-shell -u neo4j -p password \
                "MATCH (c:Company) WHERE c.is_final_assembler = true
                 WITH sum(c.market_cap) AS total_market_cap
                 MATCH (c:Company) WHERE c.is_final_assembler = true
                 RETURN c.name AS Company, c.market_cap AS MarketCap,
                        round(100.0 * c.market_cap / total_market_cap, 2) AS MarketShare
                 ORDER BY MarketShare DESC"
            ;;
        6)
            echo ""
            echo "🌍 GEOGRAPHIC DISTRIBUTION"
            echo "Query: Supply chain geographic risk analysis"
            echo ""
            docker exec supply-chain-neo4j cypher-shell -u neo4j -p password \
                "MATCH (c:Company)
                 RETURN c.country AS Country, collect(c.name) AS Companies, count(c) AS CompanyCount
                 ORDER BY CompanyCount DESC"
            ;;
        7)
            echo ""
            echo "⚡ CUSTOM CYPHER QUERY"
            echo "Enter your own Cypher query (or 'back' to return):"
            read -p "cypher> " custom_query
            if [ "$custom_query" != "back" ]; then
                docker exec supply-chain-neo4j cypher-shell -u neo4j -p password "$custom_query"
            fi
            ;;
        8)
            echo ""
            echo "🌐 Opening Neo4j Browser..."
            echo "URL: http://localhost:7474"
            echo "Login: neo4j / password"
            if command -v xdg-open > /dev/null; then
                xdg-open http://localhost:7474
            elif command -v open > /dev/null; then
                open http://localhost:7474
            else
                echo "Please open http://localhost:7474 in your browser"
            fi
            ;;
        9)
            echo ""
            echo "🎪 Demo completed! Thank you for exploring our graph analytics platform."
            echo "💼 This demonstrates the graph database expertise required for"
            echo "   the Jefferies Securities Graph Data Engineer position."
            exit 0
            ;;
        *)
            echo "❌ Invalid choice. Please select 1-9."
            ;;
    esac
    
    echo ""
    read -p "Press Enter to continue..."
    echo ""
done