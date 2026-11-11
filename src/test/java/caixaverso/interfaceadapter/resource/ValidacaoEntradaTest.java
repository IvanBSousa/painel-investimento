package caixaverso.interfaceadapter.resource;

import caixaverso.application.dto.LoginRequestDTO;
import caixaverso.application.dto.SimulacaoRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ValidacaoEntradaTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void deveRejeitarCamposObrigatoriosAusentes() {
        var violations = validator.validate(new SimulacaoRequestDTO(null, null, null, null));
        Set<String> fields = violations.stream()
                .map(violation -> violation.getPropertyPath().toString()).collect(Collectors.toSet());

        assertEquals(Set.of("valorInvestido", "prazoMeses", "produto"), fields);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01", "0.001", "10000000000000000.00", "1E+16"})
    void deveRejeitarValorInvalido(String valor) {
        assertFalse(validator.validate(
                new SimulacaoRequestDTO(1L, new BigDecimal(valor), 1, "CDB")).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "9999999999999999.99", "1E+3"})
    void deveAceitarLimitesMonetariosEClienteOpcional(String valor) {
        assertTrue(validator.validate(
                new SimulacaoRequestDTO(null, new BigDecimal(valor), 1, "CDB")).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void deveRejeitarPrazoNaoPositivo(int prazo) {
        assertFalse(validator.validate(
                new SimulacaoRequestDTO(1L, BigDecimal.ONE, prazo, "CDB")).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void deveRejeitarClienteNaoPositivo(long clienteId) {
        assertFalse(validator.validate(
                new SimulacaoRequestDTO(clienteId, BigDecimal.ONE, 1, "CDB")).isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void deveRejeitarProdutoOuCredenciaisEmBranco(String texto) {
        assertFalse(validator.validate(
                new SimulacaoRequestDTO(1L, BigDecimal.ONE, 1, texto)).isEmpty());
        assertFalse(validator.validate(new LoginRequestDTO(texto, "senha")).isEmpty());
        assertFalse(validator.validate(new LoginRequestDTO("user", texto)).isEmpty());
    }

    @Test
    void deveRespeitarTamanhoDaColunaProduto() {
        assertTrue(validator.validate(
                new SimulacaoRequestDTO(1L, BigDecimal.ONE, 1, "a".repeat(255))).isEmpty());
        assertFalse(validator.validate(
                new SimulacaoRequestDTO(1L, BigDecimal.ONE, 1, "a".repeat(256))).isEmpty());
    }

    @Test
    void deveValidarCorpoNuloECamposNoResource() throws Exception {
        var resource = new SimulacaoResource(null);
        var method = SimulacaoResource.class.getMethod("simular", SimulacaoRequestDTO.class);
        assertEquals(1, validator.forExecutables()
                .validateParameters(resource, method, new Object[]{null}).size());
        assertEquals(3, validator.forExecutables().validateParameters(resource, method,
                new Object[]{new SimulacaoRequestDTO(null, null, null, null)}).size());

        var login = new AutenticacaoResource(null);
        var loginMethod = AutenticacaoResource.class.getMethod("login", LoginRequestDTO.class);
        assertEquals(1, validator.forExecutables()
                .validateParameters(login, loginMethod, new Object[]{null}).size());
        assertEquals(2, validator.forExecutables().validateParameters(login, loginMethod,
                new Object[]{new LoginRequestDTO("", "")}).size());
    }

    @Test
    void deveValidarPaginacaoEIdsNosResources() throws Exception {
        var resource = new SimulacaoResource(null);
        var method = SimulacaoResource.class.getMethod("listarTodos", int.class, int.class);
        assertEquals(2, validator.forExecutables()
                .validateParameters(resource, method, new Object[]{-1, 0}).size());
        assertTrue(validator.forExecutables()
                .validateParameters(resource, method, new Object[]{0, 20}).isEmpty());

        var investimentos = new InvestimentosResource(null);
        assertEquals(1, validator.forExecutables().validateParameters(investimentos,
                InvestimentosResource.class.getMethod("listarPorCliente", Long.class), new Object[]{0L}).size());
        var perfil = new PerfilRiscoResource(null);
        assertEquals(1, validator.forExecutables().validateParameters(perfil,
                PerfilRiscoResource.class.getMethod("obter", Long.class), new Object[]{-1L}).size());
    }
}
