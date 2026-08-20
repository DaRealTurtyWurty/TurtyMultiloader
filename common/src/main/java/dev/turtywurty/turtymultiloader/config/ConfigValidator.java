package dev.turtywurty.turtymultiloader.config;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Performs whole-object validation after decoding and before a value is installed or saved.
 */
@FunctionalInterface
public interface ConfigValidator<T> {
    List<String> validate(T value);

    static <T> ConfigValidator<T> none() {
        return value -> List.of();
    }

    static <T> ConfigValidator<T> predicate(Predicate<? super T> predicate, String failureMessage) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(failureMessage, "failureMessage");
        return value -> predicate.test(value) ? List.of() : List.of(failureMessage);
    }
}
