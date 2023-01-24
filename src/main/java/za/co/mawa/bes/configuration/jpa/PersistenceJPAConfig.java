package za.co.mawa.bes.configuration.jpa;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EntityScan("za.co.mawa.bes")
@EnableJpaRepositories("za.co.mawa.bes")
@ComponentScan(basePackages = "za.co.mawa.bes")
public class PersistenceJPAConfig {

}