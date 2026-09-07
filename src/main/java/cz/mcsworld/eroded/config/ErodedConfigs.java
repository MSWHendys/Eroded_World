package cz.mcsworld.eroded.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.config.combat.CombatConfig;
import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.config.loot.LootConfig;
import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Native JSON configuration manager for Eroded World.
 *
 * <p>The manager deliberately keeps configuration handling small and explicit:
 * strict JSON syntax is checked before Gson sees the file, every config runs
 * semantic validation after deserialization, writes are atomic, reload is
 * transactional, and invalid administrator files are preserved under
 * {@code .invalid} before recovery from {@code .last-good}.</p>
 */
public final class ErodedConfigs {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter INVALID_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private static boolean initialized;

    private ErodedConfigs() {
    }

    public static CombatConfig COMBAT;
    public static CraftingConfig CRAFTING;
    public static DarknessConfigs DARKNESS;
    public static DeathConfig DEATH;
    public static EnergyConfig ENERGY;
    public static TerritoryConfig TERRITORY;
    public static LootConfig LOOT;

    /**
     * Performs the first load. Missing files are created from validated defaults.
     */
    public static synchronized ReloadResult initialize() {
        if (initialized) {
            return reload();
        }
        ReloadResult result = reloadInternal(true);
        initialized = true;
        return result;
    }

    /**
     * Reloads every Eroded World config as one transaction.
     *
     * <p>If one edited file is invalid, valid edits in the other files are not
     * applied during a normal runtime reload. The malformed bytes are backed up
     * and that config is restored from its last known-good snapshot (or the
     * already active runtime config when no snapshot exists).</p>
     */
    public static synchronized ReloadResult reload() {
        if (!initialized) {
            return initialize();
        }
        return reloadInternal(false);
    }

