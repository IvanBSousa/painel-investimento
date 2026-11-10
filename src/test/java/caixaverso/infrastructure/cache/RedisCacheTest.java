package caixaverso.infrastructure.cache;

import caixaverso.application.usecase.InvestimentoUseCase;
import caixaverso.application.usecase.PerfilRiscoUseCase;
import caixaverso.application.usecase.ProdutoUseCase;
import caixaverso.application.usecase.SimulacaoUseCase;
import caixaverso.domain.enums.PerfilRiscoEnum;
import caixaverso.infrastructure.persistence.entity.ProdutoEntity;
import caixaverso.infrastructure.persistence.repository.TelemetriaRepositoryImpl;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CompositeCacheKey;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RedisCacheTest {

    @Inject
    CacheManager caches;
    @Inject
    EntityManager em;
    @Inject
    InvestimentoUseCase investimentos;
    @Inject
    PerfilRiscoUseCase perfis;
    @Inject
    ProdutoUseCase produtos;
    @Inject
    SimulacaoUseCase simulacoes;
    @Inject
    TelemetriaRepositoryImpl telemetria;

    @BeforeEach
    @AfterEach
    void limparCaches() {
        for (CacheChange change : CacheChange.values()) {
            for (String name : change.cacheNames()) {
                caches.getCache(name).orElseThrow().invalidateAll().await().indefinitely();
            }
        }
    }

    @Test
    void serializaEDesserializaOsDadosDeTodosOsGets() {
        var carteira = investimentos.listarPorCliente(1L);
        assertFalse(carteira.isEmpty());
        assertEquals(carteira, investimentos.listarPorCliente(1L));
        assertCached("investimentos", 1L, carteira);
        assertTrue(investimentos.listarPorCliente(-1L).isEmpty());

        var recomendacoes = produtos.buscarProdutosPorPerfil(PerfilRiscoEnum.CONSERVADOR);
        assertFalse(recomendacoes.isEmpty());
        assertEquals(recomendacoes, produtos.buscarProdutosPorPerfil(PerfilRiscoEnum.CONSERVADOR));
        assertCached("produtos-recomendados", PerfilRiscoEnum.CONSERVADOR, recomendacoes);
        assertNotEquals(recomendacoes, produtos.buscarProdutosPorPerfil(PerfilRiscoEnum.AGRESSIVO));

        var perfil = perfis.calcularPerfil(1L);
        assertEquals(perfil, perfis.calcularPerfil(1L));
        assertCached("perfil-risco", 1L, perfil);
        assertNotEquals(perfil.clienteId(), perfis.calcularPerfil(-1L).clienteId());

        var historico = simulacoes.listarTodos(0, 10);
        assertEquals(historico, simulacoes.listarTodos(0, 10));
        assertCached("simulacoes", new CompositeCacheKey(0, 10), historico);
        var agrupamento = simulacoes.agregacaoPorProdutoDia();
        assertEquals(agrupamento, simulacoes.agregacaoPorProdutoDia());
        assertCached("simulacoes-por-produto-dia",
                caches.getCache("simulacoes-por-produto-dia").orElseThrow().getDefaultKey(), agrupamento);

        LocalDate hoje = LocalDate.now();
        var chamadas = telemetria.agrupaPorServicoEPeriodo(hoje, hoje);
        assertFalse(chamadas.isEmpty());
        assertEquals(chamadas, telemetria.agrupaPorServicoEPeriodo(hoje, hoje));
        assertCached("telemetria", new CompositeCacheKey(hoje, hoje), chamadas);
        assertTrue(telemetria.agrupaPorServicoEPeriodo(hoje.plusYears(100), hoje.plusYears(100)).isEmpty());
        var perfilItem = chamadas.stream().filter(item -> item.nome().equals("perfil-risco"))
                .findFirst().orElseThrow();
        assertTrue(perfilItem.quantidadeChamadas() >= 3);
    }

    @Test
    void invalidaProdutosSomenteDepoisDoCommit() {
        var antes = produtos.buscarProdutosPorPerfil(PerfilRiscoEnum.CONSERVADOR);
        ProdutoEntity novo = new ProdutoEntity(null, "Redis commit test", "CDB",
                new BigDecimal("0.12"), "Baixo");

        try {
            QuarkusTransaction.requiringNew().run(() -> {
                em.persist(novo);
                em.flush();
                assertEquals(antes, produtos.buscarProdutosPorPerfil(PerfilRiscoEnum.CONSERVADOR));
            });

            var depois = produtos.buscarProdutosPorPerfil(PerfilRiscoEnum.CONSERVADOR);
            assertEquals(antes.size() + 1, depois.size());
            assertEquals(depois, produtos.buscarProdutosPorPerfil(PerfilRiscoEnum.CONSERVADOR));
        } finally {
            if (novo.getId() != null) {
                QuarkusTransaction.requiringNew().run(() -> {
                    ProdutoEntity persistido = em.find(ProdutoEntity.class, novo.getId());
                    if (persistido != null) {
                        em.remove(persistido);
                    }
                });
            }
        }

        assertEquals(antes, produtos.buscarProdutosPorPerfil(PerfilRiscoEnum.CONSERVADOR));
    }

    @Test
    void rollbackNaoInvalidaCache() {
        var antes = produtos.buscarProdutosPorPerfil(PerfilRiscoEnum.CONSERVADOR);
        var cache = caches.getCache("produtos-recomendados").orElseThrow();

        assertThrows(IllegalStateException.class, () -> QuarkusTransaction.requiringNew().run(() -> {
            em.persist(new ProdutoEntity(null, "Redis rollback test", "CDB",
                    new BigDecimal("0.12"), "Baixo"));
            em.flush();
            throw new IllegalStateException("rollback");
        }));

        var armazenado = cache.get(PerfilRiscoEnum.CONSERVADOR, key -> {
            fail("Rollback must not invalidate an existing cache entry");
            return antes;
        }).await().indefinitely();
        assertEquals(antes, armazenado);
    }

    private void assertCached(String name, Object key, Object expected) {
        Object value = caches.getCache(name).orElseThrow().get(key, ignored -> {
            throw new AssertionError("Expected Redis cache hit: " + name);
        }).await().indefinitely();
        assertEquals(expected, value);
    }
}
