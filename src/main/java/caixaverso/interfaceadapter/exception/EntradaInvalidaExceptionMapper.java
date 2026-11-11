package caixaverso.interfaceadapter.exception;

import caixaverso.application.exception.EntradaInvalidaException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class EntradaInvalidaExceptionMapper implements ExceptionMapper<EntradaInvalidaException> {

    @Override
    public Response toResponse(EntradaInvalidaException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("mensagem", exception.getMessage()))
                .build();
    }
}
