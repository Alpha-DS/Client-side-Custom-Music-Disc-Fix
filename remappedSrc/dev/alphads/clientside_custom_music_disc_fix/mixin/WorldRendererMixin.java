package dev.alphads.clientside_custom_music_disc_fix.mixin;

import dev.alphads.clientside_custom_music_disc_fix.config.Config;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxParticleManager;
import dev.alphads.clientside_custom_music_disc_fix.mixin.accessors.PlayingSongsAccessor;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxHopperPlaylistManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    /** This method prevents new song instances from interrupting the current playing songs. Also stores
     * new songs to a playlist to simulate hopper support.
     * <p> Performance: Called everytime a new music disc is played or removed on the server. </p>
     */
    @Inject(method = "playSong", at = @At(value = "HEAD"), cancellable = true)
    private void playSongMixin(SoundEvent song, BlockPos songPosition, CallbackInfo ci) {
        SoundInstance oldSong = ((PlayingSongsAccessor) MinecraftClient.getInstance().worldRenderer).getPlayingSongs().get(songPosition);
        // If a song is already playing at the jukebox position
        if (oldSong != null) {
            if (Config.getConfigOption(Config.ConfigKeys.SIMULATE_JUKEBOX_HOPPER) && song != null) {
                JukeboxHopperPlaylistManager.addSongToPlaylist(songPosition, song);
            }
            ci.cancel();
        }
    }

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
            if (((PlayingSongsAccessor) MinecraftClient.getInstance().worldRenderer).getPlayingSongs().containsKey(pos)) {
                SoundInstance currentSong = ((PlayingSongsAccessor) MinecraftClient.getInstance().worldRenderer).getPlayingSongs().get(pos);
                MinecraftClient.getInstance().getSoundManager().stop(currentSong);
                ((PlayingSongsAccessor) MinecraftClient.getInstance().worldRenderer).getPlayingSongs().remove(pos);
                // Remove the playlist and note particles regardless of the config option
                JukeboxHopperPlaylistManager.removePlaylist(pos);
                JukeboxParticleManager.removeParticleLocation(pos);
            }
        }
    }
}