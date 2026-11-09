package romsanbryan.aquadesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@ComponentScan(basePackages = {
		"romsanbryan.aquadesk",
		"romsanbryan.parameters",
		"romsanbryan.notifications"
})
@EntityScan(basePackages = {
		"romsanbryan.data.entities"
})
@EnableJpaRepositories(basePackages = {
		"romsanbryan.data.repositories"
})
@EnableScheduling
public class AquadeskApplication {
	private static final Logger log = LoggerFactory.getLogger(AquadeskApplication.class);

	public static void main(String[] args) {
		log.info("Starting Aquadesk application");
		SpringApplication.run(AquadeskApplication.class, args);
	}
}