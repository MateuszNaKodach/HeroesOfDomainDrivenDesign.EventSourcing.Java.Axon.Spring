package com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures;

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

public class IncreaseAvailableCreaturesMcp {

    @Component
    static class Tools {

        private final CommandGateway commandGateway;

        public Tools(CommandGateway commandGateway) {
            this.commandGateway = commandGateway;
        }

        @Tool(
                name = "increase_available_creatures",
                description = "Increase the number of available creatures in a dwelling. This affects how many creatures can be recruited from this dwelling. The playerId should be provided via MCP client context (similar to REST API headers)."
        )
        public CompletableFuture<Map<String, Object>> increaseAvailableCreatures(
                @ToolParam(description = "Unique identifier for the game instance") String gameId,
                @ToolParam(description = "Unique identifier for the dwelling to modify") String dwellingId,
                @ToolParam(description = "Type of creature to increase availability for (e.g., 'ANGEL', 'DRAGON', 'GRIFFIN')") String creatureId,
                @ToolParam(description = "Number of creatures to increase availability by (positive integer)") Integer increaseBy,
                ToolContext toolContext
        ) {
            var playerId = playerId(toolContext);

            var command = IncreaseAvailableCreatures.command(dwellingId, creatureId, increaseBy);

            return commandGateway.send(command, GameMetaData.with(gameId, playerId))
                    .thenApply(_ -> success(dwellingId, creatureId, increaseBy, playerId))
                    .exceptionally(throwable -> failure(dwellingId, throwable));
        }

        @NotNull
        private static Map<String, Object> success(String dwellingId, String creatureId, Integer increaseBy, String playerId) {
            return Map.of(
                    "success", true,
                    "dwellingId", dwellingId,
                    "creatureId", creatureId,
                    "increaseBy", increaseBy,
                    "playerId", playerId,
                    "message", "Available creatures increased successfully"
            );
        }

        @NotNull
        private static Map<String, Object> failure(String dwellingId, Throwable throwable) {
            return Map.of(
                    "success", false,
                    "error", throwable != null ? throwable.getMessage() : "Unknown error",
                    "dwellingId", dwellingId,
                    "message", "Failed to increase available creatures: " + (throwable != null ? throwable.getMessage() : "Unknown error")
            );
        }

        private String playerId(ToolContext toolContext) {
            return (String) toolContext.getContext().getOrDefault("playerId", "default-player-id");
        }

    }

    @Configuration
    static class Config {

        @Bean
        ToolCallbackProvider increaseAvailableCreaturesTool(IncreaseAvailableCreaturesMcp.Tools sliceTools) {
            return MethodToolCallbackProvider.builder().toolObjects(sliceTools).build();
        }

    }
}