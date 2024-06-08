package dev.alphads.clientside_custom_music_disc_fix.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger("ClientSideCustomMusicDiscFix");
    private static final String CONFIG_FILE_NAME = "client-side_custom_music_disc_fix.json";
    private static final Path CONFIG_FILE_PATH = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
    static final ConfigOptions DEFAULT_CONFIG_OPTIONS = new ConfigOptions();
    private static ConfigOptions configOptions = new ConfigOptions(DEFAULT_CONFIG_OPTIONS);

    private Config() {}

    public static void instantiateConfig() {
        if (!CONFIG_FILE_PATH.toFile().exists()) {
            generateDefaultConfigFile();
            LOGGER.warn("Cannot find config file. New config file generated at {}", CONFIG_FILE_PATH.toAbsolutePath());
        } else {
            readConfigFromFile();
            validateConfigOptions();
        }
    }

    private static void generateDefaultConfigFile() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(CONFIG_FILE_PATH.toFile())) {
            gson.toJson(DEFAULT_CONFIG_OPTIONS, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to generate default config file: {}. Using default config values.", e.getMessage());
        }
    }

    private static void readConfigFromFile() {
        try (FileReader reader = new FileReader(CONFIG_FILE_PATH.toFile())) {
            Gson gson = new Gson();
            configOptions = new ConfigOptions(gson.fromJson(reader, ConfigOptions.class));
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("Failed to read config file: {}. Using default config values.", e.getMessage());
            configOptions = new ConfigOptions(DEFAULT_CONFIG_OPTIONS);
        }
    }

    private static void validateConfigOptions() {
        // Check for missing config options
        for (Map.Entry<String, Boolean> entry : DEFAULT_CONFIG_OPTIONS.options.entrySet()) {
            if (!configOptions.options.containsKey(entry.getKey())) {
                LOGGER.error("Missing config option: {}. Using default value: {}", entry.getKey(), entry.getValue());
                configOptions.options.put(entry.getKey(), entry.getValue());
            }
        }
        // Check for unknown config options
        Iterator<Map.Entry<String, Boolean>> iterator = configOptions.options.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Boolean> entry = iterator.next();
            if (!DEFAULT_CONFIG_OPTIONS.options.containsKey(entry.getKey())) {
                LOGGER.warn("Unknown config option: {}", entry.getKey());
                iterator.remove();
            }
        }
    }

    public static void saveConfig() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(CONFIG_FILE_PATH.toFile())) {
            gson.toJson(configOptions, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save config file: {}", e.getMessage());
        }
    }

    public static boolean getConfigOption(String option) {
        return configOptions.getConfigOption(option);
    }

    static HashMap<String, Boolean> getConfigOptionsMap() {
        return configOptions.options;
    }

    static void setConfigOption(String option, boolean value) {
        if (configOptions.options.containsKey(option)) {
            configOptions.options.put(option, value);
        } else {
            LOGGER.error("Unknown config option: {}. Value is unchanged.", option);
        }
    }

    public static final class ConfigKeys {
        public static final String STOP_WHEN_DISC_ENTITY_SPAWNS = "stopWhenDiscEntitySpawns";
        public static final String STOP_WHEN_JUKEBOX_BREAKS = "stopWhenJukeboxBreaks";
        public static final String SYNC_JUKEBOX_PARTICLES = "syncJukeboxParticles";
        public static final String SIMULATE_JUKEBOX_HOPPER = "simulateJukeboxHopper";

        // Add text for each config option
        public static final Map<String, Text> CONFIG_LABELS = new HashMap<>();
        static {
            CONFIG_LABELS.put(STOP_WHEN_DISC_ENTITY_SPAWNS, Text.translatable("config.client_side_custom_music_disc_fix.stopWhenDiscEntitySpawns"));
            CONFIG_LABELS.put(STOP_WHEN_JUKEBOX_BREAKS, Text.translatable("config.client_side_custom_music_disc_fix.stopWhenJukeboxBreaks"));
            CONFIG_LABELS.put(SYNC_JUKEBOX_PARTICLES, Text.translatable("config.client_side_custom_music_disc_fix.syncJukeboxParticles"));
            CONFIG_LABELS.put(SIMULATE_JUKEBOX_HOPPER, Text.translatable("config.client_side_custom_music_disc_fix.simulateJukeboxHopper"));
        }

        // Add tooltips for each config option
        public static final Map<String, Text> CONFIG_TOOLTIPS = new HashMap<>();
        static {
            CONFIG_TOOLTIPS.put(STOP_WHEN_DISC_ENTITY_SPAWNS, Text.translatable("config.client_side_custom_music_disc_fix.stopWhenDiscEntitySpawns.tooltip"));
            CONFIG_TOOLTIPS.put(STOP_WHEN_JUKEBOX_BREAKS, Text.translatable("config.client_side_custom_music_disc_fix.stopWhenJukeboxBreaks.tooltip"));
            CONFIG_TOOLTIPS.put(SYNC_JUKEBOX_PARTICLES, Text.translatable("config.client_side_custom_music_disc_fix.syncJukeboxParticles.tooltip"));
            CONFIG_TOOLTIPS.put(SIMULATE_JUKEBOX_HOPPER, Text.translatable("config.client_side_custom_music_disc_fix.simulateJukeboxHopper.tooltip"));
        }

        private ConfigKeys() {
        }
    }

    static class ConfigOptions {
        final HashMap<String, Boolean> options;

        public ConfigOptions() {
            options = new HashMap<>();
            // Default config options
            options.put(ConfigKeys.STOP_WHEN_DISC_ENTITY_SPAWNS, true);
            options.put(ConfigKeys.STOP_WHEN_JUKEBOX_BREAKS, true);
            options.put(ConfigKeys.SYNC_JUKEBOX_PARTICLES, true);
            options.put(ConfigKeys.SIMULATE_JUKEBOX_HOPPER, true);
        }

        @Contract(pure = true)
        public ConfigOptions(@NotNull ConfigOptions other) {
            this.options = new HashMap<>(other.options);
        }

        public boolean getConfigOption(String option) {
            Boolean value = options.get(option);
            if (value != null) {
                return value;
            } else {
                LOGGER.error("Unknown config option: {}. Using default value {}", option, DEFAULT_CONFIG_OPTIONS.options.get(option));
                return DEFAULT_CONFIG_OPTIONS.options.get(option);
            }
        }
    }
}