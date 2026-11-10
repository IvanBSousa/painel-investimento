package caixaverso.infrastructure.cache;

public enum CacheChange {
    INVESTIMENTOS("investimentos", "perfil-risco"),
    PRODUTOS("produtos-recomendados"),
    SIMULACOES("simulacoes", "simulacoes-por-produto-dia"),
    TELEMETRIA("telemetria");

    private final String[] cacheNames;

    CacheChange(String... cacheNames) {
        this.cacheNames = cacheNames;
    }

    public String[] cacheNames() {
        return cacheNames.clone();
    }
}
