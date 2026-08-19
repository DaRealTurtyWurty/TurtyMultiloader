package dev.turtywurty.turtymultiloader.transfer.resource;

import dev.turtywurty.turtymultiloader.transfer.unit.UnitDimension;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Describes one family of transferable resources. Identity is its namespaced identifier.
 */
public final class ResourceType<T> {
    private final Identifier id;
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final UnitDimension unitDimension;
    private final Predicate<? super T> emptyPredicate;
    private final Supplier<? extends Holder<T>> emptyHolder;

    private ResourceType(
        Identifier id,
        ResourceKey<? extends Registry<T>> registryKey,
        UnitDimension unitDimension,
        Predicate<? super T> emptyPredicate,
        Supplier<? extends Holder<T>> emptyHolder
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.registryKey = registryKey;
        this.unitDimension = Objects.requireNonNull(unitDimension, "unitDimension");
        this.emptyPredicate = Objects.requireNonNull(emptyPredicate, "emptyPredicate");
        this.emptyHolder = Objects.requireNonNull(emptyHolder, "emptyHolder");
    }

    public static <T> ResourceType<T> registered(
        Identifier id,
        ResourceKey<? extends Registry<T>> registryKey,
        UnitDimension unitDimension,
        Predicate<? super T> emptyPredicate,
        Supplier<? extends Holder<T>> emptyHolder
    ) {
        return new ResourceType<>(id, Objects.requireNonNull(registryKey, "registryKey"), unitDimension,
            emptyPredicate, emptyHolder);
    }

    public static <T> ResourceType<T> direct(
        Identifier id,
        UnitDimension unitDimension,
        Predicate<? super T> emptyPredicate,
        Supplier<? extends T> emptyValue
    ) {
        Objects.requireNonNull(emptyValue, "emptyValue");
        return new ResourceType<>(id, null, unitDimension, emptyPredicate, () -> Holder.direct(emptyValue.get()));
    }

    public Identifier id() {
        return this.id;
    }

    /**
     * The backing vanilla registry, when this family consists of registered values.
     */
    public Optional<ResourceKey<? extends Registry<T>>> registryKey() {
        return Optional.ofNullable(this.registryKey);
    }

    /**
     * The dimension every storage of this resource family must use.
     */
    public UnitDimension unitDimension() {
        return this.unitDimension;
    }

    public boolean isEmpty(T value) {
        return this.emptyPredicate.test(value);
    }

    public ResourceVariant<T> empty() {
        return ResourceVariant.of(this, this.emptyHolder.get());
    }

    public ResourceVariant<T> of(Holder<T> holder) {
        return ResourceVariant.of(this, holder);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ResourceType<?> other && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "ResourceType[" + this.id + "]";
    }
}
