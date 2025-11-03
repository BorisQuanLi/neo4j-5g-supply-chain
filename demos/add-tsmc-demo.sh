#!/bin/bash

# Add TSMC to the 5G Supply Chain Graph Demo
# This script demonstrates adding a critical semiconductor company to the graph

echo "🏭 Adding TSMC (Taiwan Semiconductor Manufacturing Company) to Supply Chain Graph..."

# Run the Python ETL script to add TSMC
cd ../python-etl/src
python add_tsmc_company.py

echo "✅ TSMC added to graph database"

# Query TSMC-ARM relationship via Neo4j Browser (Cypher)
echo "📊 Querying TSMC-ARM relationship via Cypher..."
echo "
// Find TSMC-ARM relationship with properties
MATCH (tsmc:Company {stock_symbol: 'TSM'})-[r:MANUFACTURES_DESIGNS_FOR]->(arm:Company {stock_symbol: 'ARM'})
RETURN tsmc.name, tsmc.founded_year, tsmc.employees,
       r.process_nodes, r.annual_volume_millions, r.strategic_importance,
       arm.name, arm.business_model, arm.founded_year
" > /tmp/tsmc_arm_query.cypher

echo "🔍 TSMC-ARM relationship query saved to /tmp/tsmc_arm_query.cypher"
echo "   Run this in Neo4j Browser at http://localhost:7474"

# Test Spring Boot API access
echo "🌐 Testing TSMC access via Spring Boot API..."
curl -s "http://localhost:8080/api/v1/graph-analytics/companies/Taiwan%20Semiconductor%20Manufacturing%20Company" | jq '.'

echo "🎯 Demo complete! TSMC-ARM relationship created with:"
echo "   • Node Properties: stock_symbol, founded_year, employees, business_model"
echo "   • Relationship Properties: process_nodes, annual_volume, strategic_importance"
echo "   • Access via: Neo4j Browser UI + Spring Boot REST API"