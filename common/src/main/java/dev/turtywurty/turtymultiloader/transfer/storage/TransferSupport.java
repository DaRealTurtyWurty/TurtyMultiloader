package dev.turtywurty.turtymultiloader.transfer.storage;

public enum TransferSupport {
    NONE(false, false),
    INSERT_ONLY(true, false),
    EXTRACT_ONLY(false, true),
    BOTH(true, true);

    private final boolean insertion;
    private final boolean extraction;

    TransferSupport(boolean insertion, boolean extraction) {
        this.insertion = insertion;
        this.extraction = extraction;
    }

    public boolean supportsInsertion() {
        return this.insertion;
    }

    public boolean supportsExtraction() {
        return this.extraction;
    }

    public TransferSupport and(TransferSupport other) {
        if (this.insertion && other.insertion)
            return this.extraction && other.extraction ? BOTH : INSERT_ONLY;
        return this.extraction && other.extraction ? EXTRACT_ONLY : NONE;
    }
}
