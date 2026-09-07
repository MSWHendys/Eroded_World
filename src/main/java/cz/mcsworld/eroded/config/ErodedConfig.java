package cz.mcsworld.eroded.config;

/**
 * Minimal contract for Eroded World JSON configurations.
 *
 * <p>The project intentionally owns this interface instead of depending on a
 * third-party configuration library. Implementations keep their existing
 * post-load semantic validation.</p>
 */
public interface ErodedConfig {
    void validatePostLoad() throws ConfigValidationException;
}