    private static ReloadResult reloadInternal(boolean firstLoad) {
        CombatConfig oldCombat = COMBAT;
        CraftingConfig oldCrafting = CRAFTING;
        DarknessConfigs oldDarkness = DARKNESS;
        DeathConfig oldDeath = DEATH;
        EnergyConfig oldEnergy = ENERGY;
        TerritoryConfig oldTerritory = TERRITORY;
        LootConfig oldLoot = LOOT;

        LoadAttempt<CombatConfig> combat = loadCandidate(CombatConfig.class, "combat.json");
        LoadAttempt<CraftingConfig> crafting = loadCandidate(CraftingConfig.class, "crafting.json");
        LoadAttempt<DarknessConfigs> darkness = loadCandidate(DarknessConfigs.class, "darkness.json");
        LoadAttempt<DeathConfig> death = loadCandidate(DeathConfig.class, "death.json");
        LoadAttempt<EnergyConfig> energy = loadCandidate(EnergyConfig.class, "energy.json");
        LoadAttempt<TerritoryConfig> territory = loadCandidate(TerritoryConfig.class, "territory.json");
        LoadAttempt<LootConfig> loot = loadCandidate(LootConfig.class, "loot.json");

        List<ReloadFailure> failures = new ArrayList<>();
        addFailure(combat, failures);
        addFailure(crafting, failures);
        addFailure(darkness, failures);
        addFailure(death, failures);
        addFailure(energy, failures);
        addFailure(territory, failures);
        addFailure(loot, failures);

        if (!failures.isEmpty()) {
            String incident = INVALID_TIMESTAMP.format(LocalDateTime.now());
            List<ReloadFailure> recoveredFailures = new ArrayList<>();

            Recovery<CombatConfig> recoveredCombat = recoverIfNeeded(
                    CombatConfig.class, "combat.json", oldCombat, combat, incident, recoveredFailures);
            Recovery<CraftingConfig> recoveredCrafting = recoverIfNeeded(
                    CraftingConfig.class, "crafting.json", oldCrafting, crafting, incident, recoveredFailures);
            Recovery<DarknessConfigs> recoveredDarkness = recoverIfNeeded(
                    DarknessConfigs.class, "darkness.json", oldDarkness, darkness, incident, recoveredFailures);
            Recovery<DeathConfig> recoveredDeath = recoverIfNeeded(
                    DeathConfig.class, "death.json", oldDeath, death, incident, recoveredFailures);
            Recovery<EnergyConfig> recoveredEnergy = recoverIfNeeded(
                    EnergyConfig.class, "energy.json", oldEnergy, energy, incident, recoveredFailures);
            Recovery<TerritoryConfig> recoveredTerritory = recoverIfNeeded(
                    TerritoryConfig.class, "territory.json", oldTerritory, territory, incident, recoveredFailures);
            Recovery<LootConfig> recoveredLoot = recoverIfNeeded(
                    LootConfig.class, "loot.json", oldLoot, loot, incident, recoveredFailures);

            // During a normal reload, unrelated valid files remain on the old
            // runtime values (all-or-nothing). During the first startup there
            // are no old runtime objects, so valid files are allowed to become
            // active while only the bad file is recovered.
            assignActive(
                    chooseAfterFailedTransaction(firstLoad, oldCombat, combat, recoveredCombat),
                    chooseAfterFailedTransaction(firstLoad, oldCrafting, crafting, recoveredCrafting),
                    chooseAfterFailedTransaction(firstLoad, oldDarkness, darkness, recoveredDarkness),
                    chooseAfterFailedTransaction(firstLoad, oldDeath, death, recoveredDeath),
                    chooseAfterFailedTransaction(firstLoad, oldEnergy, energy, recoveredEnergy),
                    chooseAfterFailedTransaction(firstLoad, oldTerritory, territory, recoveredTerritory),
                    chooseAfterFailedTransaction(firstLoad, oldLoot, loot, recoveredLoot)
            );

            logFailures(recoveredFailures);
            return new ReloadResult(false, recoveredFailures);
        }

        CombatConfig newCombat = combat.config();
        CraftingConfig newCrafting = crafting.config();
        DarknessConfigs newDarkness = darkness.config();
        DeathConfig newDeath = death.config();
        EnergyConfig newEnergy = energy.config();
        TerritoryConfig newTerritory = territory.config();
        LootConfig newLoot = loot.config();

        // Keep old administrator values intact while making every newly introduced
        // config option visible on disk. Missing object keys are merged from the
        // current defaults; existing keys (including unknown legacy keys) always win.
        boolean lootEntriesChanged = newLoot.ensureDefaultEntries();
        if (lootEntriesChanged) {
            try {
                newLoot.validatePostLoad();
                appendMissingLootEntries(loot.mergedJson(), newLoot);
            } catch (RuntimeException | ConfigValidationException e) {
                ReloadFailure failure = new ReloadFailure(
                        "loot.json",
                        "migraci loot configu nelze připravit: " + conciseReason(e),
                        null,
                        false,
                        false
                );
                logFailures(List.of(failure));
                return new ReloadResult(false, List.of(failure));
            }
        }

        persistMigration("combat.json", combat, false);
        persistMigration("crafting.json", crafting, false);
        persistMigration("darkness.json", darkness, false);
        persistMigration("death.json", death, false);
        persistMigration("energy.json", energy, false);
        persistMigration("territory.json", territory, false);
        persistMigration("loot.json", loot, lootEntriesChanged);

        assignActive(newCombat, newCrafting, newDarkness, newDeath,
                newEnergy, newTerritory, newLoot);

        snapshotLastGood();

        ErodedMod.LOGGER.info("[Eroded World] Configuration reload OK.");
        return new ReloadResult(true, List.of());
    }

    /** Saves the currently active Energy config using an atomic strict-JSON write. */
    public static synchronized boolean saveEnergy() {
        return saveActive("energy.json", ENERGY, EnergyConfig.class);
    }

    /** Saves the currently active Darkness config using an atomic strict-JSON write. */
    public static synchronized boolean saveDarkness() {
        return saveActive("darkness.json", DARKNESS, DarknessConfigs.class);
    }

    private static <T extends ErodedConfig> boolean saveActive(
            String name,
            T config,
            Class<T> configClass
    ) {
        if (config == null) {
            ErodedMod.LOGGER.error("[Eroded World] Cannot save {} before configs are initialized.", name);
            return false;
        }

        try {
            config.validatePostLoad();
            writeConfigPreservingUnknown(name, config);
            snapshotOne(name, configClass);
            return true;
        } catch (IOException | RuntimeException | ConfigValidationException e) {
            ErodedMod.LOGGER.error("[Eroded World] Could not save {}.", name, e);
            return false;
        }
    }

