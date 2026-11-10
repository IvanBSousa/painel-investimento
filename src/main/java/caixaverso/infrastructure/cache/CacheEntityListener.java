package caixaverso.infrastructure.cache;

import caixaverso.infrastructure.persistence.entity.InvestimentoEntity;
import caixaverso.infrastructure.persistence.entity.ProdutoEntity;
import caixaverso.infrastructure.persistence.entity.SimulacaoEntity;
import caixaverso.infrastructure.persistence.entity.TelemetriaEntity;
import caixaverso.domain.entity.Telemetria;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

@Dependent
public class CacheEntityListener {

    @Inject
    Event<CacheChange> changes;

    @PostPersist
    @PostUpdate
    @PostRemove
    public void changed(Object entity) {
        if (entity instanceof InvestimentoEntity) {
            changes.fire(CacheChange.INVESTIMENTOS);
        } else if (entity instanceof ProdutoEntity) {
            changes.fire(CacheChange.PRODUTOS);
        } else if (entity instanceof SimulacaoEntity) {
            changes.fire(CacheChange.SIMULACOES);
        } else if (entity instanceof TelemetriaEntity || entity instanceof Telemetria) {
            changes.fire(CacheChange.TELEMETRIA);
        } else {
            throw new IllegalArgumentException("Unsupported cache entity: " + entity.getClass().getName());
        }
    }
}
