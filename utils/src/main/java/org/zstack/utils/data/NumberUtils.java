package org.zstack.utils.data;

import javax.annotation.Nullable;

/**
 * Created by Wenhao.Zhang on 23/05/18
 */
public class NumberUtils {
    public static boolean isPowerOf2(long number) {
        return number > 0 && (number & (number - 1)) == 0;
    }

    public static boolean isPositive(Number i){
        return i != null && i.doubleValue() > 0;
    }

    public static @Nullable UnitNumber ofUnitNumber(String text) {
        return UnitNumber.valueOfOrNull(text);
    }

    public static UnitNumber ofUnitNumberOrThrow(String text) throws NumberFormatException {
        return UnitNumber.valueOf(text);
    }
}
