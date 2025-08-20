package com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling;

import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BuildDwellingMcp {

    @Component
    static class Tools {

        private final CommandGateway commandGateway;

        public Tools(CommandGateway commandGateway) {
            this.commandGateway = commandGateway;
        }

        @Tool(
                name = "build_dwelling",
                description = "Build a creature dwelling for recruiting specific creatures. Establishes a new dwelling with associated creature type and recruitment cost. The playerId should be provided via MCP client context (similar to REST API headers)."
        )
        public CompletableFuture<Map<String, Object>> buildDwelling(
                @ToolParam(description = "Unique identifier for the game instance") String gameId,
                @ToolParam(description = "Unique identifier for the dwelling to build") String dwellingId,
                @ToolParam(description = "Type of creature this dwelling will recruit (e.g., 'ANGEL', 'DRAGON', 'GRIFFIN')") String creatureId,
                @ToolParam(description = "Resource cost per troop recruitment. Map of resource types to amounts (e.g., {'GOLD': 1000, 'WOOD': 10})") Map<String, Integer> costPerTroop,
                ToolContext toolContext
        ) {
            var playerId = playerId(toolContext);

            var command = BuildDwelling.command(dwellingId, creatureId, costPerTroop);

            return commandGateway.send(command, GameMetaData.with(gameId, playerId))
                    .thenApply(_ -> getSuccess(dwellingId, creatureId, costPerTroop, playerId))
                    .exceptionally(throwable -> failure(dwellingId, throwable));
        }

        @NotNull
        private static Map<String, Object> failure(String dwellingId, Throwable throwable) {
            return Map.of(
                    "success", false,
                    "error", throwable != null ? throwable.getMessage() : "Unknown error",
                    "dwellingId", dwellingId,
                    "message", "Failed to build dwelling: " + (throwable != null ? throwable.getMessage() : "Unknown error")
            );
        }

        @NotNull
        private static Map<String, Object> getSuccess(String dwellingId, String creatureId, Map<String, Integer> costPerTroop, String playerId) {
            return Map.of(
                    "success", true,
                    "dwellingId", dwellingId,
                    "creatureId", creatureId,
                    "costPerTroop", costPerTroop,
                    "playerId", playerId,
                    "message", "Dwelling built successfully"
            );
        }

        private String playerId(ToolContext toolContext) {
            return (String) toolContext.getContext().getOrDefault("playerId", "default-player-id");
        }

    }

    @Configuration
    static class Config {

        @Bean
        ToolCallbackProvider buildDwellingTool(BuildDwellingMcp.Tools sliceTools) {
            return MethodToolCallbackProvider.builder().toolObjects(sliceTools).build();
        }

    }
}