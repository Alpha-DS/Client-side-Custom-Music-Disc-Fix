package dev.alphads.clientside_custom_music_disc_fix;

import dev.alphads.clientside_custom_music_disc_fix.config.Config;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxParticleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class ClientSideCustomMusicDiscFix implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Config.instantiateConfig();
        JukeboxParticleManager.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> Config.saveConfig());
    }
}
