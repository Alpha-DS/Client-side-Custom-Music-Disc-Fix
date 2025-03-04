package dev.alphads.clientside_custom_music_disc_fix.mixin;

import dev.alphads.clientside_custom_music_disc_fix.config.Config;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxParticleManager;
import dev.alphads.clientside_custom_music_disc_fix.mixin.accessors.PlayingSongsAccessor;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxHopperPlaylistManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;


/** This mixin is responsible for removing the music disc sound instance from the playing songs list when the sound file
 *  from the resource pack reaches its end.
 * <p> Performance: Called everytime a sound stops playing. </p>
 */

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    @Unique
    private final String[] MUSIC_DISC_NAMES = {"13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal", "strad", "ward", "11", "wait", "pigstep", "otherside", "5", "relic"};
    @Unique
    private final ArrayList<SoundInstance> songsToPlay = new ArrayList<>();

    @Inject(method = "tick()V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/sound/SoundSystem;soundEndTicks:Ljava/util/Map;", ordinal = 1), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void tickMixin(CallbackInfo ci, Iterator<TickableSoundInstance> iterator, Map.Entry<SoundInstance, Channel.SourceManager> entry, Channel.SourceManager sourceManager2, SoundInstance soundInstance, float h) {
        String soundId = soundInstance.getId().toString();
        for (String musicDiscName : MUSIC_DISC_NAMES) {
            if (soundId.equals("minecraft:music_disc." + musicDiscName)) {
                Map<BlockPos, SoundInstance> playingSongs = ((PlayingSongsAccessor) MinecraftClient.getInstance().worldRenderer).getPlayingSongs();
                    if(playingSongs.containsValue(soundInstance)){
                        BlockPos soundPosition = new BlockPos((int)Math.floor(soundInstance.getX()), (int)Math.floor(soundInstance.getY()), (int)Math.floor(soundInstance.getZ()));
                        playingSongs.remove(soundPosition);
                        if (Config.getConfigOption(Config.ConfigKeys.SYNC_JUKEBOX_PARTICLES)) {
                            JukeboxParticleManager.removeParticleLocation(soundPosition);
                        }
                        // If simulate jukebox hopper is enabled, play the next song in the playlist. Clear the playlist otherwise
                        if (Config.getConfigOption(Config.ConfigKeys.SIMULATE_JUKEBOX_HOPPER)) {
                            SoundEvent nextSong = JukeboxHopperPlaylistManager.getSongFromPlaylist(soundPosition);
                            if (nextSong != null) {
                                MusicDiscItem musicDiscItem = MusicDiscItem.bySound(nextSong);
                                if (musicDiscItem != null) {
                                    MinecraftClient.getInstance().inGameHud.setRecordPlayingOverlay(musicDiscItem.getDescription());
                                }
                                SoundInstance songInstance = PositionedSoundInstance.record(nextSong, Vec3d.ofCenter(soundPosition));
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
                break;
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
}
