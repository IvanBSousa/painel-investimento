package caixaverso.interfaceadapter.resource;

import caixaverso.application.dto.TelemetriaItemDTO;
import caixaverso.infrastructure.persistence.repository.TelemetriaRepositoryImpl;
import io.quarkus.security.Authenticated;
import io.vertx.core.cli.annotations.Description;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import caixaverso.application.dto.TelemetriaPeriodoDTO;
import caixaverso.application.dto.TelemetriaResponseDTO;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Path("/telemetria")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class TelemetriaResource {

    private final TelemetriaRepositoryImpl telemetriaRepositoryImpl;

    public TelemetriaResource(TelemetriaRepositoryImpl telemetriaRepositoryImpl) {
        this.telemetriaRepositoryImpl = telemetriaRepositoryImpl;
    }

    @GET
    @RolesAllowed("Admin")
    public Response obterTelemetria(
            @Parameter(name= " Início", description = "Caso não seja informada, pega o primeiro dia do mês atual.")
            @QueryParam("comeco") LocalDate comeco,
            @Parameter(name= "Fim", description = "Caso não seja informada, pega o último dia do mês atual.")
            @QueryParam("fim") LocalDate fim) {
        LocalDate hoje = LocalDate.now();
        if (comeco == null) {
            comeco = hoje.with(TemporalAdjusters.firstDayOfMonth());
        }
        if (fim == null) {
            fim = hoje.with(TemporalAdjusters.lastDayOfMonth());
        }

        TelemetriaPeriodoDTO periodo = new TelemetriaPeriodoDTO(comeco, fim);

        if (fim .isBefore(comeco)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("""
                    {
                      "mensagem": "O parâmetro 'fim' deve ser maior ou igual ao parâmetro 'comeco'."
                    }
                    """).build();
        }

        List<TelemetriaItemDTO> itens = telemetriaRepositoryImpl.agrupaPorServicoEPeriodo(comeco, fim);

        if (itens.isEmpty()) {
            return Response.status(Response.Status.OK)
                    .entity("""
                    {
                      "mensagem": "Nenhum dado de telemetria encontrado."
                    }
                    """).build();
        }

        TelemetriaResponseDTO response = new TelemetriaResponseDTO(itens, periodo);

        return Response.ok(response).build();
    }
}