    private static <T extends ErodedConfig> LoadAttempt<T> loadCandidate(
            Class<T> configClass,
            String name
    ) {
        Path file = configPath(name);

        try {
            if (!Files.exists(file)) {
                T defaults = createValidatedDefaults(configClass);
                writeConfig(name, defaults);
                return new LoadAttempt<>(defaults, null, null, List.of());
            }

            String raw = Files.readString(file, StandardCharsets.UTF_8);
            StrictJsonSyntax.validate(raw);

            JsonElement parsed = JsonParser.parseString(raw);
            if (!parsed.isJsonObject()) {
                throw new JsonParseException("soubor neobsahuje objekt konfigurace");
            }

            JsonObject merged = parsed.getAsJsonObject();
            T defaults = createValidatedDefaults(configClass);
            JsonObject defaultJson = GSON.toJsonTree(defaults).getAsJsonObject();
            List<String> addedPaths = new ArrayList<>();
            mergeMissingKeys(merged, defaultJson, "", addedPaths);

            T config = GSON.fromJson(merged, configClass);
            if (config == null) {
                throw new JsonParseException("soubor neobsahuje objekt konfigurace");
            }
            config.validatePostLoad();

            return new LoadAttempt<>(
                    config,
                    null,
                    merged,
                    Collections.unmodifiableList(new ArrayList<>(addedPaths))
            );
        } catch (StrictJsonSyntax.SyntaxException e) {
            return failed(name, "neplatná JSON syntaxe: " + conciseReason(e));
        } catch (JsonParseException e) {
            return failed(name, "JSON nelze převést na konfiguraci: " + conciseReason(e));
        } catch (ConfigValidationException e) {
            return failed(name, "neplatná hodnota: " + conciseReason(e));
        } catch (IOException e) {
            return failed(name, "soubor nelze přečíst nebo vytvořit: " + conciseReason(e));
        } catch (RuntimeException e) {
            ErodedMod.LOGGER.error("[Eroded World] Config load failed unexpectedly for {}.", name, e);
            return failed(name, "chyba při načtení: " + conciseReason(e));
        }
    }

    private static <T extends ErodedConfig> LoadAttempt<T> failed(String name, String reason) {
        return new LoadAttempt<>(null, new ReloadFailure(
                name, reason, null, false, false
        ), null, List.of());
    }

    private static <T extends ErodedConfig> void addFailure(
            LoadAttempt<T> attempt,
            List<ReloadFailure> failures
    ) {
        if (attempt.failure() != null) {
            failures.add(attempt.failure());
        }
    }

    private static <T extends ErodedConfig> Recovery<T> recoverIfNeeded(
            Class<T> configClass,
            String name,
            T activeConfig,
            LoadAttempt<T> attempt,
            String incident,
            List<ReloadFailure> failures
    ) {
        if (attempt.failure() == null) {
            return new Recovery<>(attempt.config(), null);
        }

        Recovery<T> recovery = recoverInvalid(
                configClass, name, activeConfig, attempt.failure(), incident
        );
        failures.add(recovery.failure());
        return recovery;
    }

    private static <T extends ErodedConfig> T chooseAfterFailedTransaction(
            boolean firstLoad,
            T oldConfig,
            LoadAttempt<T> attempt,
            Recovery<T> recovery
    ) {
        if (attempt.failure() != null) {
            // recoverInvalid always supplies a validated runtime fallback, even
            // when it could not repair the on-disk file.
            return recovery.config();
        }

        if (firstLoad || oldConfig == null) {
            return attempt.config();
        }
        return oldConfig;
    }

