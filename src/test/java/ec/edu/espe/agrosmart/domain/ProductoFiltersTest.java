package ec.edu.espe.agrosmart.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProductoFiltersTest {

    @Test
    void isValid_conPrecioPositivoYCorreos_debeSerValido() {
        Producto producto = new Producto(1L, "Café Arábica", "Café",
                new BigDecimal("12.50"), List.of("ventas@cafeteria.ec"));

        assertTrue(ProductoFilters.IS_VALID.test(producto));
    }

    @Test
    void isValid_conPrecioCero_debeSerInvalido() {
        Producto producto = new Producto(1L, "Café Sin Precio", "Café",
                BigDecimal.ZERO, List.of("ventas@cafeteria.ec"));

        assertFalse(ProductoFilters.IS_VALID.test(producto));
    }

    @Test
    void isValid_conCorreosVacios_debeSerInvalido() {
        Producto producto = new Producto(1L, "Café Sin Correos", "Café",
                new BigDecimal("15.00"), List.of());

        assertFalse(ProductoFilters.IS_VALID.test(producto));
    }
}