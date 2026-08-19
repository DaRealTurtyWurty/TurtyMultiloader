package dev.turtywurty.turtymultiloader.transfer.transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Root/nested transaction. Closing without committing rolls all enlisted state back.
 */
public final class TransferTransaction implements TransferTransactionScope {
    private static final ThreadLocal<TransferTransaction> CURRENT = new ThreadLocal<>();

    private final TransferTransaction parent;
    private final Thread owner = Thread.currentThread();
    private final Map<TransactionParticipant<?>, Object> snapshots = new LinkedHashMap<>();
    private final List<Consumer<TransferResult>> closeCallbacks = new ArrayList<>();
    private final List<Runnable> commitCallbacks = new ArrayList<>();
    private boolean committed;
    private boolean closed;

    private TransferTransaction(TransferTransaction parent) {
        this.parent = parent;
        CURRENT.set(this);
    }

    public static TransferTransaction openRoot() {
        if (CURRENT.get() != null)
            throw new IllegalStateException("A root transaction cannot be opened inside another transaction");
        return new TransferTransaction(null);
    }

    public static Optional<TransferTransaction> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static <R> R runSimulation(Function<? super TransferContext, ? extends R> action) {
        Objects.requireNonNull(action, "action");
        TransferTransaction current = CURRENT.get();
        try (TransferTransaction transaction = current == null ? openRoot() : current.openNested()) {
            return action.apply(transaction);
        }
    }

    @Override
    public int depth() {
        checkThread();
        return this.parent == null ? 0 : this.parent.depth() + 1;
    }

    @Override
    public Optional<TransferTransaction> parent() {
        return Optional.ofNullable(this.parent);
    }

    @Override
    public TransferTransaction openNested() {
        checkOpenAndCurrent();
        return new TransferTransaction(this);
    }

    public void commit() {
        checkOpenAndCurrent();
        this.committed = true;
    }

    public void rollback() {
        checkOpenAndCurrent();
        close();
    }

    @Override
    public void addCloseCallback(Consumer<TransferResult> callback) {
        checkOpen();
        this.closeCallbacks.add(Objects.requireNonNull(callback, "callback"));
    }

    @Override
    public void addCommitCallback(Runnable callback) {
        checkOpen();
        this.commitCallbacks.add(Objects.requireNonNull(callback, "callback"));
    }

    @Override
    public void enlist(TransactionParticipant<?> participant) {
        checkOpenAndCurrent();
        this.snapshots.computeIfAbsent(
            Objects.requireNonNull(participant, "participant"),
            TransactionParticipant::createSnapshotForTransaction
        );
    }

    @Override
    public void close() {
        if (this.closed)
            return;
        checkOpenAndCurrent();

        TransferResult result = this.committed ? TransferResult.COMMITTED : TransferResult.ROLLED_BACK;
        List<Map.Entry<TransactionParticipant<?>, Object>> entries = new ArrayList<>(this.snapshots.entrySet());
        RuntimeException failure = null;
        try {
            if (this.committed)
                commitSnapshots(entries);
            else
                rollbackSnapshots(entries);
        } catch (RuntimeException exception) {
            failure = exception;
        } finally {
            this.closed = true;
            CURRENT.set(this.parent);
        }

        List<Consumer<TransferResult>> callbacks = new ArrayList<>(this.closeCallbacks);
        Collections.reverse(callbacks);
        for (Consumer<TransferResult> callback : callbacks) {
            try {
                callback.accept(result);
            } catch (RuntimeException exception) {
                if (failure == null)
                    failure = exception;
                else
                    failure.addSuppressed(exception);
            }
        }
        if (failure != null)
            throw failure;
    }

    private void commitSnapshots(List<Map.Entry<TransactionParticipant<?>, Object>> entries) {
        if (this.parent != null) {
            for (Map.Entry<TransactionParticipant<?>, Object> entry : entries) {
                Object previous = this.parent.snapshots.putIfAbsent(entry.getKey(), entry.getValue());
                if (previous != null)
                    entry.getKey().releaseSnapshotForTransaction(entry.getValue());
            }
            this.parent.commitCallbacks.addAll(this.commitCallbacks);
            return;
        }

        for (Map.Entry<TransactionParticipant<?>, Object> entry : entries) {
            entry.getKey().finalCommitForTransaction(entry.getValue());
            entry.getKey().releaseSnapshotForTransaction(entry.getValue());
        }
        for (Runnable callback : this.commitCallbacks)
            callback.run();
    }

    private static void rollbackSnapshots(List<Map.Entry<TransactionParticipant<?>, Object>> entries) {
        Collections.reverse(entries);
        for (Map.Entry<TransactionParticipant<?>, Object> entry : entries) {
            entry.getKey().restoreSnapshotForTransaction(entry.getValue());
            entry.getKey().releaseSnapshotForTransaction(entry.getValue());
        }
    }

    private void checkThread() {
        if (Thread.currentThread() != this.owner)
            throw new IllegalStateException("Transactions may only be used on their opening thread");
    }

    private void checkOpen() {
        checkThread();
        if (this.closed)
            throw new IllegalStateException("TransferTransaction is closed");
    }

    private void checkOpenAndCurrent() {
        checkOpen();
        if (CURRENT.get() != this)
            throw new IllegalStateException("TransferTransaction is not the current transaction");
    }
}