    private static void mergeMissingKeys(
            JsonObject existing,
            JsonObject defaults,
            String prefix,
            List<String> addedPaths
    ) {
        for (var entry : defaults.entrySet()) {
            String key = entry.getKey();
            JsonElement defaultValue = entry.getValue();
            String path = prefix.isEmpty() ? key : prefix + "." + key;

            if (!existing.has(key)) {
                existing.add(key, defaultValue.deepCopy());
                addedPaths.add(path);
                continue;
            }

            JsonElement currentValue = existing.get(key);
            if (currentValue != null
                    && currentValue.isJsonObject()
                    && defaultValue != null
                    && defaultValue.isJsonObject()) {
                mergeMissingKeys(
                        currentValue.getAsJsonObject(),
                        defaultValue.getAsJsonObject(),
                        path,
                        addedPaths
                );
            }
        }
    }

    private static void appendMissingLootEntries(JsonObject mergedJson, LootConfig config) {
        if (mergedJson == null) {
            return;
        }

        JsonElement lootElement = mergedJson.get("loot");
        if (lootElement == null || !lootElement.isJsonArray()) {
            return;
        }

        JsonArray lootArray = lootElement.getAsJsonArray();
        for (var entry : config.loot) {
            boolean found = false;
            for (JsonElement element : lootArray) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonElement item = element.getAsJsonObject().get("item");
                if (item != null
                        && item.isJsonPrimitive()
                        && item.getAsString().equalsIgnoreCase(entry.item)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                lootArray.add(GSON.toJsonTree(entry));
            }
        }
    }

    private static <T extends ErodedConfig> void persistMigration(
            String name,
            LoadAttempt<T> attempt,
            boolean forceWrite
    ) {
        if (attempt.mergedJson() == null
                || (!forceWrite && attempt.addedPaths().isEmpty())) {
            return;
        }

        try {
            writeJson(name, attempt.mergedJson());
            if (!attempt.addedPaths().isEmpty()) {
                ErodedMod.LOGGER.info(
                        "[Eroded World] Updated {} with {} missing config option(s); existing values were preserved.",
                        name,
                        attempt.addedPaths().size()
                );
                ErodedMod.LOGGER.debug(
                        "[Eroded World] Added config paths to {}: {}",
                        name,
                        String.join(", ", attempt.addedPaths())
                );
            } else {
                ErodedMod.LOGGER.info(
                        "[Eroded World] Updated {} with missing default entries; existing values were preserved.",
                        name
                );
            }
        } catch (IOException | RuntimeException e) {
            // The in-memory config is already valid and can safely be used. Keep
            // the server running and retry the additive migration next reload.
            ErodedMod.LOGGER.error(
                    "[Eroded World] Could not persist additive config migration for {}. "
                            + "Existing settings were left untouched and the migration will be retried later.",
                    name,
                    e
            );
        }
    }

    private static <T extends ErodedConfig> T parseAndValidate(
            Path file,
            Class<T> configClass
    ) throws IOException, ConfigValidationException {
        String raw = Files.readString(file, StandardCharsets.UTF_8);

        // Validate strict JSON before Gson. This prevents typo tokens such as
        // `tru` or `fajse` from being interpreted leniently as boolean values.
        StrictJsonSyntax.validate(raw);

        T config = GSON.fromJson(raw, configClass);
        if (config == null) {
            throw new JsonParseException("soubor neobsahuje objekt konfigurace");
        }
        config.validatePostLoad();
        return config;
    }

    private static <T extends ErodedConfig> boolean isStrictlyValid(
            Path file,
            Class<T> configClass
    ) {
        try {
            parseAndValidate(file, configClass);
            return true;
        } catch (IOException | RuntimeException | ConfigValidationException e) {
            ErodedMod.LOGGER.warn(
                    "[Eroded World] Refusing invalid recovery snapshot {}: {}",
                    file,
                    conciseReason(e)
            );
            return false;
        }
    }

    private static <T extends ErodedConfig> T createValidatedDefaults(Class<T> configClass) {
        try {
            T defaults = configClass.getDeclaredConstructor().newInstance();
            defaults.validatePostLoad();
            return defaults;
        } catch (ReflectiveOperationException | ConfigValidationException e) {
            throw new IllegalStateException(
                    "nelze vytvořit bezpečný default pro " + configClass.getSimpleName(),
                    e
            );
        }
    }

