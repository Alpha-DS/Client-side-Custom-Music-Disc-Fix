package dev.alphads.clientside_custom_music_disc_fix.mixin;

import dev.alphads.clientside_custom_music_disc_fix.config.Config;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxHopperPlaylistManager;
import dev.alphads.clientside_custom_music_disc_fix.utils.PlayingSongsGetter;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.WorldEventHandler;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldEventHandler.class)
public abstract class WorldEventHandlerMixin {

    /** This method prevents new song instances from interrupting the current playing songs. Also stores
     * new songs to a playlist to simulate hopper support.
     * <p> Performance: Called everytime a new music disc is played on the server. </p>
     */
    @Inject(method = "playJukeboxSong", at = @At(value = "HEAD"), cancellable = true)
    private void playJukeboxSongMixin(RegistryEntry<JukeboxSong> song, BlockPos songPosition, CallbackInfo ci) {
        SoundInstance oldSong = PlayingSongsGetter.getPlayingSongs().get(songPosition);
        // If a song is already playing at the jukebox position
        if (oldSong != null) {
            if (Config.getConfigOption(Config.ConfigKeys.SIMULATE_JUKEBOX_HOPPER) && song != null) {
                JukeboxHopperPlaylistManager.addSongToPlaylist(songPosition, song);
            }
            ci.cancel();
        }
    }

    /** This method interrupts packets that stop songs if an existing song is playing
     * <p> Performance: Called every time packet 1010 is sent, e.g. if disc is removed or jukebox is broken</p>
     */
    @Inject(method = "stopJukeboxSongAndUpdate", at = @At(value = "HEAD"), cancellable = true)
    private void stopJukeboxSongAndUpdateMixin(BlockPos songPosition, CallbackInfo ci) {
        SoundInstance oldSong = PlayingSongsGetter.getPlayingSongs().get(songPosition);
        // If a song is already playing at the jukebox position
        if (oldSong != null) {
            ci.cancel();
        }
    }
}
