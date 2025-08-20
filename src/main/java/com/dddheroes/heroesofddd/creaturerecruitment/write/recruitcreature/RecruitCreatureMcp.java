package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

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

public class RecruitCreatureMcp {

    @Component
    static class Tools {

        private final CommandGateway commandGateway;

        public Tools(CommandGateway commandGateway) {
            this.commandGateway = commandGateway;
        }

        @Tool(
                name = "recruit_creature",
                description = "Recruit creatures from a dwelling to an army. This will move creatures from the dwelling's available pool to the specified army, consuming resources in the process. The playerId should be provided via MCP client context (similar to REST API headers)."
        )
        public CompletableFuture<Map<String, Object>> recruitCreature(
                @ToolParam(description = "Unique identifier for the game instance") String gameId,
                @ToolParam(description = "Unique identifier for the dwelling to recruit from") String dwellingId,
                @ToolParam(description = "Type of creature to recruit (e.g., 'ANGEL', 'DRAGON', 'GRIFFIN')") String creatureId,
                @ToolParam(description = "Unique identifier for the army to recruit creatures to") String armyId,
                @ToolParam(description = "Number of creatures to recruit (positive integer)") Integer quantity,
                @ToolParam(description = "Expected cost of recruitment as resource map (e.g., {'gold': 1000, 'wood': 10})") Map<String, Integer> expectedCost,
                ToolContext toolContext
        ) {
            var playerId = playerId(toolContext);

            var command = RecruitCreature.command(dwellingId, creatureId, armyId, quantity, expectedCost);

            return commandGateway.send(command, GameMetaData.with(gameId, playerId))
                    .thenApply(_ -> success(dwellingId, creatureId, armyId, quantity, playerId))
                    .exceptionally(throwable -> failure(dwellingId, throwable));
        }

        @NotNull
        private static Map<String, Object> success(String dwellingId, String creatureId, String armyId, Integer quantity, String playerId) {
            return Map.of(
                    "success", true,
                    "dwellingId", dwellingId,
                    "creatureId", creatureId,
                    "armyId", armyId,
                    "quantity", quantity,
                    "playerId", playerId,
                    "message", "Creatures recruited successfully"
            );
        }

        @NotNull
        private static Map<String, Object> failure(String dwellingId, Throwable throwable) {
            return Map.of(
                    "success", false,
                    "error", throwable != null ? throwable.getMessage() : "Unknown error",
                    "dwellingId", dwellingId,
                    "message", "Failed to recruit creatures: " + (throwable != null ? throwable.getMessage() : "Unknown error")
            );
        }

        private String playerId(ToolContext toolContext) {
            return (String) toolContext.getContext().getOrDefault("playerId", "default-player-id");
        }

    }

    @Configuration
    static class Config {

        @Bean
        ToolCallbackProvider recruitCreatureTool(RecruitCreatureMcp.Tools sliceTools) {
            return MethodToolCallbackProvider.builder().toolObjects(sliceTools).build();
        }

    }
}