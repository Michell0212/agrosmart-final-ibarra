package ec.edu.espe.agrosmart.service;

import ec.edu.espe.agrosmart.domain.Producto;
import ec.edu.espe.agrosmart.entity.ProductoEntity;
import ec.edu.espe.agrosmart.exception.ProductoNoEncontradoException;
import ec.edu.espe.agrosmart.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

class ProductoServiceTest {

    private List<ProductoEntity> datosDePrueba() {
        return List.of(
                new ProductoEntity("Café Arábica", new BigDecimal("12.50"), 100, "Café",
                        "compras@cafeteria.ec"),
                new ProductoEntity("Café Geisha", new BigDecimal("28.00"), 50, "Café",
                        "ventas@especialidad.ec"),
                new ProductoEntity("Café Honey", new BigDecimal("18.75"), 80, "Café",
                        "pedidos@cafeteria.ec"),
                new ProductoEntity("Café Sin Precio", new BigDecimal("0.00"), 40, "Café",
                        "info@cafeteria.ec"),
                new ProductoEntity("Café Sin Correos", new BigDecimal("15.00"), 60, "Café",
                        "")
        );
    }

    @Test
    void obtenerProductosComercializables_conTresValidosYDosInvalidos_debeEmitirSoloLosValidos() {
        ProductoRepository repo = Mockito.mock(ProductoRepository.class);
        Mockito.when(repo.findAll()).thenReturn(datosDePrueba());
        ProductoService service = new ProductoService(repo);

        Flux<Producto> flujo = service.obtenerProductosComercializables();

        StepVerifier.create(flujo)
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void obtenerProductosComercializables_cuandoTodosInvalidos_debeEmitirElDefault() {
        ProductoRepository repo = Mockito.mock(ProductoRepository.class);
        Mockito.when(repo.findAll()).thenReturn(List.of(
                new ProductoEntity("Café Malo", BigDecimal.ZERO, 10, "Café", "")
        ));
        ProductoService service = new ProductoService(repo);

        StepVerifier.create(service.obtenerProductosComercializables())
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void buscarPorId_conIdInexistente_debeEmitirError() {
        ProductoRepository repo = Mockito.mock(ProductoRepository.class);
        Mockito.when(repo.findById(9999L)).thenReturn(Optional.empty());
        ProductoService service = new ProductoService(repo);

        StepVerifier.create(service.buscarPorId(9999L))
                .expectError(ProductoNoEncontradoException.class)
                .verify();
    }
}