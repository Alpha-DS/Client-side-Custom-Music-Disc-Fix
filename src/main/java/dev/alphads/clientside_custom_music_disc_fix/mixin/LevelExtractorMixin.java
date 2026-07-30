package dev.alphads.clientside_custom_music_disc_fix.mixin;

import dev.alphads.clientside_custom_music_disc_fix.config.Config;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxParticleManager;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxHopperPlaylistManager;
import dev.alphads.clientside_custom_music_disc_fix.utils.PlayingSongsGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {

    /** This method removes the music disc sound instance from the playingSongs map when
     *  a jukebox with a SoundInstance is broken.
     * <p> Performance: Called everytime a block update packet is received. </p>
     */
    @Inject(method = "blockChanged", at = @At("TAIL"))
    private void blockChangedMixin(BlockPos pos, int updateFlags, CallbackInfo ci) {
        if (!Config.getConfigOption(Config.ConfigKeys.STOP_WHEN_JUKEBOX_BREAKS)) {
            return;
        }

        BlockState state = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getBlockState(pos) : null;
        if (state != null && state.isAir()) {
            if (PlayingSongsGetter.getPlayingSongs().containsKey(pos)) {
                SoundInstance currentSong = PlayingSongsGetter.getPlayingSongs().get(pos);
                Minecraft.getInstance().getSoundManager().stop(currentSong);
                PlayingSongsGetter.getPlayingSongs().remove(pos);
                // Remove the playlist and note particles regardless of the config option
                JukeboxHopperPlaylistManager.removePlaylist(pos);
                JukeboxParticleManager.removeParticleLocation(pos);
            }
        }
    }
}