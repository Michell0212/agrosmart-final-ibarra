package ec.edu.espe.agrosmart;

import ec.edu.espe.agrosmart.entity.ProductoEntity;
import ec.edu.espe.agrosmart.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.math.BigDecimal;
import java.util.List;

@SpringBootApplication
public class AgrosmartApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgrosmartApplication.class, args);
	}

	@Bean
	CommandLineRunner sembrarCafes(ProductoRepository repository) {
		return args -> {
			if (repository.count() == 0) {
				repository.saveAll(List.of(
						new ProductoEntity("Café Arábica Lavado Alta Montaña",
								new BigDecimal("12.50"), 100, "Café",
								"compras@cafeteria.ec,barista@cafeteria.ec"),
						new ProductoEntity("Café Geisha Natural Microlote",
								new BigDecimal("28.00"), 50, "Café",
								"ventas@especialidad.ec"),
						new ProductoEntity("Café Honey Process Origen Único",
								new BigDecimal("18.75"), 80, "Café",
								"pedidos@cafeteria.ec"),
						new ProductoEntity("Café Sin Precio Asignado",
								new BigDecimal("0.00"), 40, "Café",
								"info@cafeteria.ec"),
						new ProductoEntity("Café Sin Correos de Notificación",
								new BigDecimal("15.00"), 60, "Café",
								"")
				));
			}
		};
	}
}