package com.ecommerce.shared.domain.common;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public sealed interface Result<T> permits Result.Success, Result.Failure {

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(String errorMessage) {
        return new Failure<>(errorMessage);
    }

    static <T> Result<T> failure(String errorCode, String errorMessage) {
        return new Failure<>(errorCode, errorMessage);
    }

    default boolean isSuccess() {
        return this instanceof Success<T>;
    }

    default boolean isFailure() {
        return this instanceof Failure<T>;
    }

    default T getValue() {
        return switch (this) {
            case Success<T> success -> success.value();
            case Failure<T> failure -> throw new IllegalStateException("Cannot get value from failure");
        };
    }

    default String getErrorMessage() {
        return switch (this) {
            case Success<T> success -> throw new IllegalStateException("Cannot get error from success");
            case Failure<T> failure -> failure.errorMessage();
        };
    }

    default String getErrorCode() {
        return switch (this) {
            case Success<T> success -> throw new IllegalStateException("Cannot get error code from success");
            case Failure<T> failure -> failure.errorCode();
        };
    }

    default <U> Result<U> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T> success -> Result.success(mapper.apply(success.value()));
            case Failure<T> failure -> Result.failure(failure.errorCode(), failure.errorMessage());
        };
    }

    default <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        return switch (this) {
            case Success<T> success -> mapper.apply(success.value());
            case Failure<T> failure -> Result.failure(failure.errorCode(), failure.errorMessage());
        };
    }

    default Result<T> peek(Consumer<T> consumer) {
        if (this instanceof Success<T> success) {
            consumer.accept(success.value());
        }
        return this;
    }

    default Result<T> peekError(Consumer<String> consumer) {
        if (this instanceof Failure<T> failure) {
            consumer.accept(failure.errorMessage());
        }
        return this;
    }

    record Success<T>(T value) implements Result<T> {
        public Success {
            Objects.requireNonNull(value, "Success value cannot be null");
        }
    }

    record Failure<T>(String errorCode, String errorMessage) implements Result<T> {
        public Failure(String errorMessage) {
            this("GENERAL_ERROR", errorMessage);
        }

        public Failure {
            Objects.requireNonNull(errorCode, "Error code cannot be null");
            Objects.requireNonNull(errorMessage, "Error message cannot be null");
        }
    }
}