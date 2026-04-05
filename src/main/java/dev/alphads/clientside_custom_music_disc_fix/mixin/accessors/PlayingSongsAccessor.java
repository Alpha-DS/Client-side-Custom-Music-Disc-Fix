package dev.alphads.clientside_custom_music_disc_fix.mixin.accessors;

import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(LevelEventHandler.class)
public interface PlayingSongsAccessor {
    @Accessor
    Map<BlockPos, SoundInstance> getPlayingJukeboxSongs();
}
