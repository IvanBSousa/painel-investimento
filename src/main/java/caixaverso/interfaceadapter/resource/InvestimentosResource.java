package caixaverso.interfaceadapter.resource;

import caixaverso.application.usecase.InvestimentoUseCase;
import caixaverso.application.dto.InvestimentoDTO;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/investimentos")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class InvestimentosResource {

    private final InvestimentoUseCase investimentoUseCase;

    public InvestimentosResource(InvestimentoUseCase investimentoUseCase) {
        this.investimentoUseCase = investimentoUseCase;
    }

    @GET
    @Path("/{clienteId}")
    @RolesAllowed("Admin")
    public Response listarPorCliente(@PathParam("clienteId") @Positive(message = "O clienteId deve ser positivo.") Long clienteId) {
        List<InvestimentoDTO> investimento = investimentoUseCase.listarPorCliente(clienteId);
        if (investimento.isEmpty()) {
            return Response.status(Response.Status.OK)
                    .entity("""
                    {
                      "mensagem": "O cliente não possui investimentos."
                    }
                    """).build();
        }
        return Response.ok(investimento).build();
    }
}
