package dev.alphads.clientside_custom_music_disc_fix.mixin.accessors;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientLevel.class)
public interface LevelEventHandlerAccessor {
    @Accessor
    LevelEventHandler getLevelEventHandler();
}
