package caixaverso.interfaceadapter.resource;

import caixaverso.application.dto.LoginRequestDTO;
import caixaverso.application.usecase.UsuarioUseCase;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
public class AutenticacaoResource {

    private final UsuarioUseCase usuarioUseCase;

    public AutenticacaoResource(UsuarioUseCase usuarioUseCase) {
        this.usuarioUseCase = usuarioUseCase;
    }

    @POST
    @Path("/login")
    @Produces(MediaType.TEXT_PLAIN)
    public Response login(@NotNull(message = "O corpo da requisição é obrigatório.") @Valid LoginRequestDTO loginRequest) {
        return usuarioUseCase.autenticaEGeraToken(loginRequest.usuario(), loginRequest.senha())
                .map(token -> Response.ok(token).build())
                .orElseGet(() -> Response.status(Response.Status.UNAUTHORIZED)
                        .entity("""
                        {
                          "mensagem": "Credenciais inválidas.",
                          "detalhes": "Usuário e/ou senha incorretos."
                        }
                        """).build());
    }
}
