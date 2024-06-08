package dev.alphads.clientside_custom_music_disc_fix.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Map;

public class ConfigScreen extends Screen {
    private final ConfigBuilder configBuilder = ConfigBuilder.create();
    private final ConfigEntryBuilder entryBuilder = configBuilder.entryBuilder();
    private final ConfigCategory generalCategory = configBuilder.getOrCreateCategory(Text.translatable("category.client-side_custom_music_disc_fix.config.general"));

    public ConfigScreen(Text title, Screen parent) {
        super(title);
        configBuilder.setTitle(title);
        configBuilder.setParentScreen(parent);
        loadConfigOptions();
        configBuilder.setSavingRunnable(Config::saveConfig);
    }

    private void loadConfigOptions() {
        for (Map.Entry<String, Boolean> entry : Config.getConfigOptionsMap().entrySet()) {
            generalCategory.addEntry(entryBuilder.startBooleanToggle(Config.ConfigKeys.CONFIG_LABELS.get(entry.getKey()), entry.getValue())
                    .setDefaultValue(Config.DEFAULT_CONFIG_OPTIONS.options.get(entry.getKey()))
                    .setTooltip(Config.ConfigKeys.CONFIG_TOOLTIPS.get(entry.getKey()))
                    .setSaveConsumer(newValue -> Config.setConfigOption(entry.getKey(), newValue))
                    .build());
        }
    }

    public Screen buildScreen() {
        return configBuilder.build();
    }
}
