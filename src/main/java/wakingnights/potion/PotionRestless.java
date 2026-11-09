package wakingnights.potion;

/**
 * Restless status icon (amplifier = Restless level - 1): this player is somewhere, or wears something, that makes
 * sleeping add that many Waking Night levels. Does nothing itself; kept in sync by Restless.
 */
public class PotionRestless extends PotionWakingNight {

    public static final PotionRestless INSTANCE = new PotionRestless();

    private PotionRestless() {
        super("restless", 0x5C3B1F);
    }
}
