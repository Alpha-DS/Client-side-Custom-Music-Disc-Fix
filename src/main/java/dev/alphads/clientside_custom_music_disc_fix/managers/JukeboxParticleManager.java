package dev.alphads.clientside_custom_music_disc_fix.managers;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;

/** This class manages and stores the playlist of songs if the synchronize jukebox particles config is enabled. */

public class JukeboxParticleManager {
    private static final HashSet<Vec3d> PARTICLE_LOCATIONS = new HashSet<>();
    // Constants for the note particle effect from a jukebox, obtained from JukeboxBlockEntity and ServerWorld classes
    private static final float offsetY = 0;
    private static final float offsetZ = 0;
    private static final float speed = 1;
    private static final double velocityY = speed * offsetY;
    private static final double velocityZ = speed * offsetZ;
    private static final ParticleEffect parameters = ParticleTypes.NOTE;
    public static final double BLOCKPOS_Y_OFFSET = 1.2F;
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static int currentTick = 0;

    public static void addParticleLocation(BlockPos pos) {
        Vec3d vec3d = Vec3d.ofBottomCenter(pos).add(0, BLOCKPOS_Y_OFFSET, 0);
        PARTICLE_LOCATIONS.add(vec3d);
    }

    public static void removeParticleLocation(BlockPos pos) {
        Vec3d vec3d = Vec3d.ofBottomCenter(pos).add(0, BLOCKPOS_Y_OFFSET, 0);
        PARTICLE_LOCATIONS.remove(vec3d);
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
        for (Vec3d pos : PARTICLE_LOCATIONS) {
            if (client.world != null) {
                // Simulate randomness in the particle's speed
                float random = (float)client.world.getRandom().nextInt(4) / 24.0F;
                double velocityX = speed * random;
                client.world.addParticleClient(parameters, pos.getX(), pos.getY(), pos.getZ(), velocityX, velocityY, velocityZ);
            }
        }
    }
}
