package com.dddheroes.heroesofddd.shared.mcp.configuration;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class HealthCheckMcpTool {

    @Tool(name = "health_check", description = "Verifies that the MCP server is running and operational")
    public Map<String, Object> healthCheck() {
        return Map.of(
            "status", "healthy",
            "server", "Heroes of DDD MCP Server",
            "timestamp", LocalDateTime.now().toString(),
            "message", "MCP server is running successfully!"
        );
    }
}