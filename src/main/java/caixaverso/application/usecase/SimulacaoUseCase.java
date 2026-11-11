package caixaverso.application.usecase;

import caixaverso.application.dto.SimulacaoAgrupadaResponseDTO;
import caixaverso.application.dto.SimulacaoHistoricoDTO;
import caixaverso.application.dto.SimulacaoRequestDTO;
import caixaverso.application.dto.SimulacaoResponseDTO;
import caixaverso.application.exception.EntradaInvalidaException;
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
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class SimulacaoUseCase {

    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("9999999999999999.99");

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
        if (page < 0 || size < 1 || (long) page * size > Integer.MAX_VALUE) {
            throw new EntradaInvalidaException(
                    "Paginação inválida: page deve ser não negativo, size positivo e page * size não pode exceder 2147483647.");
        }
        List<SimulacaoEntity> lista = simulacaoRepository.listarPaginado(page, size);

        return lista.stream()
                .map(historicoMapper::toDTO)
                .toList();
    }

    public SimulacaoEntity simularInterno(Long clienteId, BigDecimal valor, int prazoMeses, String nomeProduto) {
        if (prazoMeses < 1 || prazoMeses > SimulacaoRequestDTO.PRAZO_MAXIMO_MESES) {
            throw new EntradaInvalidaException(
                    "O prazo em meses deve estar entre 1 e "
                            + SimulacaoRequestDTO.PRAZO_MAXIMO_MESES + ".");
        }

        ProdutoEntity produto = produtoRepositoryImpl.findByName(nomeProduto)
                .orElseThrow(() -> new EntradaInvalidaException("Produto não encontrado."));

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
                .orElseThrow(() -> new EntradaInvalidaException("Produto não encontrado.")
        ));
    }

    private BigDecimal converterTaxaAnualParaMensal(BigDecimal taxaAnual) {
        if (taxaAnual == null || taxaAnual.compareTo(BigDecimal.ONE.negate()) < 0) {
            throw new IllegalStateException("A rentabilidade anual do produto não permite capitalização composta.");
        }
        double taxaMensal = Math.pow(1.0 + taxaAnual.doubleValue(), 1.0 / 12.0) - 1.0;
        if (!Double.isFinite(taxaMensal)) {
            throw new IllegalStateException("A rentabilidade anual do produto excede a capacidade do cálculo.");
        }
        return BigDecimal.valueOf(taxaMensal)
                .setScale(10, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularJurosCompostos(BigDecimal valor, BigDecimal taxaMensal, int meses) {
        BigDecimal valorFinal;
        try {
            valorFinal = valor.multiply(
                    BigDecimal.ONE.add(taxaMensal).pow(meses, MathContext.DECIMAL128),
                    MathContext.DECIMAL128);
        } catch (ArithmeticException exception) {
            throw new EntradaInvalidaException(
                    "O valor e o prazo informados excedem a capacidade de cálculo da simulação.", exception);
        }
        if (valorFinal.compareTo(VALOR_MAXIMO) > 0) {
            throw new EntradaInvalidaException(
                    "O valor final excede o limite de armazenamento de 9999999999999999.99.");
        }
        return valorFinal.setScale(2, RoundingMode.HALF_UP);
    }

    @CacheResult(cacheName = "simulacoes-por-produto-dia")
    public List<SimulacaoAgrupadaResponseDTO> agregacaoPorProdutoDia() {
        return simulacaoRepository.agruparPorProdutoDia();
    }
}
