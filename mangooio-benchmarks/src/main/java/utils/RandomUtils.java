package utils;

import interfaces.Constants;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

/**
 *
 * @author svenkubiak
 *
 */
public final class RandomUtils {
    private static XoRoShiRo128PlusRandom random = new XoRoShiRo128PlusRandom();

    private RandomUtils() {
    }

    public static int getRandomId() {
        return random.nextInt(Constants.ROWS - 1) + 1;
    }
}