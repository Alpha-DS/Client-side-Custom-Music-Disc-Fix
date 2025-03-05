package dev.alphads.clientside_custom_music_disc_fix.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigScreen configScreen = new ConfigScreen(Text.translatable("title.client-side_custom_music_disc_fix.config"),parent);
            return configScreen.buildScreen();
        };
    }
}