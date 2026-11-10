package caixaverso.infrastructure.cache;

import caixaverso.infrastructure.persistence.entity.InvestimentoEntity;
import caixaverso.infrastructure.persistence.entity.ProdutoEntity;
import caixaverso.infrastructure.persistence.entity.SimulacaoEntity;
import caixaverso.infrastructure.persistence.entity.TelemetriaEntity;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CacheInvalidationTest {

    @Test
    @SuppressWarnings("unchecked")
    void identificaTodosOsTiposDeAlteracao() {
        CacheEntityListener listener = new CacheEntityListener();
        Event<CacheChange> events = mock(Event.class);
        listener.changes = events;

        listener.changed(new InvestimentoEntity());
        listener.changed(new ProdutoEntity());
        listener.changed(new SimulacaoEntity());
        listener.changed(new TelemetriaEntity());
        listener.changed(new caixaverso.domain.entity.Telemetria());

        verify(events).fire(CacheChange.INVESTIMENTOS);
        verify(events).fire(CacheChange.PRODUTOS);
        verify(events).fire(CacheChange.SIMULACOES);
        verify(events, times(2)).fire(CacheChange.TELEMETRIA);
        assertThrows(IllegalArgumentException.class, () -> listener.changed(new Object()));
    }

    @Test
    void invalidaSomenteOsCachesAfetados() {
        CacheInvalidation invalidation = new CacheInvalidation();
        invalidation.cacheManager = mock(CacheManager.class);
        Cache paginas = mock(Cache.class);
        Cache agregacoes = mock(Cache.class);
        when(invalidation.cacheManager.getCache("simulacoes")).thenReturn(Optional.of(paginas));
        when(invalidation.cacheManager.getCache("simulacoes-por-produto-dia"))
                .thenReturn(Optional.of(agregacoes));
        when(paginas.invalidateAll()).thenReturn(Uni.createFrom().voidItem());
        when(agregacoes.invalidateAll()).thenReturn(Uni.createFrom().voidItem());

        invalidation.afterCommit(CacheChange.SIMULACOES);

        verify(paginas).invalidateAll();
        verify(agregacoes).invalidateAll();
        verify(invalidation.cacheManager).getCache("simulacoes");
        verify(invalidation.cacheManager).getCache("simulacoes-por-produto-dia");
        verifyNoMoreInteractions(invalidation.cacheManager);
    }

    @Test
    void falhaExplicitamenteSeCacheNaoExiste() {
        CacheInvalidation invalidation = new CacheInvalidation();
        invalidation.cacheManager = mock(CacheManager.class);
        when(invalidation.cacheManager.getCache("telemetria")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> invalidation.afterCommit(CacheChange.TELEMETRIA));
    }
}