    private static <T extends ErodedConfig> Recovery<T> recoverInvalid(
            Class<T> configClass,
            String name,
            T activeConfig,
            ReloadFailure original,
            String incident
    ) {
        Path file = configPath(name);
        Path invalidCopy = invalidDir().resolve(incident).resolve(name);
        Path lastGood = lastGoodDir().resolve(name);
        String backupDisplay = null;
        boolean restoredFromSnapshot = false;

        // Never overwrite malformed bytes until a diagnostic copy exists.
        try {
            if (Files.exists(file)) {
                Files.createDirectories(invalidCopy.getParent());
                Files.copy(file, invalidCopy, StandardCopyOption.REPLACE_EXISTING);
                backupDisplay = configDir().relativize(invalidCopy).toString().replace('\\', '/');
            }
        } catch (IOException e) {
            ErodedMod.LOGGER.error(
                    "[Eroded World] Could not back up invalid {}. The original file will NOT be overwritten.",
                    name,
                    e
            );
            T runtimeFallback = activeConfig != null
                    ? activeConfig
                    : createValidatedDefaults(configClass);
            return new Recovery<>(runtimeFallback, new ReloadFailure(
                    name,
                    original.reason() + "; záloha selhala: " + conciseReason(e),
                    null,
                    false,
                    false
            ));
        }

        try {
            T recovered;
            if (Files.exists(lastGood) && isStrictlyValid(lastGood, configClass)) {
                Files.copy(lastGood, file, StandardCopyOption.REPLACE_EXISTING);
                recovered = parseAndValidate(file, configClass);
                restoredFromSnapshot = true;
            } else {
                // Never trust an invalid old snapshot. Preserve it separately
                // and recover from the current validated runtime object; during
                // first startup, use a clean validated default instead.
                if (Files.exists(lastGood)) {
                    Path badSnapshot = invalidDir().resolve(incident)
                            .resolve("last-good-" + name);
                    Files.createDirectories(badSnapshot.getParent());
                    Files.copy(lastGood, badSnapshot, StandardCopyOption.REPLACE_EXISTING);
                }

                recovered = activeConfig != null
                        ? activeConfig
                        : createValidatedDefaults(configClass);
                recovered.validatePostLoad();
                writeConfig(name, recovered);
                recovered = parseAndValidate(file, configClass);
            }

            return new Recovery<>(recovered, new ReloadFailure(
                    name,
                    original.reason(),
                    backupDisplay,
                    true,
                    restoredFromSnapshot
            ));
        } catch (IOException | RuntimeException | ConfigValidationException e) {
            ErodedMod.LOGGER.error(
                    "[Eroded World] Invalid {} was backed up, but restoring the working config failed.",
                    name,
                    e
            );
            T runtimeFallback = activeConfig != null
                    ? activeConfig
                    : createValidatedDefaults(configClass);
            return new Recovery<>(runtimeFallback, new ReloadFailure(
                    name,
                    original.reason() + "; obnova platné verze selhala: " + conciseReason(e),
                    backupDisplay,
                    false,
                    restoredFromSnapshot
            ));
        }
    }

    private static void writeConfig(String name, ErodedConfig config) throws IOException {
        writeJson(name, GSON.toJsonTree(config));
    }

    private static void writeConfigPreservingUnknown(String name, ErodedConfig config) throws IOException {
        JsonElement desiredElement = GSON.toJsonTree(config);
        if (!desiredElement.isJsonObject()) {
            writeJson(name, desiredElement);
            return;
        }

        JsonObject desired = desiredElement.getAsJsonObject();
        Path existingPath = configPath(name);
        if (Files.exists(existingPath)) {
            String raw = Files.readString(existingPath, StandardCharsets.UTF_8);
            StrictJsonSyntax.validate(raw);
            JsonElement existingElement = JsonParser.parseString(raw);
            if (existingElement.isJsonObject()) {
                preserveUnknownKeys(desired, existingElement.getAsJsonObject());
            }
        }

        writeJson(name, desired);
    }

