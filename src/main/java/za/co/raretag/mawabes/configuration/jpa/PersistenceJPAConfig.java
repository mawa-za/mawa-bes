package za.co.raretag.mawabes.configuration.jpa;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EntityScan("za.co.raretag.mawabes.entity")
@EnableJpaRepositories("za.co.raretag.mawabes.repository")
public class PersistenceJPAConfig {

}