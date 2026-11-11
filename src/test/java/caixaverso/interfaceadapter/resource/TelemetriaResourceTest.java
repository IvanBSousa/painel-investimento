package caixaverso.interfaceadapter.resource;

import caixaverso.infrastructure.persistence.repository.TelemetriaRepositoryImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TelemetriaResourceTest {

    @Test
    void deveRejeitarPeriodoInvertidoAntesDeConsultar() {
        var repository = mock(TelemetriaRepositoryImpl.class);
        var resource = new TelemetriaResource(repository);
        LocalDate inicio = LocalDate.of(2026, 1, 2);

        try (var response = resource.obterTelemetria(inicio, inicio.minusDays(1))) {
            assertEquals(400, response.getStatus());
        }
        verifyNoInteractions(repository);
    }
}
