package dev.turtywurty.turtymultiloader.fabric.transfer;

import dev.turtywurty.turtymultiloader.transfer.transaction.TransactionParticipant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferResult;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransactionScope;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Bridges Fabric and neutral transaction contexts in both directions.
 */
public final class FabricTransactionAdapters {
    private static final ThreadLocal<Map<TransferContext, Transaction>>
        NATIVE_BY_NEUTRAL = ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<Map<TransactionContext, FabricContext>>
        NEUTRAL_BY_NATIVE = ThreadLocal.withInitial(IdentityHashMap::new);

    private FabricTransactionAdapters() {
    }

    public static TransactionContext toFabric(TransferContext neutral) {
        Objects.requireNonNull(neutral, "neutral");
        Map<TransferContext, Transaction> map = NATIVE_BY_NEUTRAL.get();
        return map.computeIfAbsent(neutral, context -> {
            Transaction nativeTransaction = context.parent()
                .map(parent -> ((Transaction) toFabric(parent)).openNested())
                .orElseGet(Transaction::openOuter);
            context.addCloseCallback(result -> {
                try {
                    if (result == TransferResult.COMMITTED)
                        nativeTransaction.commit();
                } finally {
                    nativeTransaction.close();
                    map.remove(context);
                    if (map.isEmpty())
                        NATIVE_BY_NEUTRAL.remove();
                }
            });
            return nativeTransaction;
        });
    }

    public static TransferContext fromFabric(
        TransactionContext fabric
    ) {
        Objects.requireNonNull(fabric, "fabric");
        Map<TransactionContext, FabricContext> map = NEUTRAL_BY_NATIVE.get();
        FabricContext existing = map.get(fabric);
        if (existing != null)
            return existing;

        Map<TransactionParticipant<?>, ParticipantBridge> participants;
        int depth = fabric.nestingDepth();
        if (depth == 0) {
            participants = new IdentityHashMap<>();
        } else {
            TransactionContext parent = fabric.getOpenTransaction(depth - 1);
            participants = ((FabricContext) fromFabric(parent)).participants;
        }

        FabricContext result = new FabricContext(fabric, participants);
        map.put(fabric, result);
        fabric.addCloseCallback((transaction, closeResult) -> {
            map.remove(fabric);
            if (map.isEmpty())
                NEUTRAL_BY_NATIVE.remove();
        });
        return result;
    }

    private static final class ParticipantBridge
        extends SnapshotParticipant<Object> {
        private final TransactionParticipant<?> participant;
        private Object finalSnapshot;

        private ParticipantBridge(TransactionParticipant<?> participant) {
            this.participant = participant;
        }

        @Override
        protected Object createSnapshot() {
            return this.participant.createSnapshotForTransaction();
        }

        @Override
        protected void readSnapshot(Object snapshot) {
            this.participant.restoreSnapshotForTransaction(snapshot);
        }

        @Override
        protected void releaseSnapshot(Object snapshot) {
            this.finalSnapshot = snapshot;
            this.participant.releaseSnapshotForTransaction(snapshot);
        }

        @Override
        protected void onFinalCommit() {
            this.participant.finalCommitForTransaction(this.finalSnapshot);
            this.finalSnapshot = null;
        }
    }

    private static class FabricContext implements TransferContext {
        protected final TransactionContext fabric;
        private final Map<TransactionParticipant<?>, ParticipantBridge> participants;

        private FabricContext(
            TransactionContext fabric,
            Map<TransactionParticipant<?>, ParticipantBridge> participants
        ) {
            this.fabric = fabric;
            this.participants = participants;
        }

        @Override
        public int depth() {
            return this.fabric.nestingDepth();
        }

        @Override
        public Optional<? extends TransferContext> parent() {
            if (depth() == 0)
                return Optional.empty();
            return Optional.of(fromFabric(this.fabric.getOpenTransaction(depth() - 1)));
        }

        @Override
        public TransferTransactionScope openNested() {
            return new FabricScope(this.fabric.openNested(), this.participants);
        }

        @Override
        public void addCloseCallback(Consumer<TransferResult> callback) {
            this.fabric.addCloseCallback((transaction, result) -> callback.accept(
                result.wasCommitted() ? TransferResult.COMMITTED : TransferResult.ROLLED_BACK
            ));
        }

        @Override
        public void addCommitCallback(Runnable callback) {
            this.fabric.addOuterCloseCallback(result -> {
                if (result.wasCommitted())
                    callback.run();
            });
        }

        @Override
        public void enlist(TransactionParticipant<?> participant) {
            this.participants.computeIfAbsent(participant, ParticipantBridge::new).updateSnapshots(this.fabric);
        }
    }

    private static final class FabricScope extends FabricContext implements TransferTransactionScope {
        private final Transaction transaction;

        private FabricScope(
            Transaction transaction,
            Map<TransactionParticipant<?>, ParticipantBridge> participants
        ) {
            super(transaction, participants);
            this.transaction = transaction;
        }

        @Override
        public void commit() {
            this.transaction.commit();
        }

        @Override
        public void rollback() {
            this.transaction.abort();
        }

        @Override
        public void close() {
            this.transaction.close();
        }
    }
}
