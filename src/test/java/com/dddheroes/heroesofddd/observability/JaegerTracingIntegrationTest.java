package com.dddheroes.heroesofddd.observability;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.creaturerecruitment.events.CreatureRecruited;
import com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingById;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreature;
import com.dddheroes.heroesofddd.observability.JaegerClient.Span;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * GOLDEN-MASTER / REGRESSION BASELINE of the distributed-tracing spans produced by
 * <b>Axon Framework 4</b> (4.13.1) + {@code axon-tracing-opentelemetry} for this application.
 *
 * <p>This test exists to <b>pin down the exact span names and attributes</b> Axon 4 emits for a
 * representative command → event → projection → query flow, so that when the application is migrated
 * to <b>Axon Framework 5</b> the same test can be re-run and any change in tracing output (renamed
 * operations, dropped spans, changed span kinds, renamed/added/removed attributes, broken context
 * propagation) shows up as a concrete, reviewable diff rather than going unnoticed.
 *
 * <p>The full Axon-4 baseline this test enforces is also written up, span by span, in
 * {@code AXON4_TRACING_BASELINE.md} next to this file — keep the two in sync.
 *
 * <h2>What the spans look like in Axon 4</h2>
 * Axon's {@code OpenTelemetrySpanFactory} names spans {@code "<operation>(<MessageName>)"} where the
 * message name is the command/query name (FQCN) or the event payload simple name, and emits them under
 * the {@code "axon-framework"} instrumentation scope (the tracer name wired in {@code TracingConfiguration}).
 * Because Axon Server is used, command/query buses are <b>distributed</b>, so each command yields both the
 * local ({@code dispatchCommand}/{@code handleCommand}) and distributed
 * ({@code dispatchDistributedCommand}/{@code handleDistributedCommand}) spans.
 *
 * <h2>Stack &amp; determinism</h2>
 * Axon Server (event store + routing), PostgreSQL (read model) and Jaeger (OTLP backend) all run in Docker
 * via Testcontainers. Determinism is achieved with: a fresh Jaeger per run, sampling forced to 1.0,
 * {@code SdkTracerProvider.forceFlush()} before each read (drains the OTLP batch processor), and Awaitility
 * polling the Jaeger query API until the whole expected span set is present.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles({"observability", "observability-jaeger", "axonserver"})
@Testcontainers
class JaegerTracingIntegrationTest {

    private static final String SERVICE_NAME = "heroesofddd";
    private static final Duration LOOKBACK = Duration.ofMinutes(10);

    // ===============================================================================================
    // THE AXON 4 BASELINE — exact span operation names this flow produces.
    // (Span name format: "<operation>(<MessageName>)"; "*Distributed*" variants come from Axon Server.)
    // ===============================================================================================

    /** Spans produced by Axon's own bus/repository/processor SpanFactories (the framework tracing surface). */
    private static final Set<String> EXPECTED_AXON_FRAMEWORK_SPANS = new TreeSet<>(List.of(
            // -- CommandBus: dispatch + handle, each in a local and an Axon-Server-distributed variant --
            "CommandBus.dispatchCommand(BuildDwelling)",
            "CommandBus.dispatchDistributedCommand(BuildDwelling)",
            "CommandBus.handleCommand(BuildDwelling)",
            "CommandBus.handleDistributedCommand(BuildDwelling)",
            "CommandBus.dispatchCommand(IncreaseAvailableCreatures)",
            "CommandBus.dispatchDistributedCommand(IncreaseAvailableCreatures)",
            "CommandBus.handleCommand(IncreaseAvailableCreatures)",
            "CommandBus.handleDistributedCommand(IncreaseAvailableCreatures)",
            "CommandBus.dispatchCommand(RecruitCreature)",
            "CommandBus.dispatchDistributedCommand(RecruitCreature)",
            "CommandBus.handleCommand(RecruitCreature)",
            "CommandBus.handleDistributedCommand(RecruitCreature)",
            "CommandBus.dispatchCommand(AddCreatureToArmy)",
            "CommandBus.dispatchDistributedCommand(AddCreatureToArmy)",
            "CommandBus.handleCommand(AddCreatureToArmy)",
            "CommandBus.handleDistributedCommand(AddCreatureToArmy)",
            // -- EventBus: one publish span per event + the internal commit span --
            "EventBus.publishEvent(DwellingBuilt)",
            "EventBus.publishEvent(AvailableCreaturesChanged)",
            "EventBus.publishEvent(CreatureRecruited)",
            "EventBus.publishEvent(CreatureAddedToArmy)",
            "EventBus.commitEvents",
            // -- QueryBus: dispatch (local + distributed) + processing + response --
            "QueryBus.query(GetDwellingById)",
            "QueryBus.queryDistributed(GetDwellingById)",
            "QueryBus.processQueryMessage(GetDwellingById)",
            "QueryBus.processQueryResponse(GetDwellingById)",
            // -- Repository: event-sourced aggregate loading lifecycle --
            "Repository.load",
            "Repository.obtainLock",
            "Repository.initializeState(Dwelling)",
            // -- Event processors re-reading the events: pooled/streaming + the subscribing query cache --
            "StreamingEventProcessor.process(DwellingBuilt)",
            "StreamingEventProcessor.process(AvailableCreaturesChanged)",
            "StreamingEventProcessor.process(CreatureRecruited)",
            "EventProcessor.process(DwellingBuilt)"
    ));

