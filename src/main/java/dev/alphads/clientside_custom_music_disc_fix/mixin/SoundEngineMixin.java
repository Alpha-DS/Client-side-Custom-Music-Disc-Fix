package dev.alphads.clientside_custom_music_disc_fix.mixin;

import dev.alphads.clientside_custom_music_disc_fix.config.Config;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxParticleManager;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxHopperPlaylistManager;
import dev.alphads.clientside_custom_music_disc_fix.utils.PlayingSongsGetter;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Unique
    private ArrayList<SoundInstance> songsToPlay = new ArrayList<>();

    /** This method is responsible for removing the music disc sound instance from the playing songs list when the sound file
     *  from the resource pack reaches its end.
     * <p> Performance: Called everytime a sound stops playing. </p>
     */

    @Inject(method = "tickInGameSound()V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/sounds/SoundEngine;soundDeleteTime:Ljava/util/Map;", ordinal = 1, opcode = Opcodes.GETFIELD), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void tickMixin(CallbackInfo ci, Iterator<Map.Entry<SoundInstance, ChannelAccess.ChannelHandle>> iterator, Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry, ChannelAccess.ChannelHandle handle, SoundInstance instance) {
        Map<BlockPos, SoundInstance> playingSongs = PlayingSongsGetter.getPlayingSongs();
        if(playingSongs.containsValue(instance)){
            BlockPos soundPosition = new BlockPos((int)Math.floor(instance.getX()), (int)Math.floor(instance.getY()), (int)Math.floor(instance.getZ()));
            playingSongs.remove(soundPosition);
            if (Config.getConfigOption(Config.ConfigKeys.SYNC_JUKEBOX_PARTICLES)) {
                JukeboxParticleManager.removeParticleLocation(soundPosition);
            }
            // If simulate jukebox hopper is enabled, play the next song in the playlist. Clear the playlist otherwise
            if (Config.getConfigOption(Config.ConfigKeys.SIMULATE_JUKEBOX_HOPPER)) {
                Holder<JukeboxSong> nextSong = JukeboxHopperPlaylistManager.getSongFromPlaylist(soundPosition);
                if (nextSong != null) {
                    Minecraft.getInstance().gui.hud.setNowPlaying(nextSong.value().description());
                    SoundInstance songInstance = SimpleSoundInstance.forJukeboxSong(nextSong.value().soundEvent().value(), Vec3.atCenterOf(soundPosition));
                    // Deferring the play call to the end of the tick method to avoid ConcurrentModificationException
                    if (songsToPlay == null) {
                        songsToPlay = new ArrayList<>();
                    }
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

    @Inject(method = "tickInGameSound()V", at = @At("TAIL"))
    private void playSongs(CallbackInfo ci) {
        if (songsToPlay == null) return;
        for (SoundInstance songInstance : songsToPlay) {
            Minecraft.getInstance().getSoundManager().play(songInstance);
        }
        songsToPlay.clear();
    }

    /** This method disables music disc attenuation if the config option is enabled.
     * <p> Performance: Called everytime a sound is played. </p>
     */
    @Unique
    private static Boolean shouldModify = false;

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"))
    private void playMixin(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (Config.getConfigOption(Config.ConfigKeys.DISABLE_DISC_ATTENUATION) && instance.getSource() == SoundSource.RECORDS) {
            shouldModify = true;
        }
    }

    // Set attenuation type to NONE
    @ModifyVariable(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At(value = "STORE"), name = "attenuation")
    private SoundInstance.Attenuation modifyAttenuation(SoundInstance.Attenuation attenuation) {
        return shouldModify ? SoundInstance.Attenuation.NONE : attenuation;
    }

    // Set relative to true (disables 3D positional audio effect)
    @ModifyVariable(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At(value = "STORE"), name = "isRelative")
    private boolean modifyRelative (boolean isRelative) {
        if (shouldModify) {
            shouldModify = false;
            return true;
        }
        return isRelative;
    }
}
