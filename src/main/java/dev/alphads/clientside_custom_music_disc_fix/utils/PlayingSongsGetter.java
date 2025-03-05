package dev.alphads.clientside_custom_music_disc_fix.utils;

import dev.alphads.clientside_custom_music_disc_fix.mixin.accessors.PlayingSongsAccessor;
import dev.alphads.clientside_custom_music_disc_fix.mixin.accessors.WorldEventHandlerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.WorldEventHandler;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Util class to get the playingSongs map from the Accessors. */
public class PlayingSongsGetter {
    public static Map<BlockPos, SoundInstance> getPlayingSongs() {
        if (MinecraftClient.getInstance().world == null) {
            return new HashMap<>();
        } else {
            WorldEventHandler worldEventHandler = ((WorldEventHandlerAccessor) MinecraftClient.getInstance().world).getWorldEventHandler();
            return ((PlayingSongsAccessor) worldEventHandler).getPlayingSongs();
        }
    }
}
