package com.jefferies.supplychain.integration;

import com.jefferies.supplychain.model.Company;
import com.jefferies.supplychain.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for CompanyRepository using TestContainers.
 * Tests the repository layer against a real Neo4j database instance.
 */
@DataNeo4jTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Company Repository Integration Tests")
class CompanyRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private CompanyRepository companyRepository;

    private Company appleTestData;
    private Company samsungTestData;
    private Company qualcommTestData;

    @BeforeEach
    void setUp() {
        // Clean the database before each test
        companyRepository.deleteAll();
        
        // Create test data based on real 5G supply chain entities
        appleTestData = createTestCompany(
            4295905573L, // Apple's real PermID
            "Apple Inc",
            true, // is final assembler
            0.98, // high match score
            "AAPL",
            "Technology",
            "Consumer Electronics",
            "United States"
        );
        
        samsungTestData = createTestCompany(
            4295877456L, // Samsung's real PermID  
            "Samsung Electronics Co Ltd",
            true, // is final assembler
            0.96, // high match score
            "005930.KS",
            "Technology",
            "Semiconductors",
            "South Korea"
        );
        
        qualcommTestData = createTestCompany(
            4295906319L, // Qualcomm's real PermID
            "QUALCOMM Inc",
            false, // not final assembler, chip designer
            0.94, // high match score
            "QCOM",
            "Technology", 
            "Semiconductors",
            "United States"
        );
    }

    @Test
    @DisplayName("Should save and retrieve company by PermID")
    void testSaveAndFindByPermId() {
        // Given: A company entity
        Company savedCompany = companyRepository.save(appleTestData);
        
        // When: Retrieving by PermID
        Optional<Company> foundCompany = companyRepository.findByPermid(appleTestData.getPermid());
        
        // Then: Company should be found with correct properties
        assertThat(foundCompany).isPresent();
        assertThat(foundCompany.get().getName()).isEqualTo("Apple Inc");
        assertThat(foundCompany.get().getIsFinalAssembler()).isTrue();
        assertThat(foundCompany.get().getMatchScore()).isEqualTo(0.98);
        assertThat(foundCompany.get().getTickerSymbol()).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("Should find companies by industry sector")
    void testFindByIndustrySector() {
        // Given: Multiple companies in different sectors
        companyRepository.saveAll(List.of(appleTestData, samsungTestData, qualcommTestData));
        
        // When: Finding by technology sector
        List<Company> techCompanies = companyRepository.findByIndustrySector("Technology");
        
        // Then: All three companies should be found
        assertThat(techCompanies).hasSize(3);
        assertThat(techCompanies)
            .extracting(Company::getName)
            .containsExactlyInAnyOrder("Apple Inc", "Samsung Electronics Co Ltd", "QUALCOMM Inc");
    }

    @Test
    @DisplayName("Should find final assemblers only")
    void testFindFinalAssemblers() {
        // Given: Mix of final assemblers and component suppliers
        companyRepository.saveAll(List.of(appleTestData, samsungTestData, qualcommTestData));
        
        // When: Finding final assemblers
        List<Company> finalAssemblers = companyRepository.findByIsFinalAssembler(true);
        
        // Then: Only Apple and Samsung should be found
        assertThat(finalAssemblers).hasSize(2);
        assertThat(finalAssemblers)
            .extracting(Company::getName)
            .containsExactlyInAnyOrder("Apple Inc", "Samsung Electronics Co Ltd");
    }

    @Test
    @DisplayName("Should find companies by country")
    void testFindByCountry() {
        // Given: Companies from different countries
        companyRepository.saveAll(List.of(appleTestData, samsungTestData, qualcommTestData));
        
        // When: Finding US companies
        List<Company> usCompanies = companyRepository.findByCountry("United States");
        
        // Then: Apple and Qualcomm should be found
        assertThat(usCompanies).hasSize(2);
        assertThat(usCompanies)
            .extracting(Company::getName)
            .containsExactlyInAnyOrder("Apple Inc", "QUALCOMM Inc");
    }

    @Test
    @DisplayName("Should find companies with high match scores")
    void testFindHighQualityMatches() {
        // Given: Companies with various match scores
        Company lowQualityMatch = createTestCompany(
            1234567890L, "Generic Corp", false, 0.75, "GEN", "Technology", "Other", "Unknown"
        );
        companyRepository.saveAll(List.of(appleTestData, samsungTestData, qualcommTestData, lowQualityMatch));
        
        // When: Finding companies with match score > 0.9
        List<Company> highQualityMatches = companyRepository.findByMatchScoreGreaterThan(0.9);
        
        // Then: Only the three high-quality matches should be found
        assertThat(highQualityMatches).hasSize(3);
        assertThat(highQualityMatches)
            .allMatch(company -> company.getMatchScore() > 0.9);
    }

    @Test
    @DisplayName("Should perform complex search by name pattern")
    void testSearchByNamePattern() {
        // Given: Companies with various names
        companyRepository.saveAll(List.of(appleTestData, samsungTestData, qualcommTestData));
        
        // When: Searching for companies with "Inc" in name
        List<Company> incCompanies = companyRepository.searchByNamePattern(".*Inc.*");
        
        // Then: Apple and Qualcomm should be found
        assertThat(incCompanies).hasSize(2);
        assertThat(incCompanies)
            .extracting(Company::getName)
            .containsExactlyInAnyOrder("Apple Inc", "QUALCOMM Inc");
    }

    @Test
    @DisplayName("Should count companies by business type")
    void testCountByBusinessType() {
        // Given: Companies with different business types
        companyRepository.saveAll(List.of(appleTestData, samsungTestData, qualcommTestData));
        
        // When: Counting by business type
        long semiconductorCount = companyRepository.countByBusinessType("Semiconductors");
        long electronicsCount = companyRepository.countByBusinessType("Consumer Electronics");
        
        // Then: Counts should match expected values
        assertThat(semiconductorCount).isEqualTo(2); // Samsung and Qualcomm
        assertThat(electronicsCount).isEqualTo(1);   // Apple
    }

    @Test
    @DisplayName("Should update company centrality score")
    void testUpdateCentralityScore() {
        // Given: A saved company
        Company savedCompany = companyRepository.save(appleTestData);
        
        // When: Updating centrality score
        companyRepository.updateCentralityScore(savedCompany.getPermid(), 0.85);
        
        // Then: Score should be updated
        Optional<Company> updatedCompany = companyRepository.findByPermid(savedCompany.getPermid());
        assertThat(updatedCompany).isPresent();
        assertThat(updatedCompany.get().getCentralityScore()).isEqualTo(0.85);
    }

    @Test
    @DisplayName("Should assign community ID")
    void testAssignCommunityId() {
        // Given: Companies without community assignments
        List<Company> savedCompanies = companyRepository.saveAll(List.of(appleTestData, samsungTestData));
        
        // When: Assigning to the same community
        companyRepository.assignCommunityId(List.of(
            savedCompanies.get(0).getPermid(),
            savedCompanies.get(1).getPermid()
        ), 42L);
        
        // Then: Both should have the same community ID
        List<Company> updatedCompanies = companyRepository.findByPermidIn(List.of(
            savedCompanies.get(0).getPermid(),
            savedCompanies.get(1).getPermid()
        ));
        
        assertThat(updatedCompanies)
            .allMatch(company -> company.getCommunityId().equals(42L));
    }

    @Test
    @DisplayName("Should handle large batch operations efficiently")
    void testBatchOperations() {
        // Given: A large number of companies (simulate real-world data volume)
        List<Company> batchCompanies = generateTestCompanies(100);
        
        // When: Performing batch save
        long startTime = System.currentTimeMillis();
        List<Company> savedCompanies = companyRepository.saveAll(batchCompanies);
        long endTime = System.currentTimeMillis();
        
        // Then: All should be saved efficiently
        assertThat(savedCompanies).hasSize(100);
        assertThat(endTime - startTime).isLessThan(5000); // Should complete within 5 seconds
        
        // Verify batch retrieval
        List<Company> allCompanies = companyRepository.findAll();
        assertThat(allCompanies).hasSize(100);
    }

    /**
     * Helper method to create test company data
     */
    private Company createTestCompany(Long permid, String name, Boolean isFinalAssembler, 
                                    Double matchScore, String ticker, String sector, 
                                    String businessType, String country) {
        Company company = new Company();
        company.setPermid(permid);
        company.setName(name);
        company.setIsFinalAssembler(isFinalAssembler);
        company.setMatchScore(matchScore);
        company.setTickerSymbol(ticker);
        company.setIndustrySector(sector);
        company.setBusinessType(businessType);
        company.setCountry(country);
        company.setLastUpdated(LocalDateTime.now());
        company.setDataSource("TEST_DATA");
        company.setCentralityScore(0.0);
        return company;
    }

    /**
     * Helper method to generate multiple test companies for batch testing
     */
    private List<Company> generateTestCompanies(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> createTestCompany(
                (long) (1000000000 + i),
                "Test Company " + i,
                i % 3 == 0, // Every third company is a final assembler
                0.8 + (i % 20) * 0.01, // Match scores between 0.8 and 0.99
                "TEST" + i,
                "Technology",
                i % 2 == 0 ? "Semiconductors" : "Consumer Electronics",
                i % 5 == 0 ? "United States" : "Other"
            ))
            .toList();
    }
}