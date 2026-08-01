package ec.edu.espe.agrosmart.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProductoTest {

    @Test
    void getters_conValoresDelConstructor_debenDevolverLoRecibido() {
        List<String> correos = List.of("ventas@cafeteria.ec");
        Producto producto = new Producto(1L, "Café Arábica", "Café",
                new BigDecimal("12.50"), correos);

        assertEquals(1L, producto.getId());
        assertEquals("Café Arábica", producto.getNombre());
        assertEquals("Café", producto.getCategoria());
        assertEquals(new BigDecimal("12.50"), producto.getPrecioUsd());
        assertEquals(1, producto.getCorreosNotificacion().size());
    }

    @Test
    void getCorreosNotificacion_alMutarLaListaOriginal_noDebeAfectarAlProducto() {
        List<String> correos = new ArrayList<>();
        correos.add("ventas@cafeteria.ec");
        Producto producto = new Producto(1L, "Café Arábica", "Café",
                new BigDecimal("12.50"), correos);

        correos.add("intruso@mail.com");

        assertEquals(1, producto.getCorreosNotificacion().size());
        assertNotSame(correos, producto.getCorreosNotificacion());
    }

    @Test
    void getCorreosNotificacion_debeDevolverListaInmodificable() {
        Producto producto = new Producto(1L, "Café Arábica", "Café",
                new BigDecimal("12.50"), List.of("ventas@cafeteria.ec"));

        List<String> correos = producto.getCorreosNotificacion();

        assertThrows(UnsupportedOperationException.class,
                () -> correos.add("nuevo@mail.com"));
    }
}