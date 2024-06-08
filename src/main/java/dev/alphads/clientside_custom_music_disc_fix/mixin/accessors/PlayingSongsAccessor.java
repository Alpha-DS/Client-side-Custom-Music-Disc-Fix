package dev.alphads.clientside_custom_music_disc_fix.mixin.accessors;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(WorldRenderer.class)
public interface PlayingSongsAccessor {
    @Accessor
    Map<BlockPos, SoundInstance> getPlayingSongs();
}
