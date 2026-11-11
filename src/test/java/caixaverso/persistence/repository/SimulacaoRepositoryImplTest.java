package caixaverso.persistence.repository;

import caixaverso.application.dto.SimulacaoAgrupadaResponseDTO;
import caixaverso.infrastructure.persistence.entity.SimulacaoEntity;
import caixaverso.infrastructure.persistence.repository.SimulacaoRepositoryImpl;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimulacaoRepositoryImplTest {

    @InjectMocks
    SimulacaoRepositoryImpl repository;

    @Mock
    EntityManager em;

    @Mock
    TypedQuery<SimulacaoEntity> typedQuery;

    @Mock
    Query query;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void salvar_quandoIdNull_deveChamarPersist() {
        SimulacaoEntity entity = new SimulacaoEntity();
        entity.setId(null);

        repository.salvar(entity);

        verify(em, times(1)).persist(entity);
        verify(em, never()).merge(any());
    }

    @Test
    void salvar_quandoIdNaoNull_deveChamarMerge() {
        SimulacaoEntity entity = new SimulacaoEntity();
        entity.setId(10L);

        repository.salvar(entity);

        verify(em, times(1)).merge(entity);
        verify(em, never()).persist(any());
    }

    @Test
    void agruparPorProdutoDia_deveRetornarDTOsConvertidos() {

        when(em.createQuery(anyString()))
                .thenReturn(query);

        Object[] row = new Object[]{
                "CDB",
                LocalDate.of(2025, 1, 10),
                2L,
                BigDecimal.ZERO,
                new BigDecimal("3000")
        };

        List<Object[]> rows = new ArrayList<>();
        rows.add(row);

        when(query.getResultList()).thenReturn(rows);

        List<SimulacaoAgrupadaResponseDTO> result = repository.agruparPorProdutoDia();

        assertEquals(1, result.size());

        SimulacaoAgrupadaResponseDTO dto = result.get(0);
        assertEquals("CDB", dto.produto());
        assertEquals(LocalDate.of(2025, 1, 10), dto.data());
        assertEquals(2L, dto.quantidadeSimulacoes());
        assertEquals(new BigDecimal("1500.00"), dto.mediaValorFinal());
    }

    @Test
    void agruparPorProdutoDia_deveArredondarMediaPeriodicaComHalfUp() {
        when(em.createQuery(anyString())).thenReturn(query);
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"CDB", LocalDate.of(2025, 1, 10), 3L,
                BigDecimal.ZERO, new BigDecimal("100.01")});
        rows.add(new Object[]{"LCI", LocalDate.of(2025, 1, 10), 2L,
                BigDecimal.ZERO, new BigDecimal("2.01")});
        when(query.getResultList()).thenReturn(rows);

        var result = repository.agruparPorProdutoDia();

        assertEquals(new BigDecimal("33.34"), result.get(0).mediaValorFinal());
        assertEquals(new BigDecimal("1.01"), result.get(1).mediaValorFinal());
    }

    @Test
    void agruparPorProdutoDia_deveAceitarDate_do_SQL() {

        when(em.createQuery(anyString()))
                .thenReturn(query);

        Object[] row = new Object[]{
                "LCI",
                java.sql.Date.valueOf("2024-12-25"),
                1L,
                BigDecimal.ZERO,
                new BigDecimal("500")
        };

        List<Object[]> rows = new ArrayList<>();
        rows.add(row);

        when(query.getResultList()).thenReturn(rows);

        List<SimulacaoAgrupadaResponseDTO> result = repository.agruparPorProdutoDia();

        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2024, 12, 25), result.get(0).data());
    }

    @Test
    void agruparPorProdutoDia_quandoQuantidadeZero_deveEvitarDivisaoPorZero() {

        when(em.createQuery(anyString()))
                .thenReturn(query);

        Object[] row = new Object[]{
                "CDB",
                LocalDate.of(2025, 1, 10),
                0L,
                BigDecimal.ZERO,
                new BigDecimal("0")
        };

        List<Object[]> rows = new ArrayList<>();
        rows.add(row);

        when(query.getResultList()).thenReturn(rows);

        List<SimulacaoAgrupadaResponseDTO> result = repository.agruparPorProdutoDia();

        assertEquals(1, result.size());
        assertEquals(BigDecimal.ZERO, result.get(0).mediaValorFinal());
    }

    @Test
    void listarPaginado_deveRetornarListaPaginada() {

        // Arrange
        SimulacaoEntity s1 = new SimulacaoEntity();
        SimulacaoEntity s2 = new SimulacaoEntity();

        when(em.createQuery(anyString(), eq(SimulacaoEntity.class)))
                .thenReturn(typedQuery);

        when(typedQuery.setFirstResult(anyInt())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);

        when(typedQuery.getResultList())
                .thenReturn(List.of(s1, s2));

        // Act
        List<SimulacaoEntity> result = repository.listarPaginado(1, 2);

        // Assert
        assertEquals(2, result.size());
    }
}
