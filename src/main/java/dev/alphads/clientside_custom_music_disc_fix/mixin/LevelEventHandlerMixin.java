package dev.alphads.clientside_custom_music_disc_fix.mixin;

import dev.alphads.clientside_custom_music_disc_fix.config.Config;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxHopperPlaylistManager;
import dev.alphads.clientside_custom_music_disc_fix.utils.PlayingSongsGetter;
import net.minecraft.core.Holder;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelEventHandler.class)
public abstract class LevelEventHandlerMixin {

    /** This method prevents new song instances from interrupting the current playing songs. Also stores
     * new songs to a playlist to simulate hopper support.
     * <p> Performance: Called everytime a new music disc is played on the server. </p>
     */
    @Inject(method = "playJukeboxSong", at = @At(value = "HEAD"), cancellable = true)
    private void playJukeboxSongMixin(Holder<JukeboxSong> songHolder, BlockPos pos, CallbackInfo ci) {
        SoundInstance oldSong = PlayingSongsGetter.getPlayingSongs().get(pos);
        // If a song is already playing at the jukebox position
        if (oldSong != null) {
            if (Config.getConfigOption(Config.ConfigKeys.SIMULATE_JUKEBOX_HOPPER) && songHolder != null) {
                JukeboxHopperPlaylistManager.addSongToPlaylist(pos, songHolder);
            }
            ci.cancel();
        }
    }

    /** This method interrupts packets that stop songs if an existing song is playing
     * <p> Performance: Called every time packet 1010 is sent, e.g. if disc is removed or jukebox is broken</p>
     */
    @Inject(method = "stopJukeboxSongAndNotifyNearby", at = @At(value = "HEAD"), cancellable = true)
    private void stopJukeboxSongAndNotifyNearbyMixin(BlockPos pos, CallbackInfo ci) {
        SoundInstance oldSong = PlayingSongsGetter.getPlayingSongs().get(pos);
        // If a song is already playing at the jukebox position
        if (oldSong != null) {
            ci.cancel();
        }
    }
}
