package com.dddheroes.heroesofddd.shared.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.axonframework.common.tx.TransactionalExecutor;
import org.axonframework.conversion.GeneralConverter;
import org.axonframework.extension.springboot.TokenStoreProperties;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.core.unitofwork.transaction.TransactionalExecutorProvider;
import org.axonframework.messaging.core.unitofwork.transaction.jpa.JpaTransactionalExecutorProvider;
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.TokenStore;
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.jpa.JpaTokenStore;
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.jpa.JpaTokenStoreConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Overrides the auto-configured {@link TokenStore} to work around AxonIQ/AxonFramework#4632.
 * <p>
 * On Axon Framework 5.2.0-SNAPSHOT the pooled-processor coordinator initializes token segments asynchronously and runs
 * the JPA {@code persist} as a continuation chained off the Axon Server initial-token gRPC future - i.e. on the
 * connector's {@code FutureStreamObserver.onNext} callback thread. The default {@link JpaTransactionalExecutorProvider}
 * relies on a Spring transaction bound to that thread (via the container-managed {@code EntityManager}); there is none
 * on the gRPC thread, so startup fails with {@code TransactionRequiredException} and the processor never starts.
 * <p>
 * This bean wires the {@link JpaTokenStore} with an executor provider that:
 * <ul>
 *   <li>uses the framework's normal, transaction-bound path whenever a Spring transaction is actually active on the
 *       current thread - e.g. the pooled worker thread running the event-processing unit of work - so token writes
 *       stay in the same transaction as the read-model/projection updates (atomic, unchanged behaviour); and</li>
 *   <li>falls back to opening (and committing) its own JPA transaction when no Spring transaction is active on the
 *       current thread - e.g. the gRPC-thread token-segment initialization - so the persist succeeds regardless of
 *       which thread the async continuation lands on.</li>
 * </ul>
 * The fallback only affects token-store-only operations (notably segment initialization), which have no atomicity
 * requirement with other resources, so correctness during normal event processing is preserved.
 * <p>
 * <strong>Temporary.</strong> Remove this once AxonIQ/AxonFramework#4632 is fixed upstream and the dependency is bumped.
 */
@Configuration
public class TokenStoreConfiguration {

    /**
     * Replaces the auto-configured JPA {@link TokenStore} (which backs off via {@code @ConditionalOnMissingBean}).
     */
    @Bean
    public TokenStore tokenStore(EntityManagerFactory entityManagerFactory,
                                 GeneralConverter converter,
                                 TokenStoreProperties tokenStoreProperties) {
        JpaTokenStoreConfiguration config =
                JpaTokenStoreConfiguration.DEFAULT.claimTimeout(tokenStoreProperties.getClaimTimeout());
        return new JpaTokenStore(
                new ThreadBoundAwareJpaExecutorProvider(new JpaTransactionalExecutorProvider(entityManagerFactory)),
                converter,
                config
        );
    }

    /**
     * Delegates to the framework's {@link JpaTransactionalExecutorProvider}, choosing between its transaction-bound
     * path (when a Spring transaction is active on the calling thread) and its own-transaction path (otherwise).
     */
    private static final class ThreadBoundAwareJpaExecutorProvider
            implements TransactionalExecutorProvider<EntityManager> {

        private final JpaTransactionalExecutorProvider delegate;

        private ThreadBoundAwareJpaExecutorProvider(JpaTransactionalExecutorProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public TransactionalExecutor<EntityManager> getTransactionalExecutor(ProcessingContext processingContext) {
            boolean springTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            boolean executorAvailable = processingContext != null
                    && processingContext.getResource(JpaTransactionalExecutorProvider.SUPPLIER_KEY) != null;
            if (springTransactionActive && executorAvailable) {
                // Transaction-bound path: token write joins the active Spring transaction on this thread.
                return delegate.getTransactionalExecutor(processingContext);
            }
            // Own-transaction path: safe on any thread (e.g. gRPC token-segment initialization).
            return delegate.getTransactionalExecutor(null);
        }
    }
}
