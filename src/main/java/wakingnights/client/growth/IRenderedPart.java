package wakingnights.client.growth;

/**
 * Added to every ModelRenderer (see ModelRendererMixin): which entity render last actually drew it, and for special
 * mobs where it was drawn. Lets growths skip parts a model only draws sometimes (like a horse's saddle) and sit exactly
 * on parts the model moves or shrinks itself (like a baby's).
 */
public interface IRenderedPart {
    int wakingnights$getRenderStamp();

    /** Loads the GL matrix the part was drawn with in the current entity render (before its own rotation), if known. */
    boolean wakingnights$loadRenderMatrix();

    static boolean wasRendered(Object part) {
        return ((IRenderedPart) part).wakingnights$getRenderStamp() == GrowthLayerHandler.getRenderStamp();
    }
}
