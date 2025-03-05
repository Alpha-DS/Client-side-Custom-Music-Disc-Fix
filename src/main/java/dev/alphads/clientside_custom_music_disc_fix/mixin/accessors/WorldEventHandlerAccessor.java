package dev.alphads.clientside_custom_music_disc_fix.mixin.accessors;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.world.WorldEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientWorld.class)
public interface WorldEventHandlerAccessor {
    @Accessor
    WorldEventHandler getWorldEventHandler();
}
