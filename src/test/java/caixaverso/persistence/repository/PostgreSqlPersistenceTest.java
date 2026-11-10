package caixaverso.persistence.repository;

import caixaverso.infrastructure.persistence.entity.ProdutoEntity;
import caixaverso.infrastructure.persistence.entity.SimulacaoEntity;
import caixaverso.infrastructure.persistence.entity.TelemetriaEntity;
import caixaverso.infrastructure.persistence.repository.InvestimentoRepositoryImpl;
import caixaverso.infrastructure.persistence.repository.ProdutoRepositoryImpl;
import caixaverso.infrastructure.persistence.repository.SimulacaoRepositoryImpl;
import caixaverso.infrastructure.persistence.repository.TelemetriaRepositoryImpl;
import caixaverso.infrastructure.persistence.repository.UsuarioRepositoryImpl;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PostgreSqlPersistenceTest {

    @Inject
    EntityManager em;

    @Inject
    ProdutoRepositoryImpl produtos;

    @Inject
    UsuarioRepositoryImpl usuarios;

    @Inject
    InvestimentoRepositoryImpl investimentos;

    @Inject
    SimulacaoRepositoryImpl simulacoes;

    @Inject
    TelemetriaRepositoryImpl telemetria;

    @Test
    @TestTransaction
    void carregaDadosIniciaisNoPostgreSql() {
        String database = em.unwrap(Session.class).doReturningWork(
                connection -> connection.getMetaData().getDatabaseProductName());

        assertEquals("PostgreSQL", database);
        assertEquals(2, usuarios.count());
        assertEquals(30, produtos.count());
        assertEquals(30L, em.createQuery(
                "SELECT COUNT(i) FROM InvestimentoEntity i", Long.class).getSingleResult());
        assertEquals("Admin", usuarios.findByUsername("admin").orElseThrow().getPapel());
        assertEquals("User", usuarios.findByUsername("user").orElseThrow().getPapel());
        assertEquals(new BigDecimal("0.10"), produtos.findByIdOptional(1)
                .orElseThrow().getRentabilidade());
        assertEquals(10, investimentos.listarPorCliente(1L).size());
    }

    @Test
    @TestTransaction
    void geraIdentidadeDeProdutoAposCargaInicial() {
        ProdutoEntity produto = new ProdutoEntity(
                null, "Produto PostgreSQL", "CDB", new BigDecimal("0.12"), "Baixo");

        produtos.persistProduto(produto);
        em.flush();
        em.clear();

        assertNotNull(produto.getId());
        assertTrue(produto.getId() > 30);
        assertEquals(produto.getId(), produtos.findByName("Produto PostgreSQL")
                .orElseThrow().getId());
    }

    @Test
    @TestTransaction
    void persistePaginaEAgrupaSimulacoesPorDia() {
        SimulacaoEntity primeira = novaSimulacao("2026-01-15T10:00:00Z", "1120.00");
        SimulacaoEntity segunda = novaSimulacao("2026-01-15T11:00:00Z", "1240.00");
        simulacoes.salvar(primeira);
        simulacoes.salvar(segunda);
        em.flush();
        em.clear();

        assertNotNull(primeira.getId());
        assertNotNull(segunda.getId());
        assertNotEquals(primeira.getId(), segunda.getId());
        assertEquals(segunda.getId(), simulacoes.listarPaginado(0, 1).getFirst().getId());
        assertEquals(primeira.getId(), simulacoes.listarPaginado(1, 1).getFirst().getId());
        assertTrue(simulacoes.listarPaginado(2, 1).isEmpty());

        var grupos = simulacoes.agruparPorProdutoDia();
        assertEquals(1, grupos.size());
        var grupo = grupos.getFirst();
        assertEquals("Produto PostgreSQL", grupo.produto());
        assertEquals(LocalDate.of(2026, 1, 15), grupo.data());
        assertEquals(2L, grupo.quantidadeSimulacoes());
        assertEquals(0, new BigDecimal("1180.00").compareTo(grupo.mediaValorFinal()));
    }

    @Test
    @TestTransaction
    void persisteEAgrupaTelemetriaPorPeriodo() {
        LocalDate dia = LocalDate.of(2026, 1, 15);
        TelemetriaEntity primeira = new TelemetriaEntity(null, "postgres-test", dia, 100L);
        telemetria.salvar(primeira);
        telemetria.salvar(new TelemetriaEntity(null, "postgres-test", dia, 200L));
        telemetria.salvar(new TelemetriaEntity(null, "postgres-test", dia.minusDays(1), 900L));
        em.flush();
        em.clear();

        assertNotNull(primeira.getId());
        var grupos = telemetria.agrupaPorServicoEPeriodo(dia, dia);
        assertEquals(1, grupos.size());
        assertEquals("postgres-test", grupos.getFirst().nome());
        assertEquals(2L, grupos.getFirst().quantidadeChamadas());
        assertEquals(150.0, grupos.getFirst().mediaTempoRespostaMs());
    }

    private SimulacaoEntity novaSimulacao(String instante, String valorFinal) {
        return new SimulacaoEntity(null, 1L, "Produto PostgreSQL",
                new BigDecimal("1000.00"), new BigDecimal(valorFinal),
                new BigDecimal("0.12"), 12, Instant.parse(instante));
    }
}
