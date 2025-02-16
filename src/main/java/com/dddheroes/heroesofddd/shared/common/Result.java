package com.dddheroes.heroesofddd.shared.common;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A discriminated union that encapsulates a successful outcome with a value of type T
 * or a failure with an arbitrary Throwable exception.
 * Inspired by the Result type in Kotlin.
 *
 * @param <T> The type of the value in case of success
 *
 * @author Mateusz Nowak
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

    /**
     * Creates a successful Result representing completion without a value
     *
     * @return A new Success instance representing void completion
     */
    static Result<Void> success() {
        return new Success<>(null);
    }

    /**
     * Creates a successful Result with the given value
     *
     * @param value The success value
     * @param <T> The type of the value
     * @return A new Success instance
     */
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a failure Result with the given exception
     *
     * @param exception The exception representing the failure
     * @param <T> The type parameter
     * @return A new Failure instance
     */
    static <T> Result<T> failure(Throwable exception) {
        return new Failure<>(exception);
    }

    /**
     * Wraps a supplier operation in a Result
     *
     * @param supplier The operation to execute
     * @param <T> The type of the result
     * @return A Result containing either the operation's result or any thrown exception
     */
    static <T> Result<T> of(Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        try {
            return success(supplier.get());
        } catch (Throwable e) {
            return failure(e);
        }
    }

    /**
     * Wraps a runnable operation in a Result
     *
     * @param runnable The operation to execute
     * @return A Result containing either the operation's result or any thrown exception
     */
    static Result<Void> of(Runnable runnable) {
        Objects.requireNonNull(runnable);
        try {
            return success();
        } catch (Throwable e) {
            return failure(e);
        }
    }

    /**
     * Returns true if this is a Success instance
     */
    boolean isSuccess();

    /**
     * Returns true if this is a Failure instance
     */
    default boolean isFailure() {
        return !isSuccess();
    }

    /**
     * Returns the value if this is success or throws the exception if this is failure
     *
     * @return The encapsulated value
     * @throws Throwable The encapsulated exception if this is a failure
     */
    T getOrThrow() throws Throwable;

    /**
     * Returns the encapsulated value if this is success or null if this is failure
     */
    T getOrNull();

    /**
     * Returns the encapsulated exception if this is failure or null if this is success
     */
    Throwable exceptionOrNull();

    /**
     * Maps the success value using the given transformation
     */
    <R> Result<R> map(Function<T, R> transform);

    /**
     * Returns the encapsulated value if this is success or the default value if this is failure
     */
    default T getOrDefault(T defaultValue) {
        return isSuccess() ? getOrNull() : defaultValue;
    }

    /**
     * Maps the success value using a transformation that may throw
     */
    default <R> Result<R> flatMap(Function<T, Result<R>> transform) {
        if (isSuccess()) {
            try {
                return transform.apply(getOrNull());
            } catch (Throwable e) {
                return failure(e);
            }
        }
        return failure(exceptionOrNull());
    }

    final class Success<T> implements Result<T> {
        private final T value;

        private Success(T value) {
            this.value = value;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public T getOrThrow() {
            return value;
        }

        @Override
        public T getOrNull() {
            return value;
        }

        @Override
        public Throwable exceptionOrNull() {
            return null;
        }

        @Override
        public <R> Result<R> map(Function<T, R> transform) {
            Objects.requireNonNull(transform);
            try {
                return Result.success(transform.apply(value));
            } catch (Throwable e) {
                return Result.failure(e);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Success<?> success)) return false;
            return Objects.equals(value, success.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "Success[" + value + "]";
        }
    }

    final class Failure<T> implements Result<T> {
        private final Throwable exception;

        private Failure(Throwable exception) {
            this.exception = Objects.requireNonNull(exception);
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public T getOrThrow() throws Throwable {
            throw exception;
        }

        @Override
        public T getOrNull() {
            return null;
        }

        @Override
        public Throwable exceptionOrNull() {
            return exception;
        }

        @Override
        public <R> Result<R> map(Function<T, R> transform) {
            Objects.requireNonNull(transform);
            return Result.failure(exception);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Failure<?> failure)) return false;
            return Objects.equals(exception, failure.exception);
        }

        @Override
        public int hashCode() {
            return Objects.hash(exception);
        }

        @Override
        public String toString() {
            return "Failure[" + exception + "]";
        }
    }
}
