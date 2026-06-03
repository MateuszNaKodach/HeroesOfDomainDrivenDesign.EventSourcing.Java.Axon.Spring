package com.dddheroes.heroesofddd;

import io.axoniq.framework.testcontainer.AxonServerContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:latest");
    }

    // Aggregate-based event store mode uses the classic (non-DCB) Axon Server context.
    @Profile("axonserver-aggregate")
    @Bean
    @ServiceConnection
    AxonServerContainer axonServerAggregateContainer() {
        return new AxonServerContainer("axoniq/axonserver:latest").withDevMode(true);
    }

    // DCB event store mode requires the Axon Server context to be DCB-enabled.
    @Profile("axonserver-dcb")
    @Bean
    @ServiceConnection
    AxonServerContainer axonServerDcbContainer() {
        return new AxonServerContainer("axoniq/axonserver:latest").withDevMode(true).withDcbContext(true);
    }
}
