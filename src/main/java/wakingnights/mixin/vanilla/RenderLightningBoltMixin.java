package wakingnights.mixin.vanilla;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wakingnights.client.EventVisuals;

/**
 * Lightning in the Waking Night's colour. Vanilla adds the bolt's light on top of what's behind it, which can only
 * brighten, so a dark colour would be invisible: during a Waking Night it's blended normally instead.
 */
@Mixin(RenderLightningBolt.class)
public abstract class RenderLightningBoltMixin {
    @Redirect(method = "doRender(Lnet/minecraft/entity/effect/EntityLightningBolt;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;blendFunc(Lnet/minecraft/client/renderer/GlStateManager$SourceFactor;Lnet/minecraft/client/renderer/GlStateManager$DestFactor;)V"))
    private void wakingnights$blend(GlStateManager.SourceFactor source, GlStateManager.DestFactor dest) {
        if(EventVisuals.getLightningColor() != null) GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        else GlStateManager.blendFunc(source, dest);
    }

    @Redirect(method = "doRender(Lnet/minecraft/entity/effect/EntityLightningBolt;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BufferBuilder;color(FFFF)Lnet/minecraft/client/renderer/BufferBuilder;"))
    private BufferBuilder wakingnights$color(BufferBuilder buffer, float red, float green, float blue, float alpha) {
        float[] color = EventVisuals.getLightningColor();
        return color != null ? buffer.color(color[0], color[1], color[2], alpha) : buffer.color(red, green, blue, alpha);
    }
}
