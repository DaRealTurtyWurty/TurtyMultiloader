package dev.turtywurty.turtymultiloader.transfer.transaction;

/**
 * Base class for state that participates in neutral or loader-native transactions.
 */
public abstract class TransactionParticipant<S> {
    protected final void updateSnapshots(TransferContext transaction) {
        transaction.enlist(this);
    }

    protected abstract S createSnapshot();

    protected abstract void restoreSnapshot(S snapshot);

    protected void releaseSnapshot(S snapshot) {
    }

    protected void onFinalCommit(S originalState) {
    }

    public final Object createSnapshotForTransaction() {
        return createSnapshot();
    }

    @SuppressWarnings("unchecked")
    public final void restoreSnapshotForTransaction(Object snapshot) {
        restoreSnapshot((S) snapshot);
    }

    @SuppressWarnings("unchecked")
    public final void releaseSnapshotForTransaction(Object snapshot) {
        releaseSnapshot((S) snapshot);
    }

    @SuppressWarnings("unchecked")
    public final void finalCommitForTransaction(Object originalState) {
        onFinalCommit((S) originalState);
    }
}
