package dev.turtywurty.turtymultiloader.transfer.transaction;

/**
 * A closeable transaction level. Closing without commit rolls back.
 */
public interface TransferTransactionScope extends TransferContext, AutoCloseable {
    void commit();

    void rollback();

    @Override
    void close();
}
