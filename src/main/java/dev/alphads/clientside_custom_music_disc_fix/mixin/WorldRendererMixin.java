package dev.alphads.clientside_custom_music_disc_fix.mixin;

import dev.alphads.clientside_custom_music_disc_fix.config.Config;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxParticleManager;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxHopperPlaylistManager;
import dev.alphads.clientside_custom_music_disc_fix.utils.PlayingSongsGetter;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    /** This method removes the music disc sound instance from the playingSongs map when
     *  a jukebox with a SoundInstance is broken.
     * <p> Performance: Called everytime a block update packet is received. </p>
     */
    @Inject(method = "updateBlock", at = @At("TAIL"))
    private void updateBlockMixin(BlockView world, BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        if (!Config.getConfigOption(Config.ConfigKeys.STOP_WHEN_JUKEBOX_BREAKS)) {
            return;
        }
        if(newState.isAir()) {
            if (PlayingSongsGetter.getPlayingSongs().containsKey(pos)) {
                SoundInstance currentSong = PlayingSongsGetter.getPlayingSongs().get(pos);
                MinecraftClient.getInstance().getSoundManager().stop(currentSong);
                PlayingSongsGetter.getPlayingSongs().remove(pos);
                // Remove the playlist and note particles regardless of the config option
                JukeboxHopperPlaylistManager.removePlaylist(pos);
                JukeboxParticleManager.removeParticleLocation(pos);
            }
        }
    }
}