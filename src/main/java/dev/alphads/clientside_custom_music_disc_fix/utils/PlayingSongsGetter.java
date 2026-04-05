package dev.alphads.clientside_custom_music_disc_fix.utils;

import dev.alphads.clientside_custom_music_disc_fix.mixin.accessors.PlayingSongsAccessor;
import dev.alphads.clientside_custom_music_disc_fix.mixin.accessors.LevelEventHandlerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Util class to get the playingSongs map from the Accessors. */
public class PlayingSongsGetter {
    public static Map<BlockPos, SoundInstance> getPlayingSongs() {
        if (Minecraft.getInstance().level == null) {
            return new HashMap<>();
        } else {
            LevelEventHandler levelEventHandler = ((LevelEventHandlerAccessor) Minecraft.getInstance().level).getLevelEventHandler();
            return ((PlayingSongsAccessor) levelEventHandler).getPlayingJukeboxSongs();
        }
    }
}
