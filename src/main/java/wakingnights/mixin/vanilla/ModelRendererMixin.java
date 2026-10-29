package wakingnights.mixin.vanilla;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.model.ModelRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wakingnights.client.growth.GrowthLayerHandler;
import wakingnights.client.growth.IRenderedPart;

import java.nio.FloatBuffer;

/**
 * Remembers which entity render last drew this part, so growths only go on parts that are actually visible.
 * For special mobs it also keeps where the part was drawn, including anything the model did around it
 * (like shrinking a baby's body), so growths end up exactly on it.
 */
@Mixin(ModelRenderer.class)
public abstract class ModelRendererMixin implements IRenderedPart {
    @Shadow
    public boolean isHidden;
    @Shadow
    public boolean showModel;

    @Unique
    private int wakingnights$renderStamp = -1;
    @Unique
    private int wakingnights$matrixStamp = -1;
    @Unique
    private FloatBuffer wakingnights$matrix;
    @Unique
    private boolean wakingnights$matrixIncludesPart;

    @Inject(method = {"render(F)V", "renderWithRotation(F)V"}, at = @At("HEAD"))
    private void wakingnights$stampRender(float scale, CallbackInfo ci) {
        if(this.isHidden || !this.showModel) return;
        this.wakingnights$onRender(false);
    }

    @Override
    public void wakingnights$onRender(boolean matrixIncludesPart) {
        int stamp = GrowthLayerHandler.getRenderStamp();
        this.wakingnights$renderStamp = stamp;
        if(GrowthLayerHandler.isCapturingMatrices()) {
            if(this.wakingnights$matrix == null) this.wakingnights$matrix = GLAllocation.createDirectFloatBuffer(16);
            this.wakingnights$matrix.clear();
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, this.wakingnights$matrix);
            this.wakingnights$matrixStamp = stamp;
            this.wakingnights$matrixIncludesPart = matrixIncludesPart;
        }
    }

    @Override
    public boolean wakingnights$matrixIncludesPart() {
        return this.wakingnights$matrixIncludesPart;
    }

    @Override
    public int wakingnights$getRenderStamp() {
        return this.wakingnights$renderStamp;
    }

    @Override
    public boolean wakingnights$loadRenderMatrix() {
        if(this.wakingnights$matrix == null || this.wakingnights$matrixStamp != GrowthLayerHandler.getRenderStamp()) return false;
        this.wakingnights$matrix.rewind();
        GL11.glLoadMatrix(this.wakingnights$matrix);
        return true;
    }
}
