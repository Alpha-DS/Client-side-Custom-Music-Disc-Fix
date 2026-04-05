package dev.alphads.clientside_custom_music_disc_fix.managers;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;

/** This class manages and stores the playlist of songs if the synchronize jukebox particles config is enabled. */

public class JukeboxParticleManager {
    private static final HashSet<Vec3> PARTICLE_LOCATIONS = new HashSet<>();
    // Constants for the note particle effect from a jukebox, obtained from JukeboxBlockEntity and ServerWorld classes
    private static final float offsetY = 0;
    private static final float offsetZ = 0;
    private static final float speed = 1;
    private static final double velocityY = speed * offsetY;
    private static final double velocityZ = speed * offsetZ;
    private static final ParticleOptions parameters = ParticleTypes.NOTE;
    public static final double BLOCKPOS_Y_OFFSET = 1.2F;
    private static final Minecraft client = Minecraft.getInstance();

    private static int currentTick = 0;

    public static void addParticleLocation(BlockPos pos) {
        Vec3 vec3 = Vec3.atBottomCenterOf(pos).add(0, BLOCKPOS_Y_OFFSET, 0);
        PARTICLE_LOCATIONS.add(vec3);
    }

    public static void removeParticleLocation(BlockPos pos) {
        Vec3 vec3 = Vec3.atBottomCenterOf(pos).add(0, BLOCKPOS_Y_OFFSET, 0);
        PARTICLE_LOCATIONS.remove(vec3);
    }

    public static void clearParticleLocations() {
        PARTICLE_LOCATIONS.clear();
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ++currentTick;
            if (currentTick == 20) {
                renderParticles();
                currentTick = 0;
            }
        });
    }

    private static void renderParticles() {
        for (Vec3 pos : PARTICLE_LOCATIONS) {
            if (client.level != null) {
                // Simulate randomness in the particle's speed
                float random = (float)client.level.getRandom().nextInt(4) / 24.0F;
                double velocityX = speed * random;
                client.level.addParticle(parameters, pos.x(), pos.y(), pos.z(), velocityX, velocityY, velocityZ);
            }
        }
    }
}
