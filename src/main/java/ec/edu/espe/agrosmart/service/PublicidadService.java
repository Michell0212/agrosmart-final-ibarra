package ec.edu.espe.agrosmart.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Service
public class PublicidadService {

    private final AgroSmartAIService aiService;

    public PublicidadService(AgroSmartAIService aiService) {
        this.aiService = aiService;
    }

    public Mono<String> generarPublicidad(String producto, String audiencia) {
        return Mono.fromCallable(() -> aiService.generarPublicidad(producto, audiencia))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(e -> Mono.just(
                        "Publicidad no disponible en este momento (" + e.getClass().getSimpleName() + ")"));
    }
}