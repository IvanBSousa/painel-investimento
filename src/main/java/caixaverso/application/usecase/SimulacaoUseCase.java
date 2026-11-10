package caixaverso.application.usecase;

import caixaverso.application.dto.SimulacaoAgrupadaResponseDTO;
import caixaverso.application.dto.SimulacaoHistoricoDTO;
import caixaverso.application.dto.SimulacaoRequestDTO;
import caixaverso.application.dto.SimulacaoResponseDTO;
import caixaverso.infrastructure.mapper.SimulacaoHistoricoMapper;
import caixaverso.infrastructure.mapper.SimulacaoResponseMapper;
import caixaverso.infrastructure.persistence.entity.ProdutoEntity;
import caixaverso.infrastructure.persistence.entity.SimulacaoEntity;
import caixaverso.infrastructure.persistence.entity.TelemetriaEntity;
import caixaverso.infrastructure.persistence.repository.ProdutoRepositoryImpl;
import caixaverso.infrastructure.persistence.repository.SimulacaoRepositoryImpl;
import caixaverso.infrastructure.persistence.repository.TelemetriaRepositoryImpl;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.cache.CacheResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class SimulacaoUseCase {

    private final ProdutoRepositoryImpl produtoRepositoryImpl;
    private final SimulacaoRepositoryImpl simulacaoRepository;
    private final SimulacaoHistoricoMapper historicoMapper;
    private final SimulacaoResponseMapper responseMapper;
    private final TelemetriaRepositoryImpl telemetriaRepository;

    public SimulacaoUseCase(ProdutoRepositoryImpl produtoRepositoryImpl, SimulacaoRepositoryImpl simulacaoRepository,
                            SimulacaoHistoricoMapper historicoMapper, SimulacaoResponseMapper responseMapper,
                            TelemetriaRepositoryImpl telemetriaRepository) {
        this.telemetriaRepository = telemetriaRepository;
        this.produtoRepositoryImpl = produtoRepositoryImpl;
        this.simulacaoRepository = simulacaoRepository;
        this.historicoMapper = historicoMapper;
        this.responseMapper = responseMapper;
    }

    @CacheResult(cacheName = "simulacoes")
    public List<SimulacaoHistoricoDTO> listarTodos(int page, int size) {
        List<SimulacaoEntity> lista = simulacaoRepository.listarPaginado(page, size);

        return lista.stream()
                .map(historicoMapper::toDTO)
                .toList();
    }

    public SimulacaoEntity simularInterno(Long clienteId, BigDecimal valor, int prazoMeses, String nomeProduto) {

        ProdutoEntity produto = produtoRepositoryImpl.findByName(nomeProduto)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        BigDecimal mensal = converterTaxaAnualParaMensal(produto.getRentabilidade());
        BigDecimal valorFinal = calcularJurosCompostos(valor, mensal, prazoMeses);

        SimulacaoEntity entity = new SimulacaoEntity();
        entity.setProduto(produto.getNome());
        entity.setValorInvestido(valor);
        entity.setValorFinal(valorFinal);
        entity.setRentabilidadeEfetiva(produto.getRentabilidade());
        entity.setPrazoMeses(prazoMeses);
        entity.setDataSimulacao(Instant.now());
        entity.setClienteId(clienteId);

        simulacaoRepository.salvar(entity);
        return entity;
    }

    public SimulacaoResponseDTO simular(SimulacaoRequestDTO request) {
        Long inicio = System.currentTimeMillis();

        SimulacaoEntity entity = simularInterno(
                request.clienteId(),
                request.valorInvestido(),
                request.prazoMeses(),
                request.produto()
        );

        Long fim = System.currentTimeMillis();
        Long tempoExecucao = fim - inicio;

        TelemetriaEntity dadosTelemetria = new TelemetriaEntity();
        dadosTelemetria.setNomeServico("simular-investimento");
        dadosTelemetria.setTempoRespostaMs(tempoExecucao);
        dadosTelemetria.setDataChamada(LocalDate.now());

        telemetriaRepository.salvar(dadosTelemetria);

        return responseMapper.toResponse(
            entity,
            produtoRepositoryImpl.findByName(request.produto())
                .orElseThrow(() -> new RuntimeException("Produto não encontrado")
        ));
    }

    private BigDecimal converterTaxaAnualParaMensal(BigDecimal taxaAnual) {
        return BigDecimal.valueOf(Math.pow(1 + taxaAnual.doubleValue(), 1.0 / 12.0) - 1)
                .setScale(10, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularJurosCompostos(BigDecimal valor, BigDecimal taxaMensal, int meses) {
        double vf = valor.doubleValue() * Math.pow(1 + taxaMensal.doubleValue(), meses);
        return BigDecimal.valueOf(vf).setScale(2, RoundingMode.HALF_UP);
    }

    @CacheResult(cacheName = "simulacoes-por-produto-dia")
    public List<SimulacaoAgrupadaResponseDTO> agregacaoPorProdutoDia() {
        return simulacaoRepository.agruparPorProdutoDia();
    }
}
