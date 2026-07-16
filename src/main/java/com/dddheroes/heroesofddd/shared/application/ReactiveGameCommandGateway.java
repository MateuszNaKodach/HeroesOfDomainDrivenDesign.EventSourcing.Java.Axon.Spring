package com.dddheroes.heroesofddd.shared.application;

import org.axonframework.extension.reactor.messaging.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.MessageTypeResolver;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Application-level reactive command gateway that carries the game {@link Metadata}.
 * {@link ReactorCommandGateway} has no {@code Metadata} overload, so the command message is
 * pre-built here — a ready {@code CommandMessage} passes through the gateway untouched.
 * <p>
 * With the JPA event store ({@code axon.axonserver.enabled=false}) command handling executes the
 * blocking event-store work inline on the calling thread, so the send is subscribed on the
 * bounded-elastic scheduler to keep it off the WebFlux event loop. With Axon Server the dispatch
 * is non-blocking gRPC and the offload is skipped.
 */
@Component
public class ReactiveGameCommandGateway {

    private final ReactorCommandGateway reactorCommandGateway;
    private final MessageTypeResolver messageTypeResolver;
    private final boolean offloadBlockingEventStore;

    public ReactiveGameCommandGateway(
            ReactorCommandGateway reactorCommandGateway,
            MessageTypeResolver messageTypeResolver,
            @Value("${axon.axonserver.enabled:false}") boolean axonServerEnabled
    ) {
        this.reactorCommandGateway = reactorCommandGateway;
        this.messageTypeResolver = messageTypeResolver;
        this.offloadBlockingEventStore = !axonServerEnabled;
    }

    public Mono<Void> send(Object command, Metadata metadata) {
        var result = Mono.defer(() -> {
            var message = new GenericCommandMessage(
                    messageTypeResolver.resolveOrThrow(command), command, metadata);
            return reactorCommandGateway.send(message, (ProcessingContext) null);
        });
        return offloadBlockingEventStore ? result.subscribeOn(Schedulers.boundedElastic()) : result;
    }
}