    private static void preserveUnknownKeys(JsonObject desired, JsonObject existing) {
        for (var entry : existing.entrySet()) {
            String key = entry.getKey();
            JsonElement oldValue = entry.getValue();

            if (!desired.has(key)) {
                desired.add(key, oldValue.deepCopy());
                continue;
            }

            JsonElement newValue = desired.get(key);
            if (newValue != null
                    && newValue.isJsonObject()
                    && oldValue != null
                    && oldValue.isJsonObject()) {
                preserveUnknownKeys(newValue.getAsJsonObject(), oldValue.getAsJsonObject());
            }
        }
    }

    private static void writeJson(String name, JsonElement jsonTree) throws IOException {
        Path path = configPath(name);
        Path parent = path.getParent();
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");

        if (parent != null) {
            Files.createDirectories(parent);
        }

        String json = GSON.toJson(jsonTree);
        StrictJsonSyntax.validate(json);

        try {
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, path,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // Keep the primary write error.
            }
            throw e;
        }
    }

    private static void snapshotLastGood() {
        try {
            Files.createDirectories(lastGoodDir());
        } catch (IOException e) {
            ErodedMod.LOGGER.error("[Eroded World] Could not create .last-good config directory.", e);
            return;
        }

        snapshotOne("combat.json", CombatConfig.class);
        snapshotOne("crafting.json", CraftingConfig.class);
        snapshotOne("darkness.json", DarknessConfigs.class);
        snapshotOne("death.json", DeathConfig.class);
        snapshotOne("energy.json", EnergyConfig.class);
        snapshotOne("territory.json", TerritoryConfig.class);
        snapshotOne("loot.json", LootConfig.class);
    }

    private static <T extends ErodedConfig> void snapshotOne(String name, Class<T> configClass) {
        Path source = configPath(name);
        Path target = lastGoodDir().resolve(name);
        if (!Files.exists(source)) {
            return;
        }

        if (!isStrictlyValid(source, configClass)) {
            ErodedMod.LOGGER.error(
                    "[Eroded World] NOT updating last-good snapshot for {} because the active file failed strict validation.",
                    name
            );
            return;
        }

        try {
            Files.createDirectories(lastGoodDir());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            ErodedMod.LOGGER.error(
                    "[Eroded World] Could not update last-good snapshot for {}.",
                    name,
                    e
            );
        }
    }

    private static void logFailures(List<ReloadFailure> failures) {
        for (ReloadFailure failure : failures) {
            ErodedMod.LOGGER.error(
                    "[Eroded World] Configuration error in {}. The file was restored to the previous valid values.",
                    failure.configName()
            );
            ErodedMod.LOGGER.debug(
                    "[Eroded World] Reload detail for {}: reason={} | restored={} | backup={}",
                    failure.configName(),
                    failure.reason(),
                    failure.restored(),
                    failure.backupPath() == null ? "none" : failure.backupPath()
            );
        }
    }

    private static String conciseReason(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static Path configDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("ErodedWorld");
    }

    private static Path configPath(String name) {
        return configDir().resolve(name);
    }

    private static Path lastGoodDir() {
        return configDir().resolve(".last-good");
    }

    private static Path invalidDir() {
        return configDir().resolve(".invalid");
    }

    private static void assignActive(
            CombatConfig combat,
            CraftingConfig crafting,
            DarknessConfigs darkness,
            DeathConfig death,
            EnergyConfig energy,
            TerritoryConfig territory,
            LootConfig loot
    ) {
        COMBAT = combat;
        CRAFTING = crafting;
        DARKNESS = darkness;
        DEATH = death;
        ENERGY = energy;
        TERRITORY = territory;
        LOOT = loot;
    }

    private record LoadAttempt<T extends ErodedConfig>(
            T config,
            ReloadFailure failure,
            JsonObject mergedJson,
            List<String> addedPaths
    ) {
    }

    private record Recovery<T extends ErodedConfig>(T config, ReloadFailure failure) {
    }

    public record ReloadFailure(
            String configName,
            String reason,
            String backupPath,
            boolean restored,
            boolean restoredFromSnapshot
    ) {
    }

    public record ReloadResult(boolean success, List<ReloadFailure> failures) {
        public ReloadResult {
            failures = Collections.unmodifiableList(new ArrayList<>(failures));
        }
    }
}
