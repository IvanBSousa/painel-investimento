package caixaverso.interfaceadapter.exception;

import caixaverso.application.exception.EntradaInvalidaException;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntradaInvalidaExceptionMapperTest {

    @Test
    void deveRetornarErro400ComMensagemJson() {
        var mapper = new EntradaInvalidaExceptionMapper();
        try (var response = mapper.toResponse(new EntradaInvalidaException("Produto não encontrado."))) {
            assertEquals(400, response.getStatus());
            assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
            assertEquals(Map.of("mensagem", "Produto não encontrado."), response.getEntity());
        }
    }
}
