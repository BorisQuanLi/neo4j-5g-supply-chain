package com.jefferies.supplychain.unit.service;

import com.jefferies.supplychain.model.Company;
import com.jefferies.supplychain.repository.CompanyRepository;
import com.jefferies.supplychain.service.GraphAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GraphAnalyticsService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Graph Analytics Service Unit Tests")
class GraphAnalyticsServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    private GraphAnalyticsService graphAnalyticsService;

    @BeforeEach
    void setUp() {
        graphAnalyticsService = new GraphAnalyticsService(companyRepository);
    }

    @Test
    @DisplayName("Should find company by name")
    void testFindCompanyByName() {
        // Given
        String companyName = "Apple Inc";
        Company mockCompany = new Company();
        mockCompany.setName(companyName);
        when(companyRepository.findByName(companyName)).thenReturn(Optional.of(mockCompany));

        // When
        Optional<Company> result = graphAnalyticsService.findCompanyByName(companyName);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(companyName);
        verify(companyRepository).findByName(companyName);
    }

    @Test
    @DisplayName("Should analyze critical nodes")
    void testAnalyzeCriticalNodes() throws Exception {
        // Given
        when(companyRepository.calculatePageRankCentrality(10)).thenReturn(List.of());

        // When
        CompletableFuture<List<Company.CentralityResult>> future = graphAnalyticsService.analyzeCriticalNodes(10);
        List<Company.CentralityResult> results = future.get();

        // Then
        assertThat(results).isNotNull();
        verify(companyRepository).calculatePageRankCentrality(10);
    }

    @Test
    @DisplayName("Should analyze bridge nodes")
    void testAnalyzeBridgeNodes() throws Exception {
        // Given
        when(companyRepository.calculateBetweennessCentrality(15)).thenReturn(List.of());

        // When
        CompletableFuture<List<Company.CentralityResult>> future = graphAnalyticsService.analyzeBridgeNodes(15);
        List<Company.CentralityResult> results = future.get();

        // Then
        assertThat(results).isNotNull();
        verify(companyRepository).calculateBetweennessCentrality(15);
    }

    @Test
    @DisplayName("Should detect supply chain communities")
    void testDetectSupplyChainCommunities() throws Exception {
        // Given
        when(companyRepository.detectCommunities()).thenReturn(List.of());

        // When
        CompletableFuture<List<Object>> future = graphAnalyticsService.detectSupplyChainCommunities();
        List<Object> results = future.get();

        // Then
        assertThat(results).isNotNull();
        verify(companyRepository).detectCommunities();
    }

    @Test
    @DisplayName("Should find backup supplier routes")
    void testFindBackupSupplierRoutes() {
        // Given
        String startCompany = "Apple Inc";
        String endCompany = "Samsung";
        when(companyRepository.findOptimalSupplyPath(startCompany, endCompany)).thenReturn(List.of());

        // When
        List<Company.PathResult> results = graphAnalyticsService.findBackupSupplierRoutes(startCompany, endCompany);

        // Then
        assertThat(results).isNotNull();
        verify(companyRepository).findOptimalSupplyPath(startCompany, endCompany);
    }

    @Test
    @DisplayName("Should validate input parameters")
    void testInputValidation() {
        // When/Then: Should throw exception for null company name
        assertThatThrownBy(() -> graphAnalyticsService.findCompanyByName(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Company name cannot be null or empty");

        assertThatThrownBy(() -> graphAnalyticsService.findCompanyByName(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Company name cannot be null or empty");
    }
}