    /**
     * Spans for the actual {@code @MessageHandler} method invocations, wrapped by Axon's
     * {@code TracingHandlerEnhancerDefinition} as {@code "<DeclaringClass>.<method>(<ParamTypes>)"}.
     * (Aggregate {@code @EventSourcingHandler} replays are intentionally absent because
     * {@code axon.tracing.show-event-sourcing-handlers=false} — itself a baseline fact this set proves.)
     */
    private static final Set<String> EXPECTED_HANDLER_SPANS = new TreeSet<>(List.of(
            "Dwelling.decide(BuildDwelling)",
            "Dwelling.decide(IncreaseAvailableCreatures)",
            "Dwelling.decide(RecruitCreature)",
            "Army.decide(AddCreatureToArmy)",
            "DwellingReadModelProjector.on(DwellingBuilt,String)",
            "DwellingReadModelProjector.on(AvailableCreaturesChanged)",
            "DwellingReadModelProjector.on(CreatureRecruited)",
            "WhenCreatureRecruitedThenAddToArmyProcessor.react(CreatureRecruited,String,String)",
            "WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor.on(DwellingBuilt,String)",
            "GetAllDwellingsQueryHandler.evolve(DwellingBuilt,String)",
            "GetDwellingByIdQueryHandler.handle(GetDwellingById)"
    ));

    /** Spring/Micrometer HTTP <i>server</i> spans (trace roots) — not produced by Axon, but pinned for context. */
    private static final Set<String> EXPECTED_HTTP_SERVER_SPANS = new TreeSet<>(List.of(
            "http put /games/{gameId}/dwellings/{dwellingId}",
            "http put /games/{gameId}/dwellings/{dwellingId}/available-creatures-increases",
            "http put /games/{gameId}/dwellings/{dwellingId}/creature-recruitments",
            "http get /games/{gameId}/dwellings/{dwellingId}"
    ));

    /**
     * JDBC/JPA spans from {@code datasource-micrometer}. The instrumented DataSource emits these as children
     * of whichever Axon handler span is doing the DB work (token store, JPA read model). They share the
     * {@code org.springframework.boot} instrumentation scope with the HTTP spans.
     */
    private static final Set<String> EXPECTED_JPA_SPANS = new TreeSet<>(List.of(
            "connection",  // JDBC connection acquisition
            "query",       // SQL statement execution (SQL text on the jdbc.query[0] tag)
            "result-set"   // result-set traversal
    ));

    /** Operation-name prefixes owned by Axon's framework SpanFactories — used to police for new/renamed spans. */
    private static final List<String> AXON_FRAMEWORK_PREFIXES = List.of(
            "CommandBus.", "EventBus.", "QueryBus.", "Repository.",
            "StreamingEventProcessor.", "EventProcessor.",
            "Snapshotter.", "SagaManager.", "DeadlineManager.", "QueryUpdateEmitter."
    );

