package caixaverso.infrastructure.cache;

import io.quarkus.cache.CacheManager;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CacheInvalidation {

    private static final Logger LOG = Logger.getLogger(CacheInvalidation.class);

    @Inject
    CacheManager cacheManager;

    public void afterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) CacheChange change) {
        invalidate(change);
    }

    public void onStart(@Observes StartupEvent event) {
        // The database can be recreated on startup; Redis must not retain old results.
        for (CacheChange change : CacheChange.values()) {
            invalidate(change);
        }
    }

    private void invalidate(CacheChange change) {
        for (String name : change.cacheNames()) {
            cacheManager.getCache(name).orElseThrow(
                    () -> new IllegalStateException("Cache not configured: " + name))
                    .invalidateAll()
                    .onFailure().invoke(failure -> LOG.errorf(failure,
                            "Failed to invalidate Redis cache '%s'; entries remain until TTL expires", name))
                    .await().indefinitely();
        }
    }
}
