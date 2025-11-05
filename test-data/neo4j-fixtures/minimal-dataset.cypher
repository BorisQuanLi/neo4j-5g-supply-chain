// Minimal dataset for basic testing - Core 5G supply chain entities
// Creates a simple graph with 3 companies and 2 relationships

// Clear existing data
MATCH (n) DETACH DELETE n;

// Create core companies
CREATE (apple:Company {
  permid: 4295905573,
  name: "Apple Inc",
  ticker_symbol: "AAPL",
  industry_sector: "Technology",
  business_type: "Consumer Electronics",
  country: "United States",
  is_final_assembler: true,
  match_score: 0.98,
  market_cap: 3000000000000,
  revenue: 394328000000,
  employees: 164000,
  founded_year: 1976,
  headquarters: "Cupertino, California",
  website: "https://www.apple.com",
  description: "Apple Inc. designs, manufactures, and markets smartphones, personal computers, tablets, wearables, and accessories worldwide.",
  data_source: "TEST_DATA",
  last_updated: datetime("2024-01-01T00:00:00Z"),
  centrality_score: 0.0,
  community_id: null
});

CREATE (qualcomm:Company {
  permid: 4295906319,
  name: "QUALCOMM Inc",
  ticker_symbol: "QCOM",
  industry_sector: "Technology",
  business_type: "Semiconductors",
  country: "United States",
  is_final_assembler: false,
  match_score: 0.94,
  market_cap: 140000000000,
  revenue: 35820000000,
  employees: 51000,
  founded_year: 1985,
  headquarters: "San Diego, California",
  website: "https://www.qualcomm.com",
  description: "QUALCOMM Incorporated develops and commercializes foundational technologies for the wireless industry.",
  data_source: "TEST_DATA",
  last_updated: datetime("2024-01-01T00:00:00Z"),
  centrality_score: 0.0,
  community_id: null
});

CREATE (tsmc:Company {
  permid: 4295871234,
  name: "Taiwan Semiconductor Manufacturing Co Ltd",
  ticker_symbol: "TSM",
  industry_sector: "Technology",
  business_type: "Semiconductors",
  country: "Taiwan",
  is_final_assembler: false,
  match_score: 0.95,
  market_cap: 500000000000,
  revenue: 70850000000,
  employees: 73090,
  founded_year: 1987,
  headquarters: "Hsinchu, Taiwan",
  website: "https://www.tsmc.com",
  description: "Taiwan Semiconductor Manufacturing Company Limited manufactures and sells integrated circuits and semiconductors.",
  data_source: "TEST_DATA",
  last_updated: datetime("2024-01-01T00:00:00Z"),
  centrality_score: 0.0,
  community_id: null
});

// Create relationships
MATCH (apple:Company {permid: 4295905573}), (qualcomm:Company {permid: 4295906319})
CREATE (apple)-[:SUPPLY_COMPONENTS {
  component_type: "5G Modem Chips",
  strength: 0.85,
  contract_value: 3000000000,
  start_date: date("2019-04-16"),
  end_date: date("2026-12-31"),
  exclusivity: false,
  geographic_scope: "Global",
  data_source: "TEST_DATA",
  last_updated: datetime("2024-01-01T00:00:00Z")
}]->(qualcomm);

MATCH (apple:Company {permid: 4295905573}), (tsmc:Company {permid: 4295871234})
CREATE (apple)-[:MANUFACTURING_PARTNER {
  component_type: "A-Series Processors",
  strength: 0.95,
  contract_value: 15000000000,
  start_date: date("2014-01-01"),
  end_date: date("2027-12-31"),
  exclusivity: true,
  process_node: "3nm",
  data_source: "TEST_DATA",
  last_updated: datetime("2024-01-01T00:00:00Z")
}]->(tsmc);

// Create indexes for performance
CREATE INDEX company_permid_index IF NOT EXISTS FOR (c:Company) ON (c.permid);
CREATE INDEX company_name_index IF NOT EXISTS FOR (c:Company) ON (c.name);
CREATE INDEX company_country_index IF NOT EXISTS FOR (c:Company) ON (c.country);

// Verify the data
MATCH (n:Company) RETURN n.name, n.permid, n.country ORDER BY n.name;