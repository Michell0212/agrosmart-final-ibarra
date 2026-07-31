package ec.edu.espe.agrosmart.service;

import ec.edu.espe.agrosmart.domain.Producto;
import ec.edu.espe.agrosmart.domain.ProductoFilters;
import ec.edu.espe.agrosmart.exception.ProductoNoEncontradoException;
import ec.edu.espe.agrosmart.mapper.ProductoMapper;
import ec.edu.espe.agrosmart.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    private static final Producto PRODUCTO_GENERICO =
            new Producto(0L, "SIN PRODUCTOS COMERCIALIZABLES", "N/A",
                    java.math.BigDecimal.ZERO, java.util.List.of());

    public Flux<Producto> obtenerProductosComercializables() {
        // fromCallable difiere la consulta bloqueante: nada se ejecuta hasta que haya suscriptor.
        return Mono.fromCallable(productoRepository::findAll)
                // boundedElastic: JPA/Hibernate bloquea el hilo. En el event loop de Netty,
                // un solo hilo bloqueado degradaría todas las peticiones concurrentes.
                .subscribeOn(Schedulers.boundedElastic())
                // flatMapMany: convierte la List materializada en un Flux que emite uno a uno.
                .flatMapMany(Flux::fromIterable)
                // map a dominio: pasa de la entidad JPA al modelo inmutable.
                .map(ProductoMapper::toDominio)
                // A_MAYUSCULAS: normaliza el nombre a mayúsculas devolviendo una instancia nueva.
                .map(ProductoFilters.A_MAYUSCULAS)
                // filter: descarta los no comercializables (precio <= 0 o sin correos).
                .filter(ProductoFilters.IS_VALID)
                // doOnNext: trazabilidad por cada emisión, sin transformar el flujo.
                .doOnNext(ProductoFilters.LOG_PRODUCTO)
                // defaultIfEmpty: si el filtro dejó el flujo vacío, emite un producto genérico.
                .defaultIfEmpty(PRODUCTO_GENERICO);
    }

    public Mono<Producto> buscarPorId(Long id) {
        // fromCallable + boundedElastic: la búsqueda bloqueante también se aísla del event loop.
        return Mono.fromCallable(() -> productoRepository.findById(id))
                .subscribeOn(Schedulers.boundedElastic())
                // justOrEmpty: Optional vacío → Mono vacío, sin sacar el valor del contexto reactivo.
                .flatMap(Mono::justOrEmpty)
                .map(ProductoMapper::toDominio)
                // switchIfEmpty: el "no encontrado" se resuelve DENTRO del flujo lanzando la excepción,
                // en vez de devolver un valor por defecto (esa es la diferencia con defaultIfEmpty).
                .switchIfEmpty(Mono.error(new ProductoNoEncontradoException(id)));
    }
}