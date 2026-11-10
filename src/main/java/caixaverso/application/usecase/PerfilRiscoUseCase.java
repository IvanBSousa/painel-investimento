package caixaverso.application.usecase;

import caixaverso.application.dto.PerfilRiscoResponseDTO;
import caixaverso.infrastructure.persistence.entity.InvestimentoEntity;
import caixaverso.infrastructure.persistence.entity.TelemetriaEntity;
import caixaverso.infrastructure.persistence.repository.InvestimentoRepositoryImpl;
import caixaverso.infrastructure.persistence.repository.TelemetriaRepositoryImpl;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.cache.CacheResult;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class PerfilRiscoUseCase {

    private final InvestimentoRepositoryImpl investimentoRepository;
    private final TelemetriaRepositoryImpl telemetriaRepository;

    public PerfilRiscoUseCase(InvestimentoRepositoryImpl investimentoRepository,
                              TelemetriaRepositoryImpl telemetriaRepository) {
        this.telemetriaRepository = telemetriaRepository;
        this.investimentoRepository = investimentoRepository;
    }

    public PerfilRiscoResponseDTO calcularPerfil(Long clienteId) {
        Long inicio = System.currentTimeMillis();
        PerfilRiscoResponseDTO perfil = calcularPerfilCacheado(clienteId);

        TelemetriaEntity dadosTelemetria = new TelemetriaEntity();
        dadosTelemetria.setNomeServico("perfil-risco");
        dadosTelemetria.setTempoRespostaMs(System.currentTimeMillis() - inicio);
        dadosTelemetria.setDataChamada(LocalDate.now());
        telemetriaRepository.salvar(dadosTelemetria);

        return perfil;
    }

    @CacheResult(cacheName = "perfil-risco")
    public PerfilRiscoResponseDTO calcularPerfilCacheado(Long clienteId) {
        List<InvestimentoEntity> historico = investimentoRepository.listarPorCliente(clienteId);

        if (historico.isEmpty()) {
            return new PerfilRiscoResponseDTO(clienteId,"Conservador",20,
                    "Sem histórico. Perfil considerado conservador por padrão.");
        }

        int pontuacao = 0;

        var totalAplicado = historico.stream()
                .map(i -> i.getValor())
                .reduce((a, b) -> a.add(b))
                .orElseThrow();

        if (totalAplicado.doubleValue() > 50000) pontuacao += 30;
        else if (totalAplicado.doubleValue() > 20000) pontuacao += 20;
        else pontuacao += 10;

        int movimentacoes = historico.size();
        if (movimentacoes > 10) pontuacao += 30;
        else if (movimentacoes > 5) pontuacao += 20;
        else pontuacao += 10;

        long agressivos = historico.stream()
                .filter(i -> i.getTipo().toLowerCase().contains("fundo") ||
                        i.getTipo().toLowerCase().contains("ações") ||
                        i.getTipo().toLowerCase().contains("multimercado"))
                .count();

        if (agressivos > 5) pontuacao += 40;
        else if (agressivos > 2) pontuacao += 25;
        else pontuacao += 5;

        String perfil;
        String descricao;

        if (pontuacao >= 70) {
            perfil = "Agressivo";
            descricao = "Busca maior rentabilidade e assume mais risco.";
        } else if (pontuacao >= 30) {
            perfil = "Moderado";
            descricao = "Equilíbrio entre segurança e rentabilidade.";
        } else {
            perfil = "Conservador";
            descricao = "Prioriza segurança e liquidez.";
        }

        return new PerfilRiscoResponseDTO(clienteId, perfil, pontuacao, descricao);
    }
}
