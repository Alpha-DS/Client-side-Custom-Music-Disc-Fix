package dev.alphads.clientside_custom_music_disc_fix.mixin;

import dev.alphads.clientside_custom_music_disc_fix.config.Config;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxParticleManager;
import dev.alphads.clientside_custom_music_disc_fix.mixin.accessors.PlayingSongsAccessor;
import dev.alphads.clientside_custom_music_disc_fix.managers.JukeboxHopperPlaylistManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Unique
    private final TagKey<Item> MUSIC_DISC_TAG = TagKey.of(Registries.ITEM.getKey(), new Identifier("minecraft", "music_discs"));

    /** This method removes the music disc sound instance from the playing songs list when the music disc
     *  is dropped from the jukebox. This is detected on the client side using the music disc item entity.
     * <p> Performance: Called everytime an entity tracker update packet is received. </p>
     */

    @Inject(method = "onEntityTrackerUpdate", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onEntityTrackerUpdateMixin(EntityTrackerUpdateS2CPacket packet, CallbackInfo ci, Entity entity) {
        if(!Config.getConfigOption(Config.ConfigKeys.STOP_WHEN_DISC_ENTITY_SPAWNS)) {
            return;
        }
        if (entity instanceof ItemEntity itemEntity) {
            if (itemEntity.getStack().isIn(MUSIC_DISC_TAG)) {
                BlockPos jukeboxPos = calcJukeboxPos(itemEntity);
                // If the music disc coordinates are within a jukebox's dropping range
                if (jukeboxPos != null) {
                    SoundInstance currentSong = ((PlayingSongsAccessor) MinecraftClient.getInstance().worldRenderer).getPlayingSongs().get(jukeboxPos);
                    // If the jukeboxPos has a song playing
                    if (currentSong != null) {
                        MinecraftClient.getInstance().getSoundManager().stop(currentSong);
                        ((PlayingSongsAccessor) MinecraftClient.getInstance().worldRenderer).getPlayingSongs().remove(jukeboxPos);
                        if (Config.getConfigOption(Config.ConfigKeys.SYNC_JUKEBOX_PARTICLES)) {
                            JukeboxParticleManager.removeParticleLocation(jukeboxPos);
                        }
                        if (Config.getConfigOption(Config.ConfigKeys.SIMULATE_JUKEBOX_HOPPER)) {
                            // Play the next song in the playlist
                            SoundEvent nextSong = JukeboxHopperPlaylistManager.getSongFromPlaylist(jukeboxPos);
                            if (nextSong != null) {
                                MusicDiscItem musicDiscItem = MusicDiscItem.bySound(nextSong);
                                if (musicDiscItem != null) {
                                    MinecraftClient.getInstance().inGameHud.setRecordPlayingOverlay(musicDiscItem.getDescription());
                                }
                                SoundInstance songInstance = PositionedSoundInstance.record(nextSong, Vec3d.ofCenter(jukeboxPos));
                                MinecraftClient.getInstance().getSoundManager().play(songInstance);
                                ((PlayingSongsAccessor) MinecraftClient.getInstance().worldRenderer).getPlayingSongs().put(jukeboxPos, songInstance);
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

    @Inject(method = "onParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER), cancellable = true)
    private void onParticleMixin(ParticleS2CPacket packet, CallbackInfo ci){
        if(Config.getConfigOption(Config.ConfigKeys.SYNC_JUKEBOX_PARTICLES)){
            if(packet.getParameters() == ParticleTypes.NOTE){
                BlockPos jukeboxPos = calcParticlePos(packet.getX(), packet.getY(), packet.getZ());
                SoundInstance currentSong = ((PlayingSongsAccessor) MinecraftClient.getInstance().worldRenderer).getPlayingSongs().get(jukeboxPos);
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
        int entityPosIntX = (int) Math.floor(itemEntity.getPos().getX());
        int entityPosIntY = (int) Math.floor(itemEntity.getPos().getY());
        int entityPosIntZ = (int) Math.floor(itemEntity.getPos().getZ());

        // Get fractional part of the entity's y position
        double entityPosFracY = itemEntity.getPos().getY() - entityPosIntY;

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
