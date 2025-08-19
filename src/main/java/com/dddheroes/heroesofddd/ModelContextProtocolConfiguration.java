package com.dddheroes.heroesofddd;

import com.dddheroes.heroesofddd.shared.mcp.configuration.HealthCheckMcpTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelContextProtocolConfiguration {

    @Bean
    ToolCallbackProvider tools(HealthCheckMcpTool healthCheckMcpTool) {
        return MethodToolCallbackProvider.builder().toolObjects(healthCheckMcpTool).build();
    }
}
