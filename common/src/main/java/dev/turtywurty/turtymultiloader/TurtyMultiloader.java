package dev.turtywurty.turtymultiloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TurtyMultiloader {
    public static final String MOD_ID = "turtymultiloader";
    public static final String MOD_NAME = "TurtyMultiloader";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private TurtyMultiloader() {
    }

    public static void init() {
        LOGGER.info("Initializing {}", MOD_NAME);
    }
}
