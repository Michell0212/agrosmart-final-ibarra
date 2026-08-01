package ec.edu.espe.agrosmart.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;

class PublicidadServiceTest {

    @Test
    void generarPublicidad_caminoFeliz_debeEmitirElTextoDelModelo() {
        AgroSmartAIService ia = Mockito.mock(AgroSmartAIService.class);
        Mockito.when(ia.generarPublicidad(any(), any()))
                .thenReturn("Café de altura para paladares exigentes");
        PublicidadService service = new PublicidadService(ia);

        StepVerifier.create(service.generarPublicidad("Café", "cafeterías"))
                .expectNext("Café de altura para paladares exigentes")
                .verifyComplete();
    }

    @Test
    void generarPublicidad_cuandoElProveedorFalla_debeEmitirMensajeDeRespaldo() {
        AgroSmartAIService ia = Mockito.mock(AgroSmartAIService.class);
        Mockito.when(ia.generarPublicidad(any(), any()))
                .thenThrow(new RuntimeException("429 Too Many Requests"));
        PublicidadService service = new PublicidadService(ia);

        StepVerifier.create(service.generarPublicidad("Café", "cafeterías"))
                .expectNextMatches(texto -> texto.contains("no disponible"))
                .verifyComplete();
    }
}