package dev.turtywurty.turtymultiloader.network;

import java.util.Objects;

/** Version and negotiation policy for one payload registration. */
public record PayloadRegistrationOptions(String protocolVersion, PayloadSupport support) {
    public static final PayloadRegistrationOptions DEFAULT = required("1");

    public PayloadRegistrationOptions {
        protocolVersion = Objects.requireNonNull(protocolVersion, "protocolVersion");
        support = Objects.requireNonNull(support, "support");
        if (protocolVersion.isBlank())
            throw new IllegalArgumentException("Protocol version cannot be blank");
    }

    public static PayloadRegistrationOptions required(String protocolVersion) {
        return new PayloadRegistrationOptions(protocolVersion, PayloadSupport.REQUIRED);
    }

    public static PayloadRegistrationOptions optional(String protocolVersion) {
        return new PayloadRegistrationOptions(protocolVersion, PayloadSupport.OPTIONAL);
    }
}
