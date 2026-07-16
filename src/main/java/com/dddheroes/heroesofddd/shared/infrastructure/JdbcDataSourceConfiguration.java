package com.dddheroes.heroesofddd.shared.infrastructure;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit JDBC DataSource for the blocking side of the stack: the Axon JPA event store, token
 * store, dead-letter queue, and Flyway. Spring Boot's DataSourceAutoConfiguration backs off
 * entirely when an R2DBC ConnectionFactory is present
 * ({@code @ConditionalOnMissingBean(type = "io.r2dbc.spi.ConnectionFactory")}), so with the
 * reactive read models on R2DBC this bean must be defined manually.
 * <p>
 * A {@link JdbcConnectionDetails} bean (e.g. registered by a Testcontainers
 * {@code @ServiceConnection}) takes precedence over the {@code spring.datasource.*} properties,
 * mirroring the auto-configuration's behavior.
 */
@Configuration
class JdbcDataSourceConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource")
    DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    HikariDataSource dataSource(DataSourceProperties properties,
                                ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
        var connectionDetails = connectionDetailsProvider.getIfAvailable();
        if (connectionDetails != null) {
            properties.setUrl(connectionDetails.getJdbcUrl());
            properties.setUsername(connectionDetails.getUsername());
            properties.setPassword(connectionDetails.getPassword());
            properties.setDriverClassName(connectionDetails.getDriverClassName());
        }
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }
}
