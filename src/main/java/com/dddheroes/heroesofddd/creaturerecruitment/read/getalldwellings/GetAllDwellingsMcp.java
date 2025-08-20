package com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GetAllDwellingsMcp {

    private final QueryGateway queryGateway;
    private final ObjectMapper objectMapper;

    public GetAllDwellingsMcp(QueryGateway queryGateway, ObjectMapper objectMapper) {
        this.queryGateway = queryGateway;
        this.objectMapper = objectMapper;
    }

    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> getAllDwellingsResource() {
        var dwellingsResource = new McpSchema.Resource(
                "heroesofddd://games/{gameId}/dwellings",
                "All Dwellings",
                "Provides access to all dwellings in a game with their creature availability and recruitment costs. Uses path parameter gameId.",
                "application/json",
                new McpSchema.Annotations(
                        List.of(McpSchema.Role.USER, McpSchema.Role.ASSISTANT),
                        0.8
                )
        );

        var resourceSpecification = new McpServerFeatures.SyncResourceSpecification(
                dwellingsResource,
                (exchange, request) -> {
                    try {
                        var gameId = extractGameId(request.uri())
                                .orElseThrow(() -> new IllegalArgumentException("gameId parameter is required in URI (e.g., heroesofddd://games/game-123/dwellings)"));

                        var query = GetAllDwellings.query(gameId);
                        var result = queryGateway.query(query, GetAllDwellings.Result.class).get();

                        var jsonContent = formatDwellings(result);

                        return new McpSchema.ReadResourceResult(
                                List.of(new McpSchema.TextResourceContents(
                                        request.uri(),
                                        "application/json",
                                        jsonContent
                                ))
                        );
                    } catch (Exception e) {
                        String errorContent = String.format("""
                                {
                                  "error": "Failed to retrieve dwellings: %s",
                                  "dwellings": []
                                }
                                """, e.getMessage());

                        return new McpSchema.ReadResourceResult(
                                List.of(new McpSchema.TextResourceContents(
                                        request.uri(),
                                        "application/json",
                                        errorContent
                                ))
                        );
                    }
                }
        );

        return List.of(resourceSpecification);
    }

    private Optional<String> extractGameId(String uri) {
        // Expected URI pattern: heroesofddd://games/{gameId}/dwellings
        final String scheme = "heroesofddd://";
        final String expectedPath = "games";
        final String expectedEndpoint = "dwellings";

        if (!uri.startsWith(scheme)) {
            return Optional.empty();
        }

        String[] pathSegments = uri.substring(scheme.length()).split("/");

        if (pathSegments.length != 3 ||
                !expectedPath.equals(pathSegments[0]) ||
                !expectedEndpoint.equals(pathSegments[2])) {
            return Optional.empty();
        }

        String gameId = pathSegments[1];
        return gameId.trim().isEmpty() ? Optional.empty() : Optional.of(gameId);
    }

    private String formatDwellings(GetAllDwellings.Result result) {
        try {
            var dwellings = result.dwellings();

            var response = Map.of(
                    "dwellings", dwellings,
                    "count", dwellings.size(),
                    "timestamp", System.currentTimeMillis()
            );

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return String.format("""
                    {
                      "error": "Failed to serialize dwellings: %s",
                      "dwellings": []
                    }
                    """, e.getMessage());
        }
    }

}