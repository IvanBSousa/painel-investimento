package caixaverso.interfaceadapter.resource;

import caixaverso.application.usecase.PerfilRiscoUseCase;
import caixaverso.application.dto.PerfilRiscoResponseDTO;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/perfil-risco")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class PerfilRiscoResource {

    private final PerfilRiscoUseCase perfilRiscoUseCase;

    public PerfilRiscoResource(PerfilRiscoUseCase perfilRiscoUseCase) {
        this.perfilRiscoUseCase = perfilRiscoUseCase;
    }

    @GET
    @Path("/{clienteId}")
    @RolesAllowed("Admin")
    public Response obter(@PathParam("clienteId") @Positive(message = "O clienteId deve ser positivo.") Long clienteId) {
        PerfilRiscoResponseDTO perfilRisco = perfilRiscoUseCase.calcularPerfil(clienteId);
        return Response.ok(perfilRisco).build();
    }
}
