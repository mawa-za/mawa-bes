package za.co.mawa.bes.configuration.jpa;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.MySQLDialect;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import za.co.mawa.bes.configuration.hibernate.TenantConnectionProvider;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EntityScan("za.co.mawa.bes")
@EnableJpaRepositories("za.co.mawa.bes")
@ComponentScan(basePackages = "za.co.mawa.bes")
public class PersistenceJPAConfig {
//    @Bean
//    public LocalContainerEntityManagerFactoryBean entityManagerFactory(JpaVendorAdapter jpaVendorAdapter,
//                                                                       DataSource dataSource,
//                                                                       TenantConnectionProvider multiTenantConnectionProvider,
//                                                                       CurrentTenantIdentifierResolver tenantIdentifierResolver) {
//        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
//        em.setDataSource(dataSource);
//        em.setJpaVendorAdapter(jpaVendorAdapter);
//
//        Map<String, Object> jpaProperties = new HashMap<>();
//        jpaProperties.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
//        jpaProperties.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
//        jpaProperties.put(Environment.FORMAT_SQL, true);
//
//        jpaProperties.put(Environment.PHYSICAL_NAMING_STRATEGY, PhysicalNamingStrategyStandardImpl.class);
//        jpaProperties.put(Environment.DIALECT, MySQLDialect.class);
//
//        em.setJpaPropertyMap(jpaProperties);
//        return em;
//    }
}