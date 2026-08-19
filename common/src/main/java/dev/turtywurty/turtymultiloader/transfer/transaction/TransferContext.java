package dev.turtywurty.turtymultiloader.transfer.transaction;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Loader-neutral context accepted by every mutating storage operation.
 */
public interface TransferContext {
    int depth();

    Optional<? extends TransferContext> parent();

    TransferTransactionScope openNested();

    void addCloseCallback(Consumer<TransferResult> callback);

    /**
     * Runs only after the root transaction commits; nested registrations follow their parent.
     */
    void addCommitCallback(Runnable callback);

    /**
     * Enlists a snapshot participant before it is changed. Adapters use this to bridge native transactions.
     */
    void enlist(TransactionParticipant<?> participant);

    default <R> R simulate(Function<? super TransferContext, ? extends R> action) {
        try (TransferTransactionScope transaction = openNested()) {
            return action.apply(transaction);
        }
    }
}
