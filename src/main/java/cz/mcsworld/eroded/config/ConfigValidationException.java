package cz.mcsworld.eroded.config;

/**
 * Checked exception used when an Eroded World configuration contains an
 * unsafe or internally inconsistent value.
 */
public final class ConfigValidationException extends Exception {
    public ConfigValidationException(String message) {
        super(message);
    }

    public ConfigValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
