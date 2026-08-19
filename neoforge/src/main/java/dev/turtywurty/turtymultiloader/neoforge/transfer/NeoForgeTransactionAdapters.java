package dev.turtywurty.turtymultiloader.neoforge.transfer;

import dev.turtywurty.turtymultiloader.transfer.transaction.TransactionParticipant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferResult;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransactionScope;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Bridges NeoForge and neutral transaction contexts in both directions.
 */
public final class NeoForgeTransactionAdapters {
    private static final ThreadLocal<Map<TransferContext, Transaction>>
        NATIVE_BY_NEUTRAL = ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<Map<TransactionContext, NeoForgeContext>>
        NEUTRAL_BY_NATIVE = ThreadLocal.withInitial(IdentityHashMap::new);

    private NeoForgeTransactionAdapters() {
    }

    public static TransactionContext toNeoForge(TransferContext neutral) {
        Map<TransferContext, Transaction> map = NATIVE_BY_NEUTRAL.get();
        return map.computeIfAbsent(neutral, context -> {
            Transaction nativeTransaction = context.parent()
                .map(parent -> Transaction.open(toNeoForge(parent)))
                .orElseGet(Transaction::openRoot);
            context.addCloseCallback(result -> {
                try {
                    if (result == TransferResult.COMMITTED)
                        nativeTransaction.commit();
                    else
                        nativeTransaction.close();
                } finally {
                    map.remove(context);
                    if (map.isEmpty())
                        NATIVE_BY_NEUTRAL.remove();
                }
            });
            return nativeTransaction;
        });
    }

    public static TransferContext fromNeoForge(
        TransactionContext neoforge
    ) {
        Objects.requireNonNull(neoforge, "neoforge");
        Map<TransactionContext, NeoForgeContext> map = NEUTRAL_BY_NATIVE.get();
        NeoForgeContext existing = map.get(neoforge);
        if (existing != null)
            return existing;

        NeoForgeContext result = new NeoForgeContext(neoforge, null, new IdentityHashMap<>());
        map.put(neoforge, result);
        try {
            ContextCleanupJournal cleanup = new ContextCleanupJournal(() -> {
                map.remove(neoforge);
                if (map.isEmpty())
                    NEUTRAL_BY_NATIVE.remove();
            });
            cleanup.updateSnapshots(neoforge);
        } catch (RuntimeException exception) {
            map.remove(neoforge);
            if (map.isEmpty())
                NEUTRAL_BY_NATIVE.remove();
            throw exception;
        }
        return result;
    }

    private static final class ParticipantBridge extends SnapshotJournal<Object> {
        private final TransactionParticipant<?> participant;

        private ParticipantBridge(TransactionParticipant<?> participant) {
            this.participant = participant;
        }

        @Override
        protected Object createSnapshot() {
            return this.participant.createSnapshotForTransaction();
        }

        @Override
        protected void revertToSnapshot(Object snapshot) {
            this.participant.restoreSnapshotForTransaction(snapshot);
        }

        @Override
        protected void releaseSnapshot(Object snapshot) {
            this.participant.releaseSnapshotForTransaction(snapshot);
        }

        @Override
        protected void onRootCommit(Object originalState) {
            this.participant.finalCommitForTransaction(originalState);
        }
    }

    private static final class CallbackJournal extends SnapshotJournal<Boolean> {
        private final Consumer<TransferResult> callback;

        private CallbackJournal(Consumer<TransferResult> callback) {
            this.callback = callback;
        }

        @Override
        protected Boolean createSnapshot() {
            return Boolean.TRUE;
        }

        @Override
        protected void revertToSnapshot(Boolean snapshot) {
            this.callback.accept(TransferResult.ROLLED_BACK);
        }

        @Override
        protected void onRootCommit(Boolean originalState) {
            this.callback.accept(TransferResult.COMMITTED);
        }
    }

    private static final class ContextCleanupJournal extends SnapshotJournal<Boolean> {
        private final Runnable cleanup;

        private ContextCleanupJournal(Runnable cleanup) {
            this.cleanup = cleanup;
        }

        @Override
        protected Boolean createSnapshot() {
            return Boolean.TRUE;
        }

        @Override
        protected void revertToSnapshot(Boolean snapshot) {
            this.cleanup.run();
        }

        @Override
        protected void onRootCommit(Boolean originalState) {
            this.cleanup.run();
        }
    }

    private static class NeoForgeContext implements TransferContext {
        protected final TransactionContext neoforge;
        private final TransferContext parent;
        private final Map<TransactionParticipant<?>, ParticipantBridge> participants;

        private NeoForgeContext(
            TransactionContext neoforge,
            TransferContext parent,
            Map<TransactionParticipant<?>, ParticipantBridge> participants
        ) {
            this.neoforge = neoforge;
            this.parent = parent;
            this.participants = participants;
        }

        @Override
        public int depth() {
            return this.neoforge.depth();
        }

        @Override
        public Optional<? extends TransferContext> parent() {
            return Optional.ofNullable(this.parent);
        }

        @Override
        public TransferTransactionScope openNested() {
            return new NeoForgeScope(Transaction.open(this.neoforge), this, this.participants);
        }

        @Override
        public void addCloseCallback(Consumer<TransferResult> callback) {
            CallbackJournal journal = new CallbackJournal(callback);
            journal.updateSnapshots(this.neoforge);
        }

        @Override
        public void addCommitCallback(Runnable callback) {
            addCloseCallback(result -> {
                if (result == TransferResult.COMMITTED)
                    callback.run();
            });
        }

        @Override
        public void enlist(TransactionParticipant<?> participant) {
            this.participants.computeIfAbsent(participant, ParticipantBridge::new).updateSnapshots(this.neoforge);
        }
    }

    private static final class NeoForgeScope extends NeoForgeContext implements TransferTransactionScope {
        private final Transaction transaction;
        private boolean closed;

        private NeoForgeScope(
            Transaction transaction,
            TransferContext parent,
            Map<TransactionParticipant<?>, ParticipantBridge> participants
        ) {
            super(transaction, parent, participants);
            this.transaction = transaction;
        }

        @Override
        public void commit() {
            if (!this.closed) {
                this.transaction.commit();
                this.closed = true;
            }
        }

        @Override
        public void rollback() {
            close();
        }

        @Override
        public void close() {
            if (!this.closed) {
                this.transaction.close();
                this.closed = true;
            }
        }
    }
}
