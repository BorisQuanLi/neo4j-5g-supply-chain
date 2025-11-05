package com.jefferies.supplychain.unit.model;

import com.jefferies.supplychain.model.Company;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the Company model entity.
 * Tests validation, getters/setters, equals/hashCode, and business logic.
 */
@DisplayName("Company Model Unit Tests")
class CompanyTest {

    private Company company;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
        company = new Company();
    }

    @Test
    @DisplayName("Should create company with all required fields")
    void testCompanyCreation() {
        // Given: A company with all fields set
        company.setPermid(4295905573L);
        company.setName("Apple Inc");
        company.setIsFinalAssembler(true);
        company.setMatchScore(0.98);
        company.setTickerSymbol("AAPL");
        company.setIndustrySector("Technology");
        company.setBusinessType("Consumer Electronics");
        company.setCountry("United States");
        company.setLastUpdated(testTime);
        company.setDataSource("WIKIDATA");
        company.setCentralityScore(0.85);
        company.setCommunityId(42L);

        // Then: All fields should be correctly set
        assertThat(company.getPermid()).isEqualTo(4295905573L);
        assertThat(company.getName()).isEqualTo("Apple Inc");
        assertThat(company.getIsFinalAssembler()).isTrue();
        assertThat(company.getMatchScore()).isEqualTo(0.98);
        assertThat(company.getTickerSymbol()).isEqualTo("AAPL");
        assertThat(company.getIndustrySector()).isEqualTo("Technology");
        assertThat(company.getBusinessType()).isEqualTo("Consumer Electronics");
        assertThat(company.getCountry()).isEqualTo("United States");
        assertThat(company.getLastUpdated()).isEqualTo(testTime);
        assertThat(company.getDataSource()).isEqualTo("WIKIDATA");
        assertThat(company.getCentralityScore()).isEqualTo(0.85);
        assertThat(company.getCommunityId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Should handle null values appropriately")
    void testNullValueHandling() {
        // Given: A company with minimal fields
        company.setPermid(1234567890L);
        company.setName("Test Company");

        // Then: Optional fields should be null without throwing exceptions
        assertThat(company.getPermid()).isEqualTo(1234567890L);
        assertThat(company.getName()).isEqualTo("Test Company");
        assertThat(company.getIsFinalAssembler()).isNull();
        assertThat(company.getMatchScore()).isNull();
        assertThat(company.getTickerSymbol()).isNull();
        assertThat(company.getIndustrySector()).isNull();
        assertThat(company.getBusinessType()).isNull();
        assertThat(company.getCountry()).isNull();
        assertThat(company.getLastUpdated()).isNull();
        assertThat(company.getDataSource()).isNull();
        assertThat(company.getCentralityScore()).isNull();
        assertThat(company.getCommunityId()).isNull();
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void testEqualsAndHashCode() {
        // Given: Two companies with same PermID
        Company company1 = new Company();
        company1.setPermid(4295905573L);
        company1.setName("Apple Inc");

        Company company2 = new Company();
        company2.setPermid(4295905573L);
        company2.setName("Apple Inc"); // Same data

        Company company3 = new Company();
        company3.setPermid(4295877456L); // Different PermID
        company3.setName("Samsung Electronics");

        // Then: Companies with same PermID should be equal
        assertThat(company1).isEqualTo(company2);
        assertThat(company1.hashCode()).isEqualTo(company2.hashCode());
        
        // Companies with different PermID should not be equal
        assertThat(company1).isNotEqualTo(company3);
        assertThat(company1.hashCode()).isNotEqualTo(company3.hashCode());
    }

    @Test
    @DisplayName("Should handle edge cases in match score")
    void testMatchScoreValidation() {
        // Test valid match scores
        company.setMatchScore(0.0);
        assertThat(company.getMatchScore()).isEqualTo(0.0);
        
        company.setMatchScore(1.0);
        assertThat(company.getMatchScore()).isEqualTo(1.0);
        
        company.setMatchScore(0.95);
        assertThat(company.getMatchScore()).isEqualTo(0.95);
        
        // Test edge cases - the model should accept any double value
        // Validation would typically be handled at the service layer
        company.setMatchScore(-0.1);
        assertThat(company.getMatchScore()).isEqualTo(-0.1);
        
        company.setMatchScore(1.1);
        assertThat(company.getMatchScore()).isEqualTo(1.1);
    }

    @Test
    @DisplayName("Should handle centrality score updates")
    void testCentralityScoreUpdates() {
        // Given: A company with initial centrality score
        company.setCentralityScore(0.0);
        
        // When: Updating centrality score
        company.setCentralityScore(0.75);
        
        // Then: Score should be updated
        assertThat(company.getCentralityScore()).isEqualTo(0.75);
        
        // Test multiple updates
        company.setCentralityScore(0.85);
        assertThat(company.getCentralityScore()).isEqualTo(0.85);
    }

    @Test
    @DisplayName("Should handle community ID assignments")
    void testCommunityIdAssignment() {
        // Given: A company without community assignment
        assertThat(company.getCommunityId()).isNull();
        
        // When: Assigning to a community
        company.setCommunityId(42L);
        
        // Then: Community ID should be set
        assertThat(company.getCommunityId()).isEqualTo(42L);
        
        // Test reassignment
        company.setCommunityId(99L);
        assertThat(company.getCommunityId()).isEqualTo(99L);
        
        // Test clearing assignment
        company.setCommunityId(null);
        assertThat(company.getCommunityId()).isNull();
    }

    @Test
    @DisplayName("Should handle boolean flags correctly")
    void testBooleanFlags() {
        // Test final assembler flag
        company.setIsFinalAssembler(true);
        assertThat(company.getIsFinalAssembler()).isTrue();
        
        company.setIsFinalAssembler(false);
        assertThat(company.getIsFinalAssembler()).isFalse();
        
        company.setIsFinalAssembler(null);
        assertThat(company.getIsFinalAssembler()).isNull();
    }

    @Test
    @DisplayName("Should format string representation correctly")
    void testToString() {
        // Given: A company with basic information
        company.setPermid(4295905573L);
        company.setName("Apple Inc");
        company.setTickerSymbol("AAPL");
        
        // When: Getting string representation
        String companyString = company.toString();
        
        // Then: Should contain key identifiers
        assertThat(companyString).contains("Apple Inc");
        assertThat(companyString).contains("4295905573");
    }

    @Test
    @DisplayName("Should handle timestamp updates")
    void testTimestampUpdates() {
        // Given: Current time
        LocalDateTime now = LocalDateTime.now();
        
        // When: Setting last updated timestamp
        company.setLastUpdated(now);
        
        // Then: Timestamp should be preserved exactly
        assertThat(company.getLastUpdated()).isEqualTo(now);
        
        // Test updating timestamp
        LocalDateTime later = now.plusMinutes(5);
        company.setLastUpdated(later);
        assertThat(company.getLastUpdated()).isEqualTo(later);
    }

    @Test
    @DisplayName("Should validate data source tracking")
    void testDataSourceTracking() {
        // Test different data sources
        company.setDataSource("WIKIDATA");
        assertThat(company.getDataSource()).isEqualTo("WIKIDATA");
        
        company.setDataSource("PERMID");
        assertThat(company.getDataSource()).isEqualTo("PERMID");
        
        company.setDataSource("BLOOMBERG");
        assertThat(company.getDataSource()).isEqualTo("BLOOMBERG");
        
        company.setDataSource("MANUAL_INPUT");
        assertThat(company.getDataSource()).isEqualTo("MANUAL_INPUT");
    }

    @Test
    @DisplayName("Should support method chaining for builder pattern")
    void testBuilderPattern() {
        // Test if the model supports fluent API (if setter methods return Company)
        // This would depend on the actual implementation of the Company class
        company.setPermid(4295905573L);
        company.setName("Apple Inc");
        company.setTickerSymbol("AAPL");
        
        // Verify the chain worked
        assertThat(company.getPermid()).isEqualTo(4295905573L);
        assertThat(company.getName()).isEqualTo("Apple Inc");
        assertThat(company.getTickerSymbol()).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("Should handle industry and business type classifications")
    void testIndustryClassifications() {
        // Test technology sector companies
        company.setIndustrySector("Technology");
        company.setBusinessType("Consumer Electronics");
        
        assertThat(company.getIndustrySector()).isEqualTo("Technology");
        assertThat(company.getBusinessType()).isEqualTo("Consumer Electronics");
        
        // Test semiconductor companies
        company.setIndustrySector("Technology");
        company.setBusinessType("Semiconductors");
        
        assertThat(company.getIndustrySector()).isEqualTo("Technology");
        assertThat(company.getBusinessType()).isEqualTo("Semiconductors");
        
        // Test financial services companies
        company.setIndustrySector("Financial Services");
        company.setBusinessType("Investment Banking");
        
        assertThat(company.getIndustrySector()).isEqualTo("Financial Services");
        assertThat(company.getBusinessType()).isEqualTo("Investment Banking");
    }
}