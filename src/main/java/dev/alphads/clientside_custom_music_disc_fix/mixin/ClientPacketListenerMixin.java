package dev.alphads.clientside_custom_music_disc_fix.mixin;

import dev.alphads.clientside_custom_music_disc_fix.config.Config;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxParticleManager;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxHopperPlaylistManager;
import dev.alphads.clientside_custom_music_disc_fix.utils.PlayingSongsGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    /** This method removes the music disc sound instance from the playing songs list when the music disc
     *  is dropped from the jukebox. This is detected on the client side using the music disc item entity.
     * <p> Performance: Called everytime an entity tracker update packet is received. </p>
     */

    @Inject(method = "handleSetEntityData", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onEntityTrackerUpdateMixin(ClientboundSetEntityDataPacket packet, CallbackInfo ci, Entity entity) {
        if(!Config.getConfigOption(Config.ConfigKeys.STOP_WHEN_DISC_ENTITY_SPAWNS)) {
            return;
        }
        if (entity instanceof ItemEntity itemEntity) {
            if (itemEntity.getItem().getComponents().get(DataComponents.JUKEBOX_PLAYABLE) != null) {
                BlockPos jukeboxPos = calcJukeboxPos(itemEntity);
                // If the music disc coordinates are within a jukebox's dropping range
                if (jukeboxPos != null) {
                    SoundInstance currentSong = PlayingSongsGetter.getPlayingSongs().get(jukeboxPos);
                    // If the jukeboxPos has a song playing
                    if (currentSong != null) {
                        Minecraft.getInstance().getSoundManager().stop(currentSong);
                        PlayingSongsGetter.getPlayingSongs().remove(jukeboxPos);
                        if (Config.getConfigOption(Config.ConfigKeys.SYNC_JUKEBOX_PARTICLES)) {
                            JukeboxParticleManager.removeParticleLocation(jukeboxPos);
                        }
                        if (Config.getConfigOption(Config.ConfigKeys.SIMULATE_JUKEBOX_HOPPER)) {
                            // Play the next song in the playlist
                            Holder<JukeboxSong> nextSong = JukeboxHopperPlaylistManager.getSongFromPlaylist(jukeboxPos);
                            if (nextSong != null) {
                                Minecraft.getInstance().gui.setNowPlaying(nextSong.value().description());
                                SoundInstance songInstance = SimpleSoundInstance.forJukeboxSong(nextSong.value().soundEvent().value(), Vec3.atCenterOf(jukeboxPos));
                                Minecraft.getInstance().getSoundManager().play(songInstance);
                                PlayingSongsGetter.getPlayingSongs().put(jukeboxPos, songInstance);
                                if (Config.getConfigOption(Config.ConfigKeys.SYNC_JUKEBOX_PARTICLES)) {
                                    JukeboxParticleManager.addParticleLocation(jukeboxPos);
                                }
                            }
                        } else {
                            JukeboxHopperPlaylistManager.removePlaylist(jukeboxPos);
                        }
                    }
                }
            }
        }
    }

    /** This method transfers the management of the jukebox particles to the
     *  client through theJukeboxParticleManager class.
     * <p> Performance: Called everytime a particle packet is received. </p>
     */

    @Inject(method = "handleParticleEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER), cancellable = true)
    private void onParticleMixin(ClientboundLevelParticlesPacket packet, CallbackInfo ci){
        if(Config.getConfigOption(Config.ConfigKeys.SYNC_JUKEBOX_PARTICLES)){
            if(packet.getParticle() == ParticleTypes.NOTE){
                BlockPos jukeboxPos = calcParticlePos(packet.getX(), packet.getY(), packet.getZ());
                SoundInstance currentSong = PlayingSongsGetter.getPlayingSongs().get(jukeboxPos);
                if(currentSong != null){
                    JukeboxParticleManager.addParticleLocation(jukeboxPos);
                }
                ci.cancel();
            }
        } else {
            // Clear all particle caches on the client side if the config option is disabled
            JukeboxParticleManager.clearParticleLocations();
        }

    }

    @Unique
    private BlockPos calcParticlePos(double x, double y, double z){
        final double Y_OFFSET = JukeboxParticleManager.BLOCKPOS_Y_OFFSET;
        final double XZ_OFFSET = 0.5;
        return new BlockPos((int)(x - XZ_OFFSET), (int)(y - Y_OFFSET), (int)(z - XZ_OFFSET));
    }


    /** When the disc is dropped from the jukebox, two offsets are added to the BlockPos.
        Suppose the jukebox is located at (0, 0, 0). A constant offset of (0.5, 1.01, 0.5) is first applied
        for the disc's location, then a random offset between -0.35 to 0.35 is applied to each of its coordinates.
        This results in a final possible range of (0.15, 0.66, 0.15) to (0.85, 1.36, 0.85) for the music disc to have
        originated from a jukebox located at (0, 0, 0).
     */
    @Unique
    private BlockPos calcJukeboxPos(ItemEntity itemEntity){
        final double JUKEBOX_OFFSET_MIN = 0.66;
        final double JUKEBOX_OFFSET_MAX = 0.36;
        // Entity positions without the fractional part
        int entityPosIntX = (int) Math.floor(itemEntity.getX());
        int entityPosIntY = (int) Math.floor(itemEntity.getY());
        int entityPosIntZ = (int) Math.floor(itemEntity.getZ());

        // Get fractional part of the entity's y position
        double entityPosFracY = itemEntity.getY() - entityPosIntY;

        // Calculate the jukebox position
        if (entityPosFracY >= JUKEBOX_OFFSET_MIN && entityPosFracY < 1){
            return new BlockPos(entityPosIntX, entityPosIntY, entityPosIntZ);
        } else if (entityPosFracY >= 0 && entityPosFracY < JUKEBOX_OFFSET_MAX){
            return new BlockPos(entityPosIntX, entityPosIntY - 1, entityPosIntZ);
        } else {
            return null;
        }
    }
}
