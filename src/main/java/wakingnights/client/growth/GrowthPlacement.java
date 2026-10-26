package wakingnights.client.growth;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelChicken;
import net.minecraft.client.model.ModelCreeper;
import net.minecraft.client.model.ModelIllager;
import net.minecraft.client.model.ModelIronGolem;
import net.minecraft.client.model.ModelQuadruped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSnowMan;
import net.minecraft.client.model.ModelSpider;
import net.minecraft.client.model.ModelVillager;
import net.minecraft.client.model.ModelWolf;
import net.minecraft.client.renderer.GlStateManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Where growths can sit on a model: every visible face of every box, weighted so growths spread out sensibly.
 * Built once per model, from the pose of the first special mob drawn with it.
 * - bigger faces get more growths than small ones
 * - thin parts (legs, tails) get fewer, their growths would be tiny
 * - faces covered by another part (e.g. a wolf's body under its mane, the inside of an arm) get none
 * - the front of the head gets fewer, so they don't cover the face
 * - overlay parts (the hat layer around a zombie's head) and parts that weren't drawn (a horse's saddle) are left out
 * All positions are in model space and pixels, the same space ModelBox uses, with y pointing down.
 */
public class GrowthPlacement {
    //Faces a growth can sit on. +Y (the bottom) is left out so nothing grows out of feet.
    public static final int NEG_X = 0, POS_X = 1, NEG_Y = 2, NEG_Z = 3, POS_Z = 4;
    private static final int FACES = 5;

    //Faces narrower than this (in pixels) get fewer growths, in proportion
    private static final float THIN_SIZE = 6.0F;
    private static final float HEAD_FRONT_WEIGHT = 0.3F;
    //How far out from a face to check whether another part covers it
    private static final float COVER_CHECK_DISTANCE = 0.5F;
    private static final float[] SAMPLES = {0.2F, 0.5F, 0.8F};

    private static final Map<ModelBase, GrowthPlacement> PLACEMENTS = new WeakHashMap<>();

    private final Map<ModelRenderer, ModelRenderer> parents = new HashMap<>();
    //Rotation point and angles of every part when this was built, so spacing growths out doesn't change as the mob moves
    private final Map<ModelRenderer, float[]> restPose = new HashMap<>();
    private final List<Face> faces = new ArrayList<>();
    private float totalWeight = 0;

    public static GrowthPlacement get(ModelBase model) {
        return PLACEMENTS.computeIfAbsent(model, GrowthPlacement::new);
    }

    private GrowthPlacement(ModelBase model) {
        for(ModelRenderer part : model.boxList) {
            this.restPose.put(part, new float[]{part.rotationPointX, part.rotationPointY, part.rotationPointZ,
                    part.rotateAngleX, part.rotateAngleY, part.rotateAngleZ});
            if(part.childModels == null) continue;
            for(ModelRenderer child : part.childModels) this.parents.put(child, part);
        }

        List<ModelRenderer> parts = new ArrayList<>();
        for(ModelRenderer part : model.boxList) {
            if(part.cubeList == null || part.cubeList.isEmpty()) continue;
            if(!IRenderedPart.wasRendered(part) || isOverlay(model, part)) continue;
            parts.add(part);
        }

        ModelRenderer head = getHead(model, parts);
        for(ModelRenderer part : parts) {
            for(ModelBox box : part.cubeList) {
                for(int face = 0; face < FACES; face++) {
                    float weight = getArea(box, face) * Math.min(1.0F, getFaceSize(box, face) / THIN_SIZE) * this.getUncoveredFraction(parts, part, box, face);
                    if(part == head && face == NEG_Z) weight *= HEAD_FRONT_WEIGHT;
                    if(weight <= 0) continue;
                    this.totalWeight += weight;
                    this.faces.add(new Face(part, box, face, this.totalWeight));
                }
            }
        }
    }

    /** null if the model has no usable faces, the growths then go on the hitbox */
    @Nullable
    public Face pick(float roll) {
        float target = roll * this.totalWeight;
        for(Face face : this.faces) {
            if(target < face.cumulativeWeight) return face;
        }
        return this.faces.isEmpty() ? null : this.faces.get(this.faces.size() - 1);
    }

    /**
     * Moves into the part's frame. Normally by loading the matrix it was drawn with, which includes whatever the model
     * did around it (shrinking a baby, parent parts). Otherwise goes through the parent parts first, so a growth on
     * e.g. a lower arm still follows the upper arm.
     */
    public void applyPartTransform(ModelRenderer part, float scale) {
        if(((IRenderedPart) part).wakingnights$loadRenderMatrix()) {
            GlStateManager.translate(part.offsetX, part.offsetY, part.offsetZ);
            part.postRender(scale);
            return;
        }
        for(ModelRenderer current : this.getChain(part)) current.postRender(scale);
    }

    /** Where a point on a part was in model space when this was built (null part: the point is already in model space). */
    public float[] toRestModel(@Nullable ModelRenderer part, float[] point) {
        if(part == null) return point;
        float[] p = point.clone();
        List<ModelRenderer> chain = this.getChain(part);
        for(int i = chain.size() - 1; i >= 0; i--) {
            float[] pose = this.restPose.get(chain.get(i));
            if(pose == null) continue;
            p = rotateX(p, pose[3]);
            p = rotateY(p, pose[4]);
            p = rotateZ(p, pose[5]);
            p[0] += pose[0];
            p[1] += pose[1];
            p[2] += pose[2];
        }
        return p;
    }

    /** The part and its parents, outermost first */
    private List<ModelRenderer> getChain(ModelRenderer part) {
        List<ModelRenderer> chain = new ArrayList<>();
        for(ModelRenderer current = part; current != null && chain.size() < 16; current = this.parents.get(current)) {
            chain.add(0, current);
        }
        return chain;
    }

    // ------ Covered faces ------

    private float getUncoveredFraction(List<ModelRenderer> parts, ModelRenderer part, ModelBox box, int face) {
        float[] bounds = bounds(box);
        float[] normal = normal(face);
        List<ModelRenderer> chain = this.getChain(part);
        int uncovered = 0;
        for(float u : SAMPLES) {
            for(float v : SAMPLES) {
                float[] point = pointOnFace(bounds, face, u, v);
                for(int axis = 0; axis < 3; axis++) point[axis] += normal[axis] * COVER_CHECK_DISTANCE;
                if(!this.isInsideAnyBox(parts, box, toModel(chain, point))) uncovered++;
            }
        }
        return uncovered / (float) (SAMPLES.length * SAMPLES.length);
    }

    private boolean isInsideAnyBox(List<ModelRenderer> parts, ModelBox ignored, float[] modelPoint) {
        for(ModelRenderer part : parts) {
            float[] local = toLocal(this.getChain(part), modelPoint);
            for(ModelBox box : part.cubeList) {
                if(box == ignored) continue;
                if(local[0] > box.posX1 && local[0] < box.posX2 && local[1] > box.posY1 && local[1] < box.posY2
                        && local[2] > box.posZ1 && local[2] < box.posZ2) return true;
            }
        }
        return false;
    }

    /** Same order as ModelRenderer#postRender: translate to the rotation point, then rotate Z, Y, X. */
    private static float[] toModel(List<ModelRenderer> chain, float[] point) {
        float[] p = point.clone();
        for(int i = chain.size() - 1; i >= 0; i--) {
            ModelRenderer part = chain.get(i);
            p = rotateX(p, part.rotateAngleX);
            p = rotateY(p, part.rotateAngleY);
            p = rotateZ(p, part.rotateAngleZ);
            p[0] += part.rotationPointX;
            p[1] += part.rotationPointY;
            p[2] += part.rotationPointZ;
        }
        return p;
    }

    private static float[] toLocal(List<ModelRenderer> chain, float[] point) {
        float[] p = point.clone();
        for(ModelRenderer part : chain) {
            p[0] -= part.rotationPointX;
            p[1] -= part.rotationPointY;
            p[2] -= part.rotationPointZ;
            p = rotateZ(p, -part.rotateAngleZ);
            p = rotateY(p, -part.rotateAngleY);
            p = rotateX(p, -part.rotateAngleX);
        }
        return p;
    }

    private static float[] rotateX(float[] p, float angle) {
        if(angle == 0) return p;
        float cos = (float) Math.cos(angle), sin = (float) Math.sin(angle);
        return new float[]{p[0], p[1] * cos - p[2] * sin, p[1] * sin + p[2] * cos};
    }

    private static float[] rotateY(float[] p, float angle) {
        if(angle == 0) return p;
        float cos = (float) Math.cos(angle), sin = (float) Math.sin(angle);
        return new float[]{p[0] * cos + p[2] * sin, p[1], -p[0] * sin + p[2] * cos};
    }

    private static float[] rotateZ(float[] p, float angle) {
        if(angle == 0) return p;
        float cos = (float) Math.cos(angle), sin = (float) Math.sin(angle);
        return new float[]{p[0] * cos - p[1] * sin, p[0] * sin + p[1] * cos, p[2]};
    }

    // ------ Heads ------

    /** The head of vanilla models, or else the part that sticks out furthest to the front (models face -Z). */
    @Nullable
    private ModelRenderer getHead(ModelBase model, List<ModelRenderer> parts) {
        if(model instanceof ModelBiped) return ((ModelBiped) model).bipedHead;
        if(model instanceof ModelQuadruped) return ((ModelQuadruped) model).head;
        if(model instanceof ModelWolf) return ((ModelWolf) model).wolfHeadMain;
        if(model instanceof ModelCreeper) return ((ModelCreeper) model).head;
        if(model instanceof ModelSpider) return ((ModelSpider) model).spiderHead;
        if(model instanceof ModelVillager) return ((ModelVillager) model).villagerHead;
        if(model instanceof ModelIronGolem) return ((ModelIronGolem) model).ironGolemHead;
        if(model instanceof ModelChicken) return ((ModelChicken) model).head;
        if(model instanceof ModelIllager) return ((ModelIllager) model).head;
        if(model instanceof ModelSnowMan) return ((ModelSnowMan) model).head;

        ModelRenderer front = null;
        float frontZ = Float.MAX_VALUE;
        for(ModelRenderer part : parts) {
            List<ModelRenderer> chain = this.getChain(part);
            for(ModelBox box : part.cubeList) {
                float z = toModel(chain, new float[]{(box.posX1 + box.posX2) / 2, (box.posY1 + box.posY2) / 2, box.posZ1})[2];
                if(z < frontZ) {
                    frontZ = z;
                    front = part;
                }
            }
        }
        return front;
    }

    // ------ Overlays ------

    /** True if every box of this part wraps closely around a box of another part with the same pivot and rotation. */
    private static boolean isOverlay(ModelBase model, ModelRenderer part) {
        for(ModelRenderer other : model.boxList) {
            if(other == part || other.cubeList == null || other.cubeList.isEmpty() || !samePlacement(part, other)) continue;
            boolean wrapsAll = true;
            for(ModelBox box : part.cubeList) {
                boolean wraps = false;
                for(ModelBox inner : other.cubeList) {
                    if(wrapsAround(box, inner)) {
                        wraps = true;
                        break;
                    }
                }
                if(!wraps) {
                    wrapsAll = false;
                    break;
                }
            }
            if(wrapsAll) return true;
        }
        return false;
    }

    private static boolean samePlacement(ModelRenderer a, ModelRenderer b) {
        return close(a.rotationPointX, b.rotationPointX) && close(a.rotationPointY, b.rotationPointY) && close(a.rotationPointZ, b.rotationPointZ)
                && close(a.rotateAngleX, b.rotateAngleX) && close(a.rotateAngleY, b.rotateAngleY) && close(a.rotateAngleZ, b.rotateAngleZ);
    }

    private static boolean close(float a, float b) {
        return Math.abs(a - b) < 0.01F;
    }

    /** outer contains inner, with at most 1.5 pixels to spare on each side (and isn't the exact same box) */
    private static boolean wrapsAround(ModelBox outer, ModelBox inner) {
        float[] o = bounds(outer);
        float[] in = bounds(inner);
        boolean larger = false;
        for(int axis = 0; axis < 3; axis++) {
            float minGap = in[axis] - o[axis];
            float maxGap = o[axis + 3] - in[axis + 3];
            if(minGap < -0.01F || maxGap < -0.01F || minGap > 1.5F || maxGap > 1.5F) return false;
            if(minGap > 0.01F || maxGap > 0.01F) larger = true;
        }
        return larger;
    }

    // ------ Faces ------

    public static float[] bounds(ModelBox box) {
        return new float[]{box.posX1, box.posY1, box.posZ1, box.posX2, box.posY2, box.posZ2};
    }

    private static float getArea(ModelBox box, int face) {
        float dx = box.posX2 - box.posX1, dy = box.posY2 - box.posY1, dz = box.posZ2 - box.posZ1;
        switch(face) {
            case NEG_X: case POS_X: return dy * dz;
            case NEG_Y: return dx * dz;
            default: return dx * dy;
        }
    }

    public static float getFaceSize(ModelBox box, int face) {
        return getFaceSize(bounds(box), face);
    }

    /** The narrowest side of a face */
    public static float getFaceSize(float[] b, int face) {
        float dx = b[3] - b[0], dy = b[4] - b[1], dz = b[5] - b[2];
        switch(face) {
            case NEG_X: case POS_X: return Math.min(dy, dz);
            case NEG_Y: return Math.min(dx, dz);
            default: return Math.min(dx, dy);
        }
    }

    public static float[] pointOnFace(float[] b, int face, float u, float v) {
        float dx = b[3] - b[0], dy = b[4] - b[1], dz = b[5] - b[2];
        switch(face) {
            case NEG_X: return new float[]{b[0], b[1] + dy * u, b[2] + dz * v};
            case POS_X: return new float[]{b[3], b[1] + dy * u, b[2] + dz * v};
            case NEG_Y: return new float[]{b[0] + dx * u, b[1], b[2] + dz * v};
            case NEG_Z: return new float[]{b[0] + dx * u, b[1] + dy * v, b[2]};
            default: return new float[]{b[0] + dx * u, b[1] + dy * v, b[5]};
        }
    }

    private static float[] normal(int face) {
        switch(face) {
            case NEG_X: return new float[]{-1, 0, 0};
            case POS_X: return new float[]{1, 0, 0};
            case NEG_Y: return new float[]{0, -1, 0};
            case NEG_Z: return new float[]{0, 0, -1};
            default: return new float[]{0, 0, 1};
        }
    }

    /** Picks a face of the hitbox by area, for models without boxes. */
    public static int pickHitboxFace(float[] b, float roll) {
        float dx = b[3] - b[0], dy = b[4] - b[1], dz = b[5] - b[2];
        float[] areas = {dy * dz, dy * dz, dx * dz, dx * dy, dx * dy};
        float target = roll * (2 * dy * dz + dx * dz + 2 * dx * dy);
        int face = 0;
        while(face < FACES - 1 && target >= areas[face]) {
            target -= areas[face];
            face++;
        }
        return face;
    }

    /** Turns the growth's up (+Y) to point out of the face. */
    public static void rotateUpToNormal(int face) {
        switch(face) {
            case NEG_X: GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F); break;
            case POS_X: GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F); break;
            case NEG_Y: GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F); break;
            case NEG_Z: GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F); break;
            default: GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F); break;
        }
    }

    public static class Face {
        public final ModelRenderer part;
        public final ModelBox box;
        public final int face;
        private final float cumulativeWeight;

        Face(ModelRenderer part, ModelBox box, int face, float cumulativeWeight) {
            this.part = part;
            this.box = box;
            this.face = face;
            this.cumulativeWeight = cumulativeWeight;
        }
    }
}
