package wakingnights.capability;

/**
 * What kind of special an entity is. Saved and synced by ordinal, so only add new types at the end.
 */
public enum SpecialType {
    NONE,
    BLOODED,
    BLIGHTED;

    public static SpecialType byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : NONE;
    }
}
