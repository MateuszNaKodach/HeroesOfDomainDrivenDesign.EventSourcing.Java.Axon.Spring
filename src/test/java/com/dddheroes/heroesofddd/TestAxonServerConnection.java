package com.dddheroes.heroesofddd;

import io.axoniq.axonserver.connector.AxonServerConnection;
import org.axonframework.axonserver.connector.AxonServerConfiguration;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.axonframework.springboot.service.connection.AxonServerConnectionDetails;
import org.axonframework.test.server.AxonServerContainer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TestAxonServerConnection {

    @Autowired
    private AxonServerContainer axonServer;

    @Autowired
    private AxonServerConfiguration axonServerConfiguration;

    @Autowired
    private AxonServerConnectionDetails connectionDetails;

    @Autowired
    private AxonServerConnectionManager axonServerConnectionManager;

    @Test
    void verifyApplicationStartsNormallyWithAxonServerInstance() {
        assertTrue(axonServer.isRunning());
        assertNotNull(connectionDetails);
        assertTrue(connectionDetails.routingServers().endsWith("" + axonServer.getGrpcPort()));
        assertNotNull(axonServerConfiguration);

        assertNotEquals("localhost:8024", axonServerConfiguration.getServers());

        AxonServerConnection connection = axonServerConnectionManager.getConnection();

        await().atMost(Duration.ofSeconds(5))
               .untilAsserted(() -> assertTrue(connection.isConnected()));
    }
}
