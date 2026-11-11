package caixaverso.application.usecase;

import caixaverso.application.dto.*;
import caixaverso.application.exception.EntradaInvalidaException;
import caixaverso.infrastructure.mapper.SimulacaoHistoricoMapper;
import caixaverso.infrastructure.mapper.SimulacaoResponseMapper;
import caixaverso.infrastructure.persistence.entity.ProdutoEntity;
import caixaverso.infrastructure.persistence.entity.SimulacaoEntity;
import caixaverso.infrastructure.persistence.repository.ProdutoRepositoryImpl;
import caixaverso.infrastructure.persistence.repository.SimulacaoRepositoryImpl;

import caixaverso.infrastructure.persistence.repository.TelemetriaRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimulacaoUseCaseTest {

    @Mock
    private ProdutoRepositoryImpl produtoRepository;

    @Mock
    private SimulacaoRepositoryImpl simulacaoRepository;

    @Mock
    private SimulacaoHistoricoMapper historicoMapper;

    @Mock
    private SimulacaoResponseMapper responseMapper;

    @Mock
    private TelemetriaRepositoryImpl telemetriaRepository;

    @InjectMocks
    private SimulacaoUseCase useCase;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deveListarTodos() {
        SimulacaoEntity e1 = new SimulacaoEntity();
        SimulacaoEntity e2 = new SimulacaoEntity();

        when(simulacaoRepository.listarPaginado(0, 10))
                .thenReturn(List.of(e1, e2));

        when(historicoMapper.toDTO(any()))
                .thenReturn(mock(SimulacaoHistoricoDTO.class));

        var result = useCase.listarTodos(0, 10);

        assertEquals(2, result.size());
        verify(simulacaoRepository, times(1)).listarPaginado(0, 10);
        verify(historicoMapper, times(2)).toDTO(any());
    }

    @Test
    void deveSimularInternoComSucesso() {
        ProdutoEntity produto = new ProdutoEntity();
        produto.setNome("CDB Premium");
        produto.setRentabilidade(new BigDecimal("0.12")); // 12% aa

        when(produtoRepository.findByName("CDB Premium"))
                .thenReturn(Optional.of(produto));

        SimulacaoEntity salvo = new SimulacaoEntity();
        doNothing().when(simulacaoRepository).salvar(any());

        SimulacaoEntity result = useCase.simularInterno(
                1L,
                new BigDecimal("1000"),
                12,
                "CDB Premium"
        );

        assertNotNull(result);
        assertEquals("CDB Premium", result.getProduto());
        assertEquals(new BigDecimal("1000"), result.getValorInvestido());
        assertEquals(12, result.getPrazoMeses());
        verify(simulacaoRepository, times(1)).salvar(any());
    }

    @Test
    void deveFalharSimularInternoQuandoProdutoNaoExiste() {
        when(produtoRepository.findByName("XPTO"))
                .thenReturn(Optional.empty());

        assertThrows(EntradaInvalidaException.class, () ->
                useCase.simularInterno(1L, BigDecimal.TEN, 10, "XPTO")
        );
        verifyNoInteractions(simulacaoRepository, telemetriaRepository);
    }

    @Test
    void deveSimularComSucesso() {
        SimulacaoRequestDTO req =
                new SimulacaoRequestDTO(1L, new BigDecimal("1000"), 12, "CDB Caixa 2026");

        ProdutoEntity produto = new ProdutoEntity();
        produto.setNome("CDB Caixa 2026");
        produto.setTipo("CDB");
        produto.setRentabilidade(new BigDecimal("0.10"));
        produto.setRisco("Baixo");

        when(produtoRepository.findByName("CDB Caixa 2026"))
                .thenReturn(Optional.of(produto));

        doNothing().when(simulacaoRepository).salvar(any(SimulacaoEntity.class));

        SimulacaoProdutoValidadoDTO produtoValidado =
                new SimulacaoProdutoValidadoDTO(1,"CDB Caixa 2026", "CDB", new BigDecimal("0.10"),
                        "Baixo");

        ResultadoSimulacaoDTO resultado =
                new ResultadoSimulacaoDTO(new BigDecimal("1000"), new BigDecimal("0.10"), 10);

        Instant agora = Instant.now();

        when(responseMapper.toResponse(any(SimulacaoEntity.class), eq(produto)))
                .thenReturn(new SimulacaoResponseDTO(produtoValidado, resultado, agora));

        SimulacaoResponseDTO result = useCase.simular(req);

        assertNotNull(result);
        assertEquals("CDB", result.produtoValidado().tipo());

        verify(responseMapper).toResponse(any(SimulacaoEntity.class), eq(produto));
    }

    @Test
    void deveRetornarAgregacaoPorProdutoDia() {
        when(simulacaoRepository.agruparPorProdutoDia())
                .thenReturn(List.of(mock(SimulacaoAgrupadaResponseDTO.class)));

        var result = useCase.agregacaoPorProdutoDia();

        assertEquals(1, result.size());
        verify(simulacaoRepository, times(1)).agruparPorProdutoDia();
    }

    @Test
    void deveConverterTaxaAnualParaMensal() throws Exception {
        Method m = SimulacaoUseCase.class.getDeclaredMethod(
                "converterTaxaAnualParaMensal", BigDecimal.class);
        m.setAccessible(true);

        BigDecimal taxa = (BigDecimal) m.invoke(useCase, new BigDecimal("0.12"));
        assertNotNull(taxa);
        assertTrue(taxa.doubleValue() > 0);
    }

    @Test
    void deveCalcularJurosCompostos() throws Exception {
        Method m = SimulacaoUseCase.class.getDeclaredMethod(
                "calcularJurosCompostos", BigDecimal.class, BigDecimal.class, int.class);
        m.setAccessible(true);

        BigDecimal valor = new BigDecimal("1000");
        BigDecimal taxa = new BigDecimal("0.01");
        int meses = 12;

        BigDecimal resultado = (BigDecimal) m.invoke(useCase, valor, taxa, meses);

        assertEquals(new BigDecimal("1126.83"), resultado);
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,-1", "2147483647,2"})
    void deveRejeitarPaginacaoInvalidaAntesDaConsulta(int page, int size) {
        assertThrows(EntradaInvalidaException.class, () -> useCase.listarTodos(page, size));
        verifyNoInteractions(simulacaoRepository);
    }

    @Test
    void deveAceitarLimiteTecnicoDoOffset() {
        when(simulacaoRepository.listarPaginado(Integer.MAX_VALUE, 1)).thenReturn(List.of());

        assertTrue(useCase.listarTodos(Integer.MAX_VALUE, 1).isEmpty());
        verify(simulacaoRepository).listarPaginado(Integer.MAX_VALUE, 1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 601, Integer.MAX_VALUE})
    void deveRejeitarPrazoForaDoLimiteAntesDeConsultarProduto(int prazo) {
        assertThrows(EntradaInvalidaException.class,
                () -> useCase.simularInterno(1L, BigDecimal.ONE, prazo, "CDB"));

        verifyNoInteractions(produtoRepository, simulacaoRepository, telemetriaRepository);
    }

    @Test
    void deveAceitarPrazoMaximoNaSimulacao() {
        ProdutoEntity produto = new ProdutoEntity();
        produto.setNome("CDB");
        produto.setRentabilidade(new BigDecimal("0.12"));
        when(produtoRepository.findByName("CDB")).thenReturn(Optional.of(produto));

        SimulacaoEntity result = useCase.simularInterno(
                1L, new BigDecimal("1000.00"), SimulacaoRequestDTO.PRAZO_MAXIMO_MESES, "CDB");

        assertNotNull(result);
        assertEquals(SimulacaoRequestDTO.PRAZO_MAXIMO_MESES, result.getPrazoMeses());
        verify(simulacaoRepository).salvar(any(SimulacaoEntity.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1.01", "1E+10000"})
    void deveFalharSemPersistirQuandoRentabilidadeDoProdutoForInvalida(String taxaAnual) {
        ProdutoEntity produto = new ProdutoEntity();
        produto.setNome("CDB");
        produto.setRentabilidade(new BigDecimal(taxaAnual));
        when(produtoRepository.findByName("CDB")).thenReturn(Optional.of(produto));

        assertThrows(IllegalStateException.class,
                () -> useCase.simularInterno(1L, BigDecimal.ONE, 12, "CDB"));

        verifyNoInteractions(simulacaoRepository, telemetriaRepository);
    }

    @ParameterizedTest
    @CsvSource({"9999999999999999.99,0,9999999999999999.99", "1.00,0.005,1.01"})
    void devePreservarCentavosEArredondarComHalfUp(String valor, String taxa, String esperado) throws Exception {
        Method method = SimulacaoUseCase.class.getDeclaredMethod(
                "calcularJurosCompostos", BigDecimal.class, BigDecimal.class, int.class);
        method.setAccessible(true);

        assertEquals(new BigDecimal(esperado),
                method.invoke(useCase, new BigDecimal(valor), new BigDecimal(taxa), 1));
    }

    @ParameterizedTest
    @CsvSource({"9999999999999999.99,12", "1000.00,100000000", "1000.00,2147483647"})
    void deveRejeitarResultadoOuPrazoForaDaCapacidadeSemPersistir(String valor, int prazo) {
        ProdutoEntity produto = new ProdutoEntity();
        produto.setNome("CDB");
        produto.setRentabilidade(new BigDecimal("0.12"));
        when(produtoRepository.findByName("CDB")).thenReturn(Optional.of(produto));

        assertThrows(EntradaInvalidaException.class,
                () -> useCase.simularInterno(1L, new BigDecimal(valor), prazo, "CDB"));

        verifyNoInteractions(simulacaoRepository, telemetriaRepository);
    }
}
