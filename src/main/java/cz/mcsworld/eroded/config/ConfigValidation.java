package cz.mcsworld.eroded.config;

/**
 * Small validation helpers used by all Eroded World config files.
 * Validation is intentionally fail-fast: a bad config is rejected instead of
 * being silently clamped and written back over the administrator's file.
 */
public final class ConfigValidation {

    private ConfigValidation() {
    }

    public static void require(boolean condition, String path, String rule)
            throws ConfigValidationException {
        if (!condition) {
            throw new ConfigValidationException(path + " " + rule);
        }
    }

    public static void notNull(Object value, String path)
            throws ConfigValidationException {
        require(value != null, path, "must not be null");
    }

    public static void finite(double value, String path)
            throws ConfigValidationException {
        require(Double.isFinite(value), path, "must be a finite number");
    }

    public static void finite(float value, String path)
            throws ConfigValidationException {
        require(Float.isFinite(value), path, "must be a finite number");
    }

    public static void min(int value, int min, String path)
            throws ConfigValidationException {
        require(value >= min, path, "must be >= " + min + " (was " + value + ")");
    }

    public static void min(long value, long min, String path)
            throws ConfigValidationException {
        require(value >= min, path, "must be >= " + min + " (was " + value + ")");
    }

    public static void min(double value, double min, String path)
            throws ConfigValidationException {
        finite(value, path);
        require(value >= min, path, "must be >= " + min + " (was " + value + ")");
    }

    public static void min(float value, float min, String path)
            throws ConfigValidationException {
        finite(value, path);
        require(value >= min, path, "must be >= " + min + " (was " + value + ")");
    }

    public static void range(int value, int min, int max, String path)
            throws ConfigValidationException {
        require(value >= min && value <= max, path,
                "must be in [" + min + ", " + max + "] (was " + value + ")");
    }

    public static void range(double value, double min, double max, String path)
            throws ConfigValidationException {
        finite(value, path);
        require(value >= min && value <= max, path,
                "must be in [" + min + ", " + max + "] (was " + value + ")");
    }

    public static void range(float value, float min, float max, String path)
            throws ConfigValidationException {
        finite(value, path);
        require(value >= min && value <= max, path,
                "must be in [" + min + ", " + max + "] (was " + value + ")");
    }
}
