package dev.alphads.clientside_custom_music_disc_fix.mixin;

import dev.alphads.clientside_custom_music_disc_fix.config.Config;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxParticleManager;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxHopperPlaylistManager;
import dev.alphads.clientside_custom_music_disc_fix.utils.PlayingSongsGetter;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    @Unique
    private final ArrayList<SoundInstance> songsToPlay = new ArrayList<>();

    /** This method is responsible for removing the music disc sound instance from the playing songs list when the sound file
     *  from the resource pack reaches its end.
     * <p> Performance: Called everytime a sound stops playing. </p>
     */

    @Inject(method = "tick()V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/sound/SoundSystem;soundEndTicks:Ljava/util/Map;", ordinal = 1), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void tickMixin(CallbackInfo ci, Iterator<TickableSoundInstance> iterator, Map.Entry<SoundInstance, Channel.SourceManager> entry, Channel.SourceManager sourceManager2, SoundInstance soundInstance, float h) {
        Map<BlockPos, SoundInstance> playingSongs = PlayingSongsGetter.getPlayingSongs();
        if(playingSongs.containsValue(soundInstance)){
            BlockPos soundPosition = new BlockPos((int)Math.floor(soundInstance.getX()), (int)Math.floor(soundInstance.getY()), (int)Math.floor(soundInstance.getZ()));
            playingSongs.remove(soundPosition);
            if (Config.getConfigOption(Config.ConfigKeys.SYNC_JUKEBOX_PARTICLES)) {
                JukeboxParticleManager.removeParticleLocation(soundPosition);
            }
            // If simulate jukebox hopper is enabled, play the next song in the playlist. Clear the playlist otherwise
            if (Config.getConfigOption(Config.ConfigKeys.SIMULATE_JUKEBOX_HOPPER)) {
                RegistryEntry<JukeboxSong> nextSong = JukeboxHopperPlaylistManager.getSongFromPlaylist(soundPosition);
                if (nextSong != null) {
                    MinecraftClient.getInstance().inGameHud.setRecordPlayingOverlay(nextSong.value().description());
                    SoundInstance songInstance = PositionedSoundInstance.record(nextSong.value().soundEvent().value(), Vec3d.ofCenter(soundPosition));
                    // Deferring the play call to the end of the tick method to avoid ConcurrentModificationException
                    songsToPlay.add(songInstance);
                    playingSongs.put(soundPosition, songInstance);
                    if (Config.getConfigOption(Config.ConfigKeys.SYNC_JUKEBOX_PARTICLES)) {
                        JukeboxParticleManager.addParticleLocation(soundPosition);
                    }
                }
            } else {
                JukeboxHopperPlaylistManager.removePlaylist(soundPosition);
            }
        }
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void playSongs(CallbackInfo ci) {
        for (SoundInstance songInstance : songsToPlay) {
            MinecraftClient.getInstance().getSoundManager().play(songInstance);
        }
        songsToPlay.clear();
    }

    /** This method disables music disc attenuation if the config option is enabled.
     * <p> Performance: Called everytime a sound is played. </p>
     */
    @Unique
    private static Boolean shouldModify = false;

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"))
    private void playMixin(SoundInstance sound, CallbackInfo ci) {
        if (Config.getConfigOption(Config.ConfigKeys.DISABLE_DISC_ATTENUATION) && sound.getCategory() == SoundCategory.RECORDS) {
            shouldModify = true;
        }
    }

    // Set attenuation type to NONE
    @ModifyVariable(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At(value = "STORE"), ordinal = 0)
    private SoundInstance.AttenuationType modifyAttenuationType(SoundInstance.AttenuationType attenuationType) {
        return shouldModify ? SoundInstance.AttenuationType.NONE : attenuationType;
    }

    // Set relative to true (disables 3D positional audio effect)
    @ModifyVariable(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At(value = "STORE"), ordinal = 0)
    private boolean modifyRelative (boolean bl) {
        if (shouldModify) {
            shouldModify = false;
            return true;
        }
        return bl;
    }
}
