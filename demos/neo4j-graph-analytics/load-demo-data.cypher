// Clear existing data
MATCH (n) DETACH DELETE n;

// Create Current Smartphone Market Leaders Demo Data (Q4 2023-Q2 2025)
// Top market share: Apple (~20%), Samsung (~21%), Xiaomi (~14%)
CREATE 
(apple:Company {
  permid: 4295905573,
  name: "Apple Inc",
  ticker: "AAPL",
  country: "United States",
  industry: "Technology",
  business_type: "Consumer Electronics",
  is_final_assembler: true,
  market_cap: 3000000000000,
  revenue: 394328000000,
  match_score: 0.98
}),

(samsung:Company {
  permid: 4295877456,
  name: "Samsung Electronics Co Ltd",
  ticker: "005930.KS",
  country: "South Korea", 
  industry: "Technology",
  business_type: "Semiconductors",
  is_final_assembler: true,
  market_cap: 350000000000,
  revenue: 244187000000,
  match_score: 0.96
}),

(qualcomm:Company {
  permid: 4295906319,
  name: "QCOM (QUALCOMM Inc)",
  ticker: "QCOM",
  country: "United States",
  industry: "Technology", 
  business_type: "Semiconductors",
  is_final_assembler: false,
  market_cap: 140000000000,
  revenue: 35820000000,
  match_score: 0.94
}),

(arm:Company {
  permid: 4295903847,
  name: "ARM Holdings plc",
  ticker: "ARM",
  country: "United Kingdom",
  industry: "Technology",
  business_type: "Semiconductors", 
  is_final_assembler: false,
  market_cap: 120000000000,
  revenue: 2700000000,
  match_score: 0.92
}),

(tsmc:Company {
  permid: 4295871234,
  name: "TSM (Taiwan Semiconductor Manufacturing Co Ltd)",
  ticker: "TSM",
  country: "Taiwan",
  industry: "Technology",
  business_type: "Semiconductors",
  is_final_assembler: false,
  market_cap: 500000000000,
  revenue: 70850000000,
  match_score: 0.95
}),

(mediatek:Company {
  permid: 4295823456,
  name: "MediaTek Inc", 
  ticker: "2454.TW",
  country: "Taiwan",
  industry: "Technology",
  business_type: "Semiconductors",
  is_final_assembler: false,
  market_cap: 60000000000,
  revenue: 18930000000,
  match_score: 0.89
}),

(xiaomi:Company {
  permid: 4295908005,
  name: "Xiaomi Corporation",
  ticker: "1810.HK",
  country: "China",
  industry: "Technology",
  business_type: "Consumer Electronics",
  is_final_assembler: true,
  market_cap: 45000000000,
  revenue: 42000000000,
  match_score: 0.88,
  market_share_2024: 0.14,
  global_rank: 3
}),

// Supply Chain Relationships
(qualcomm)-[:SUPPLY_COMPONENTS {
  component_type: "5G Modem Chips",
  strength: 0.85,
  contract_value: 3000000000,
  relationship_type: "supplier"
}]->(apple),

(tsmc)-[:MANUFACTURING_PARTNER {
  component_type: "A-Series Processors", 
  strength: 0.95,
  contract_value: 15000000000,
  process_node: "3nm",
  relationship_type: "manufacturer"
}]->(apple),

(arm)-[:DESIGN_CHIPS_FOR {
  component_type: "Snapdragon Processors",
  strength: 0.92, 
  license_value: 1200000000,
  relationship_type: "ip_licensor"
}]->(qualcomm),

(tsmc)-[:MANUFACTURING_PARTNER {
  component_type: "ARM-based Chips",
  strength: 0.90,
  manufacturing_volume: 500000000,
  relationship_type: "manufacturer"
}]->(arm),

(mediatek)-[:COMPETES_WITH {
  market_segment: "Mobile Chipsets",
  strength: 0.78,
  competition_intensity: "Medium",
  relationship_type: "competitor"
}]->(qualcomm),

// Frenemy Relationships (Competition + Partnership)
(samsung)-[:COMPETES_WITH {
  market_segment: "Smartphones",
  strength: 0.95,
  competition_intensity: "High",
  market_overlap: 0.85,
  relationship_type: "competitor"
}]->(apple),

(samsung)-[:SUPPLY_COMPONENTS {
  component_type: "OLED Displays",
  strength: 0.82,
  contract_value: 8000000000,
  relationship_type: "supplier"
}]->(apple),

// Xiaomi relationships
(qualcomm)-[:SUPPLY_COMPONENTS {
  component_type: "Snapdragon Processors",
  strength: 0.80,
  contract_value: 2000000000,
  relationship_type: "supplier"
}]->(xiaomi),

(mediatek)-[:SUPPLY_COMPONENTS {
  component_type: "Dimensity Processors",
  strength: 0.75,
  contract_value: 1500000000,
  relationship_type: "supplier"
}]->(xiaomi),

(samsung)-[:COMPETES_WITH {
  market_segment: "Smartphones",
  strength: 0.80,
  competition_intensity: "High",
  geographic_focus: "Global",
  relationship_type: "competitor"
}]->(xiaomi);

// Create indexes for performance
CREATE INDEX company_permid_index IF NOT EXISTS FOR (c:Company) ON (c.permid);
CREATE INDEX company_name_index IF NOT EXISTS FOR (c:Company) ON (c.name);

// Return summary with market context
MATCH (c:Company) 
OPTIONAL MATCH (c)-[r]-()
RETURN "Current market leaders data loaded (Q4 2023-Q2 2025)" AS status,
       count(DISTINCT c) AS companies_created,
       count(r) AS relationships_created;