    @Container
    static final GenericContainer<?> JAEGER =
            new GenericContainer<>(DockerImageName.parse("jaegertracing/jaeger:2.5.0"))
                    .withExposedPorts(16686, 4318)
                    .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void tracingProperties(DynamicPropertyRegistry registry) {
        // Point OTLP export at the ephemeral Jaeger container (overrides the localhost:4318 default).
        registry.add(
                "management.otlp.tracing.endpoint",
                () -> "http://%s:%d/v1/traces".formatted(JAEGER.getHost(), JAEGER.getMappedPort(4318))
        );
        // Re-enable tracing for THIS TEST ONLY. The production profiles are already correct — the
        // 'observability' profile (application-observability.yaml) sets management.tracing.enabled=true and
        // that value wins at runtime. But Spring Boot deliberately injects a high-precedence 'test' property
        // source under @SpringBootTest that forces management.tracing.enabled=false, so we must override it
        // here (a @DynamicPropertySource outranks the 'test' source). Sampling (1.0) and transport (http)
        // come from the observability profile and are not disabled in tests.
        registry.add("management.tracing.enabled", () -> "true");
    }

    private static JaegerClient jaeger;

    @BeforeAll
    static void initJaegerClient() {
        jaeger = new JaegerClient("http://%s:%d".formatted(JAEGER.getHost(), JAEGER.getMappedPort(16686)));
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    /** Lets us force-drain the OTLP batch processor so spans show up in Jaeger deterministically. */
    @Autowired
    ObjectProvider<SdkTracerProvider> tracerProvider;

    /** Present because the {@code axonserver} profile is active; used to wait until the bus is connected. */
    @Autowired
    ObjectProvider<AxonServerConnectionManager> axonServerConnectionManager;

    @Nested
    @DisplayName("Axon 4 tracing baseline for the recruit-a-creature flow")
    class RecruitCreatureFlow {

        @Test
        @DisplayName("emits exactly the documented set of Axon spans, with the documented attributes, into Jaeger")
        void emitsBaselineSpansAndAttributes() {
            // given
            awaitAxonServerConnected();
            var gameId = UUID.randomUUID().toString();
            var playerId = UUID.randomUUID().toString();
            var dwellingId = UUID.randomUUID().toString();
            var armyId = UUID.randomUUID().toString();
            var creatureId = "angel";

            // when — drive the write model, the cross-aggregate automation, and the read model over REST
            buildDwelling(gameId, playerId, dwellingId, creatureId);
            increaseAvailableCreatures(gameId, playerId, dwellingId, creatureId, 5);
            recruitCreature(gameId, playerId, dwellingId, creatureId, armyId, 2);
            getDwellingById(gameId, dwellingId);

            // then — poll Jaeger until the entire baseline span set is present (absorbs async export/processors)
            await().atMost(Duration.ofSeconds(60))
                   .pollInterval(Duration.ofSeconds(2))
                   .ignoreExceptions()
                   .untilAsserted(() -> assertBaseline(flushAndFetchSpans(), gameId, playerId, dwellingId));
        }

        private void assertBaseline(List<Span> spans, String gameId, String playerId, String dwellingId) {
            assertThat(spans).isNotEmpty();
            var operations = distinctOperations(spans);

            // ---------- 1) EXACT span-name catalog (regression signal for renames/removals/additions) ----------
            // STRICT: the complete set of Axon-emitted spans (those under the "axon-framework" scope —
            // both the bus/repository/processor spans and the @MessageHandler-invocation spans) must EQUAL
            // the documented baseline. Any missing span (rename/removal) OR any extra/unexpected span that
            // does not match the baseline fails here with a concrete diff.
            var expectedAxonSpans = new TreeSet<String>();
            expectedAxonSpans.addAll(EXPECTED_AXON_FRAMEWORK_SPANS);
            expectedAxonSpans.addAll(EXPECTED_HANDLER_SPANS);
            var actualAxonSpans = spans.stream()
                    .filter(s -> "axon-framework".equals(s.tags().get("otel.scope.name")))
                    .map(Span::operationName)
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
            assertThat(actualAxonSpans)
                    .as("the exact set of Axon-emitted spans must match the documented baseline "
                        + "(no missing, no unexpected — update the baseline if the new version intends the change)")
                    .isEqualTo(expectedAxonSpans);

            // HTTP server spans (Spring/Micrometer; not Axon scope) — assert presence only. Spring also emits
            // low-cardinality client spans ("http put"/"http get"), so this side is not policed exhaustively.
            assertThat(operations)
                    .as("all documented HTTP server spans must be present")
                    .containsAll(EXPECTED_HTTP_SERVER_SPANS);

            // JPA/JDBC spans (datasource-micrometer) — assert presence. Their count/SQL varies per run, so
            // this side is not policed exhaustively either.
            assertThat(operations)
                    .as("all documented JPA/JDBC spans must be present")
                    .containsAll(EXPECTED_JPA_SPANS);

            // ---------- 2) Attributes — exact key sets (regression signal for attribute add/remove) ----------
            // Command dispatch span (local): INTERNAL kind, carries the W3C traceparent it propagates downstream.
            var dispatchRecruit = findSpan(spans, "CommandBus.dispatchCommand(RecruitCreature)");
            assertTagKeys(dispatchRecruit,
                          "axon_message_id", "axon_message_name", "axon_message_type",
                          "axon_metadata_gameId", "axon_metadata_playerId", "axon_metadata_traceparent",
                          "axon_payload_type", "otel.scope.name", "span.kind");

            // Command handling span: CONSUMER kind, same attribute set.
            var handleRecruit = findSpan(spans, "CommandBus.handleCommand(RecruitCreature)");
            assertTagKeys(handleRecruit,
                          "axon_message_id", "axon_message_name", "axon_message_type",
                          "axon_metadata_gameId", "axon_metadata_playerId", "axon_metadata_traceparent",
                          "axon_payload_type", "otel.scope.name", "span.kind");

            // Event publish span: PRODUCER kind. NOTE the baseline asymmetry — it has correlationId/traceId
            // (Axon causation metadata) but NO traceparent (that is injected on the async dispatch path).
            var recruitPublish = findSpan(spans, "EventBus.publishEvent(CreatureRecruited)");
            assertTagKeys(recruitPublish,
                          "axon_aggregate_identifier", "axon_message_id", "axon_message_type",
                          "axon_metadata_correlationId", "axon_metadata_gameId", "axon_metadata_playerId",
                          "axon_metadata_traceId", "axon_payload_type", "otel.scope.name", "span.kind");

            // Async event-processing span: CONSUMER kind, tracked-event type, and it DOES carry traceparent.
            var processRecruit = findSpan(spans, "StreamingEventProcessor.process(CreatureRecruited)");
            assertTagKeys(processRecruit,
                          "axon_aggregate_identifier", "axon_message_id", "axon_message_type",
                          "axon_metadata_correlationId", "axon_metadata_gameId", "axon_metadata_playerId",
                          "axon_metadata_traceId", "axon_metadata_traceparent", "axon_payload_type",
                          "otel.scope.name", "span.kind");

            // Query processing span: CONSUMER kind. NO gameId metadata — the query is dispatched without it.
            var processQuery = findSpan(spans, "QueryBus.processQueryMessage(GetDwellingById)");
            assertTagKeys(processQuery,
                          "axon_message_id", "axon_message_name", "axon_message_type",
                          "axon_metadata_traceparent", "axon_payload_type", "otel.scope.name", "span.kind");

            // Handler-enhancer span: ONLY scope + kind (message attribute providers do not run for these).
            var decideRecruit = findSpan(spans, "Dwelling.decide(RecruitCreature)");
            assertTagKeys(decideRecruit, "otel.scope.name", "span.kind");

            // ---------- 3) Attributes — exact values (regression signal for value/format changes) ----------
            // Instrumentation scope == the tracer name wired in TracingConfiguration, on EVERY Axon span.
            assertThat(spans.stream().filter(s -> isAxonFrameworkSpan(s.operationName())))
                    .as("every Axon framework span is emitted under the 'axon-framework' scope")
                    .allMatch(s -> "axon-framework".equals(s.tags().get("otel.scope.name")));

            // span.kind mapping (CONSUMER=handling, PRODUCER=publishing, INTERNAL=local dispatch/handler).
            assertTag(dispatchRecruit, "span.kind", "internal");
            assertTag(handleRecruit, "span.kind", "consumer");
            assertTag(recruitPublish, "span.kind", "producer");
            assertTag(processRecruit, "span.kind", "consumer");
            assertTag(decideRecruit, "span.kind", "internal");

            // Message identity attributes (Axon Server distribution shows up in the message TYPE).
            assertTag(handleRecruit, "axon_message_type", "GrpcBackedCommandMessage");
            assertTag(handleRecruit, "axon_message_name", RecruitCreature.class.getName());
            assertTag(handleRecruit, "axon_payload_type", RecruitCreature.class.getName());
            assertTag(handleRecruit, "axon_metadata_" + GameMetaData.GAME_ID_KEY, gameId);
            assertTag(handleRecruit, "axon_metadata_" + GameMetaData.PLAYER_ID_KEY, playerId);

            assertTag(recruitPublish, "axon_message_type", "GenericDomainEventMessage");
            assertTag(recruitPublish, "axon_payload_type", CreatureRecruited.class.getName());
            assertTag(recruitPublish, "axon_aggregate_identifier", "Dwelling:" + dwellingId);
            assertTag(recruitPublish, "axon_metadata_" + GameMetaData.GAME_ID_KEY, gameId);

            assertTag(processRecruit, "axon_message_type", "GenericTrackedDomainEventMessage");
            assertTag(processRecruit, "axon_aggregate_identifier", "Dwelling:" + dwellingId);

            assertTag(processQuery, "axon_message_type", "GrpcBackedQueryMessage");
            assertTag(processQuery, "axon_message_name", GetDwellingById.class.getName());
            assertTag(processQuery, "axon_payload_type", GetDwellingById.class.getName());

            // HTTP server span (Spring/Micrometer) — pinned for the trace-root context.
            var httpPut = findSpan(spans, "http put /games/{gameId}/dwellings/{dwellingId}");
            assertTag(httpPut, "span.kind", "server");
            assertTag(httpPut, "method", "PUT");
            assertTag(httpPut, "status", "200");
            assertTag(httpPut, "outcome", "SUCCESS");
            assertTag(httpPut, "uri", "/games/{gameId}/dwellings/{dwellingId}");

            // JPA/JDBC query span (datasource-micrometer): CLIENT kind, PostgreSQL driver, SQL text present.
            var querySpan = findSpan(spans, "query");
            assertTag(querySpan, "span.kind", "client");
            assertTag(querySpan, "jdbc.datasource.driver", "org.postgresql.Driver");
            assertThat(querySpan.tags())
                    .as("query span must carry the executed SQL on the jdbc.query[0] tag")
                    .containsKey("jdbc.query[0]");

            // ---------- 4) Causation + W3C context propagation (regression signal for broken correlation) ----------
            // The recruit event's correlationId/traceId point back to the originating command's message id.
            var recruitCommandId = dispatchRecruit.tags().get("axon_message_id");
            assertThat(recruitCommandId).as("dispatch span exposes the command message id").isNotBlank();
            assertTag(recruitPublish, "axon_metadata_correlationId", recruitCommandId);
            assertTag(recruitPublish, "axon_metadata_traceId", recruitCommandId);

            // traceparent is a well-formed W3C header, and the async handler stays in the SAME trace as dispatch.
            assertW3cTraceparent(dispatchRecruit);
            assertW3cTraceparent(processRecruit);
            assertThat(processRecruit.tags().get("axon_metadata_traceparent"))
                    .as("async event handler must stay within the dispatching command's W3C trace")
                    .contains(w3cTraceId(dispatchRecruit));

            // Sanity: the unique gameId correlates many spans across the whole flow.
            assertThat(spans.stream().filter(s -> gameId.equals(s.tags().get("axon_metadata_" + GameMetaData.GAME_ID_KEY))))
                    .as("the unique gameId correlates commands, events and async handlers")
                    .hasSizeGreaterThanOrEqualTo(10);
        }

        private boolean isAxonFrameworkSpan(String operationName) {
            return AXON_FRAMEWORK_PREFIXES.stream().anyMatch(operationName::startsWith);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // HTTP helpers (mirror the production REST controllers)
    // ---------------------------------------------------------------------------------------------

    private void buildDwelling(String gameId, String playerId, String dwellingId, String creatureId) {
        var body = Map.of("creatureId", creatureId, "costPerTroop", Map.of("GOLD", 3000, "GEMS", 1));
        put("/games/%s/dwellings/%s".formatted(gameId, dwellingId), playerId, body);
    }

    private void increaseAvailableCreatures(String gameId, String playerId, String dwellingId,
                                            String creatureId, int increaseBy) {
        var body = Map.of("creatureId", creatureId, "increaseBy", increaseBy);
        put("/games/%s/dwellings/%s/available-creatures-increases".formatted(gameId, dwellingId), playerId, body);
    }

    private void recruitCreature(String gameId, String playerId, String dwellingId, String creatureId,
                                 String armyId, int quantity) {
        // expectedCost must equal costPerTroop * quantity (GOLD 3000, GEMS 1) per the Dwelling aggregate rule
        var body = Map.of(
                "creatureId", creatureId,
                "armyId", armyId,
                "quantity", quantity,
                "expectedCost", Map.of("GOLD", 3000 * quantity, "GEMS", quantity)
        );
        put("/games/%s/dwellings/%s/creature-recruitments".formatted(gameId, dwellingId), playerId, body);
    }

    private void getDwellingById(String gameId, String dwellingId) {
        var response = restTemplate.getForEntity(
                url("/games/%s/dwellings/%s".formatted(gameId, dwellingId)), String.class);
        // The read model may not be projected yet (async); we only care that the query span is emitted.
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    private void put(String path, String playerId, Map<String, ?> body) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-player-id", playerId);
        var response = restTemplate.exchange(
                url(path), HttpMethod.PUT, new HttpEntity<>(body, headers), Void.class);
        assertThat(response.getStatusCode()).as("PUT %s should succeed", path).isEqualTo(HttpStatus.OK);
    }

    private String url(String path) {
        return "http://localhost:%d%s".formatted(port, path);
    }

    // ---------------------------------------------------------------------------------------------
    // Tracing helpers
    // ---------------------------------------------------------------------------------------------

    private List<Span> flushAndFetchSpans() {
        // Drain the OTLP BatchSpanProcessor so anything already ended is exported now, then read it back.
        tracerProvider.ifAvailable(provider -> provider.forceFlush().join(10, TimeUnit.SECONDS));
        return jaeger.fetchSpans(SERVICE_NAME, LOOKBACK);
    }

    private static Set<String> distinctOperations(List<Span> spans) {
        return spans.stream().map(Span::operationName).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
    }

    private Span findSpan(List<Span> spans, String exactOperationName) {
        return spans.stream()
                    .filter(s -> s.operationName().equals(exactOperationName))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "No span named '%s'. Present operations: %s".formatted(
                                    exactOperationName, distinctOperations(spans))));
    }

    private void assertTag(Span span, String key, String expectedValue) {
        assertThat(span.tags().get(key))
                .as("span '%s' tag '%s'", span.operationName(), key)
                .isEqualTo(expectedValue);
    }

    private void assertTagKeys(Span span, String... expectedKeys) {
        assertThat(span.tags().keySet())
                .as("span '%s' attribute keys", span.operationName())
                .containsExactlyInAnyOrder(expectedKeys);
    }

    /** Asserts the span carries a well-formed W3C {@code traceparent}: {@code 00-<32hex>-<16hex>-<2hex>}. */
    private void assertW3cTraceparent(Span span) {
        assertThat(span.tags().get("axon_metadata_traceparent"))
                .as("span '%s' W3C traceparent", span.operationName())
                .matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}");
    }

    /** Extracts the 32-hex trace-id from a span's W3C traceparent. */
    private String w3cTraceId(Span span) {
        return span.tags().get("axon_metadata_traceparent").split("-")[1];
    }

    private void awaitAxonServerConnected() {
        axonServerConnectionManager.ifAvailable(manager ->
                await().atMost(Duration.ofSeconds(15))
                       .untilAsserted(() -> assertThat(manager.getConnection().isConnected()).isTrue()));
    }
}
