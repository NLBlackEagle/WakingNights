package wakingnights.client.growth;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import wakingnights.WakingNights;
import wakingnights.capability.CapabilitySpecialEntityHandler;
import wakingnights.capability.SpecialType;
import wakingnights.handlers.ForgeConfigHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Renders the growths on a special mob. Like vanilla's arrows stuck in a player, each growth sits on a random part of
 * the mob's model and moves with it (see GrowthPlacement for where they can go). Placement, rotation, size, shape,
 * texture and animation timing are all rolled from the mob's UUID, so every client sees the same growths without syncing them.
 * Mobs whose model has no normal parts (e.g. Lycanites' .obj models) get them on their hitbox instead.
 */
public class GrowthLayer implements LayerRenderer<EntityLivingBase> {
    public static final ResourceLocation FALLBACK_TEXTURE = new ResourceLocation(WakingNights.MODID, "textures/entity/growth/growth_01.png");

    private static final GrowthShape[] SHAPES = {
            new GrowthShape("growth_01", 6.0F, 3.0D)
    };

    private static final int MAX_GROWTHS = 10;
    //Spots tried per growth, the one furthest from the others wins
    private static final int SPOT_ATTEMPTS = 6;
    //Distance growths try to keep from each other, compared to the mob's width
    private static final float MIN_SPACING_TO_MOB = 0.6F;
    //Growth width compared to the narrowest side of the face it sits on
    private static final float SIZE_TO_FACE = 1.4F;
    //Largest growth width compared to the mob's width
    private static final float MAX_SIZE_TO_MOB = 0.7F;
    private static final float MIN_SCALE = 0.15F;
    private static final float MAX_SCALE = 4.0F;
    private static final float MAX_TILT = 35.0F;
    //Per axis, so not every growth has the same proportions
    private static final float MAX_STRETCH = 0.15F;

    private static final Map<SpecialType, List<ResourceLocation>> TEXTURES = new EnumMap<>(SpecialType.class);
    private static boolean loggedError = false;
    private static String[] parsedMultipliers;
    private static Map<String, Float> sizeMultipliers = Collections.emptyMap();

    private final RenderLivingBase<?> renderer;

    public GrowthLayer(RenderLivingBase<?> renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        SpecialType type = CapabilitySpecialEntityHandler.getType(entity);
        if(type == SpecialType.NONE || entity.isInvisibleToPlayer(Minecraft.getMinecraft().player)) return;

        GrowthPlacement placement = GrowthPlacement.get(this.renderer.getMainModel());
        List<ResourceLocation> textures = getTextures(type);
        float mobMultiplier = getSizeMultiplier(entity);

        //Growths glow in the dark: drawn fully lit, the mob's own light is put back afterwards
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        try {
            this.renderGrowths(entity, type, placement, textures, mobMultiplier, scale, partialTicks);
        }
        finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
        }
    }

    private void renderGrowths(EntityLivingBase entity, SpecialType type, GrowthPlacement placement, List<ResourceLocation> textures,
                               float mobMultiplier, float scale, float partialTicks) {
        UUID id = entity.getUniqueID();
        Random rand = new Random(id.getMostSignificantBits() ^ id.getLeastSignificantBits());
        int count = Math.min(MAX_GROWTHS, 4 + rand.nextInt(3) + (int) (entity.width * entity.height));
        float[] hitbox = getHitboxBounds(entity);
        List<float[]> placed = new ArrayList<>();
        //In the model's own pixels. Babies are half as wide but their model is shrunk as a whole, growths included.
        float modelWidth = (entity.isChild() ? entity.width * 2.0F : entity.width) * 16.0F;
        //Squared: about a growth's width, so they don't overlap
        float minSpacing = modelWidth * MIN_SPACING_TO_MOB;
        minSpacing *= minSpacing;

        for(int i = 0; i < count; i++) {
            //Everything is rolled before anything gets skipped, so a growth always gets the same values.
            //Out of a few spots take the first one that keeps its distance from the growths placed so far,
            //or else the one furthest away from them.
            GrowthPlacement.Face face = null;
            int side = 0;
            float[] bounds = hitbox;
            float[] point = null;
            float bestDistance = -1;
            for(int attempt = 0; attempt < SPOT_ATTEMPTS; attempt++) {
                GrowthPlacement.Face candidate = placement.pick(rand.nextFloat());
                float faceRoll = rand.nextFloat();
                float u = 0.15F + rand.nextFloat() * 0.7F;
                float v = 0.15F + rand.nextFloat() * 0.7F;

                float[] candidateBounds = candidate != null ? GrowthPlacement.bounds(candidate.box) : hitbox;
                int candidateSide = candidate != null ? candidate.face : GrowthPlacement.pickHitboxFace(hitbox, faceRoll);
                float[] candidatePoint = GrowthPlacement.pointOnFace(candidateBounds, candidateSide, u, v);
                float distance = getDistanceToClosest(placement.toRestModel(candidate == null ? null : candidate.part, candidatePoint), placed);
                if(distance > bestDistance && bestDistance < minSpacing) {
                    bestDistance = distance;
                    face = candidate;
                    side = candidateSide;
                    bounds = candidateBounds;
                    point = candidatePoint;
                }
            }
            placed.add(placement.toRestModel(face == null ? null : face.part, point));

            float spin = rand.nextFloat() * 360.0F;
            float tiltX = (rand.nextFloat() * 2.0F - 1.0F) * MAX_TILT;
            float tiltZ = (rand.nextFloat() * 2.0F - 1.0F) * MAX_TILT;
            float sizeVariation = 0.8F + rand.nextFloat() * 0.4F;
            GrowthShape shape = SHAPES[rand.nextInt(SHAPES.length)];
            ResourceLocation texture = textures.get(rand.nextInt(textures.size()));
            double speed = 0.85D + rand.nextDouble() * 0.3D;
            double phaseTicks = rand.nextDouble() * shape.loopTicks;
            float stretchX = 1.0F + (rand.nextFloat() * 2.0F - 1.0F) * MAX_STRETCH;
            float stretchY = 1.0F + (rand.nextFloat() * 2.0F - 1.0F) * MAX_STRETCH;
            float stretchZ = 1.0F + (rand.nextFloat() * 2.0F - 1.0F) * MAX_STRETCH;

            //Parts a model only draws sometimes, like a horse's saddle
            if(face != null && !IRenderedPart.wasRendered(face.part)) continue;

            //Big flat parts like a chest would otherwise get growths wider than the mob's head
            float growthWidth = Math.min(GrowthPlacement.getFaceSize(bounds, side) * SIZE_TO_FACE, modelWidth * MAX_SIZE_TO_MOB);
            float growthScale = growthWidth / shape.width * sizeVariation * mobMultiplier;
            growthScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, growthScale));

            GlStateManager.pushMatrix();
            try {
                if(face != null) placement.applyPartTransform(face.part, scale);
                GlStateManager.translate(point[0] * scale, point[1] * scale, point[2] * scale);
                GrowthPlacement.rotateUpToNormal(side);
                //Sink tilted growths in a bit, so the raised edge of the base doesn't leave a gap
                float tilt = (float) Math.toRadians(Math.max(Math.abs(tiltX), Math.abs(tiltZ)));
                GlStateManager.translate(0.0F, -MathHelper.sin(tilt) * shape.width * 0.5F * growthScale / 16.0F, 0.0F);
                GlStateManager.rotate(spin, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(tiltX, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(tiltZ, 0.0F, 0.0F, 1.0F);
                GlStateManager.scale(growthScale * stretchX, growthScale * stretchY, growthScale * stretchZ);

                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
                shape.render(entity.getEntityId() * MAX_GROWTHS + i, speed, phaseTicks, partialTicks);
            }
            catch(RuntimeException e) {
                //A broken growth file would otherwise stop the whole mob from rendering, every frame
                if(!loggedError) {
                    loggedError = true;
                    WakingNights.LOGGER.error("Couldn't render a growth, check its geo and animation files", e);
                }
                return;
            }
            finally {
                GlStateManager.popMatrix();
            }
        }
    }

    private static float getDistanceToClosest(float[] point, List<float[]> others) {
        float closest = Float.MAX_VALUE;
        for(float[] other : others) {
            float dx = point[0] - other[0], dy = point[1] - other[1], dz = point[2] - other[2];
            closest = Math.min(closest, dx * dx + dy * dy + dz * dz);
        }
        return closest;
    }

    /** True makes RenderLivingBase tint this layer like the mob itself, so the growths flash red when it's hit. */
    @Override
    public boolean shouldCombineTextures() {
        return true;
    }

    /** The hitbox in model space and pixels, the same space a ModelBox uses: feet at y 24, y pointing down. */
    private static float[] getHitboxBounds(EntityLivingBase entity) {
        float halfWidth = entity.width * 8.0F;
        float height = entity.height * 16.0F;
        return new float[]{-halfWidth, 24.0F - height, -halfWidth, halfWidth, 24.0F, halfWidth};
    }

    private static float getSizeMultiplier(EntityLivingBase entity) {
        String[] configured = ForgeConfigHandler.client.growthSizeMultipliers;
        //The config screen replaces the array when it's changed
        if(configured != parsedMultipliers) {
            Map<String, Float> parsed = new HashMap<>();
            for(String entry : configured) {
                String[] split = entry.split("=");
                if(split.length != 2) continue;
                try {
                    parsed.put(split[0].trim(), Float.parseFloat(split[1].trim()));
                }
                catch(NumberFormatException e) {
                    WakingNights.LOGGER.warn("Invalid growth size multiplier: {}", entry);
                }
            }
            sizeMultipliers = parsed;
            parsedMultipliers = configured;
        }
        ResourceLocation id = EntityList.getKey(entity);
        return id == null ? 1.0F : sizeMultipliers.getOrDefault(id.toString(), 1.0F);
    }

    /** Blooded: growth_01.png, growth_02.png, ..., blighted: blight_01.png, ..., up to the first one that's missing. */
    private static List<ResourceLocation> getTextures(SpecialType type) {
        return TEXTURES.computeIfAbsent(type, t -> {
            String prefix = t == SpecialType.BLIGHTED ? "blight" : "growth";
            List<ResourceLocation> found = new ArrayList<>();
            IResourceManager resources = Minecraft.getMinecraft().getResourceManager();
            for(int i = 1; i <= 64; i++) {
                ResourceLocation texture = new ResourceLocation(WakingNights.MODID, String.format("textures/entity/growth/%s_%02d.png", prefix, i));
                try {
                    resources.getResource(texture).close();
                    found.add(texture);
                }
                catch(IOException e) {
                    break;
                }
            }
            if(found.isEmpty()) found.add(FALLBACK_TEXTURE);
            return found;
        });
    }

    static <T extends IAnimatable> AnimationEvent<T> newEvent(T animatable, float partialTicks) {
        return new AnimationEvent<>(animatable, 0.0F, 0.0F, partialTicks, false, Collections.emptyList());
    }
}
