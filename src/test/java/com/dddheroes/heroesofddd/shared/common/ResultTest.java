package com.dddheroes.heroesofddd.shared.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Result")
class ResultTest {

    @Nested
    @DisplayName("Success")
    class SuccessTests {

        private final Result<String> success = Result.success("test");

        @Test
        @DisplayName("should be marked as success")
        void isSuccess() {
            assertThat(success.isSuccess()).isTrue();
            assertThat(success.isFailure()).isFalse();
        }

        @Test
        @DisplayName("should return value when getting or throwing")
        void getOrThrow() {
            assertThatNoException()
                    .isThrownBy(() -> assertThat(success.getOrThrow()).isEqualTo("test"));
        }

        @Test
        @DisplayName("should return value when getting or null")
        void getOrNull() {
            assertThat(success.getOrNull()).isEqualTo("test");
        }

        @Test
        @DisplayName("should return null for exception")
        void exceptionOrNull() {
            assertThat(success.exceptionOrNull()).isNull();
        }

        @Test
        @DisplayName("should return value when getting or default")
        void getOrDefault() {
            assertThat(success.getOrDefault("default")).isEqualTo("test");
        }

        @Test
        @DisplayName("should map value successfully")
        void map() {
            Result<Integer> mapped = success.map(String::length);
            assertThat(mapped.getOrNull()).isEqualTo(4);
        }

        @Test
        @DisplayName("should propagate exception in map")
        void mapWithException() {
            RuntimeException exception = new RuntimeException("Test exception");
            Result<Integer> mapped = success.map(_ -> {
                throw exception;
            });

            assertThat(mapped.isFailure()).isTrue();
            assertThat(mapped.exceptionOrNull()).isEqualTo(exception);
        }

        @Test
        @DisplayName("should flat map value successfully")
        void flatMap() {
            Result<Integer> flatMapped = success
                    .flatMap(str -> Result.success(str.length()));
            assertThat(flatMapped.getOrNull()).isEqualTo(4);
        }

        @Test
        @DisplayName("should propagate failure in flat map")
        void flatMapWithFailure() {
            RuntimeException exception = new RuntimeException("Test exception");
            Result<Integer> flatMapped = success.flatMap(__ -> Result.failure(exception));

            assertThat(flatMapped.isFailure()).isTrue();
            assertThat(flatMapped.exceptionOrNull()).isEqualTo(exception);
        }

        @Test
        @DisplayName("should handle equals correctly")
        void testEquals() {
            assertThat(Result.success("test"))
                    .isEqualTo(Result.success("test"))
                    .isNotEqualTo(Result.success("other"))
                    .isNotEqualTo(Result.failure(new RuntimeException("test")));
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void testHashCode() {
            Result<String> other = Result.success("test");
            assertThat(success)
                    .hasSameHashCodeAs(other);
        }

        @Test
        @DisplayName("should have meaningful toString")
        void testToString() {
            assertThat(success.toString())
                    .isEqualTo("Success[test]");
        }
    }

    @Nested
    @DisplayName("Failure")
    class FailureTests {

        private final RuntimeException exception = new RuntimeException("test exception");
        private final Result<String> failure = Result.failure(exception);

        @Test
        @DisplayName("should be marked as failure")
        void isFailure() {
            assertThat(failure.isFailure()).isTrue();
            assertThat(failure.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("should throw exception when getting or throwing")
        void getOrThrow() {
            assertThatThrownBy(failure::getOrThrow)
                    .isEqualTo(exception);
        }

        @Test
        @DisplayName("should return null when getting or null")
        void getOrNull() {
            assertThat(failure.getOrNull()).isNull();
        }

        @Test
        @DisplayName("should return exception")
        void exceptionOrNull() {
            assertThat(failure.exceptionOrNull()).isEqualTo(exception);
        }

        @Test
        @DisplayName("should return default when getting or default")
        void getOrDefault() {
            assertThat(failure.getOrDefault("default")).isEqualTo("default");
        }

        @Test
        @DisplayName("should propagate failure when mapping")
        void map() {
            Result<Integer> mapped = failure.map(String::length);
            assertThat(mapped.isFailure()).isTrue();
            assertThat(mapped.exceptionOrNull()).isEqualTo(exception);
        }

        @Test
        @DisplayName("should propagate failure when flat mapping")
        void flatMap() {
            Result<Integer> flatMapped = failure
                    .flatMap(str -> Result.success(str.length()));

            assertThat(flatMapped.isFailure()).isTrue();
            assertThat(flatMapped.exceptionOrNull()).isEqualTo(exception);
        }

        @Test
        @DisplayName("should handle equals correctly")
        void testEquals() {
            Result<String> sameFailure = Result.failure(exception);
            RuntimeException differentException = new RuntimeException("other exception");

            assertThat(failure)
                    .isEqualTo(sameFailure)
                    .isNotEqualTo(Result.failure(differentException))
                    .isNotEqualTo(Result.success("test"));
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void testHashCode() {
            Result<String> other = Result.failure(exception);
            assertThat(failure)
                    .hasSameHashCodeAs(other);
        }

        @Test
        @DisplayName("should have meaningful toString")
        void testToString() {
            assertThat(failure.toString())
                    .isEqualTo("Failure[" + exception.toString() + "]");
        }
    }

    @Nested
    @DisplayName("Static factory methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("success() should create void Result")
        void successWithoutValue() {
            Result<Void> result = Result.success();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getOrNull()).isNull();
            assertThat(result.exceptionOrNull()).isNull();

            // Should be able to map from Void to another type
            Result<String> mapped = result.map(v -> "mapped");
            assertThat(mapped.getOrNull()).isEqualTo("mapped");
        }

        @Test
        @DisplayName("void Result should work with flatMap")
        void voidResultFlatMap() {
            Result<Void> result = Result.success();

            Result<String> flatMapped = result
                    .flatMap(v -> Result.success("completed"));

            assertThat(flatMapped.getOrNull()).isEqualTo("completed");
        }

        @Test
        @DisplayName("of should capture successful operation")
        void ofSuccess() {
            Result<Integer> result = Result.of(() -> 42);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getOrNull()).isEqualTo(42);
        }

        @Test
        @DisplayName("of should capture failed operation")
        void ofFailure() {
            RuntimeException exception = new RuntimeException("test exception");
            Result<Integer> result = Result.of(() -> {throw exception;});

            assertThat(result.isFailure()).isTrue();
            assertThat(result.exceptionOrNull()).isEqualTo(exception);
        }

        @Test
        @DisplayName("of should handle null supplier")
        void ofNullSupplier() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Result.of(null));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle null value in success")
        void nullValueInSuccess() {
            Result<String> result = Result.success(null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getOrNull()).isNull();
        }

        @Test
        @DisplayName("should reject null exception in failure")
        void nullExceptionInFailure() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Result.failure(null));
        }

        @Test
        @DisplayName("should reject null transform in map")
        void nullTransformInMap() {
            Result<String> success = Result.success("test");
            Result<String> failure = Result.failure(new RuntimeException());

            assertThatNullPointerException()
                    .isThrownBy(() -> success.map(null));
            assertThatNullPointerException()
                    .isThrownBy(() -> failure.map(null));
        }

        @Test
        @DisplayName("should evaluate map function only once")
        void mapEvaluatedOnce() {
            AtomicInteger counter = new AtomicInteger(0);
            Result<String> success = Result.success("test");

            success.map(str -> {
                counter.incrementAndGet();
                return str.length();
            });

            assertThat(counter.get()).isEqualTo(1);
        }
    }
}