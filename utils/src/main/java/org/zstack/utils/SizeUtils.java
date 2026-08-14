package org.zstack.utils;

import org.zstack.utils.data.NumberUtils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.data.UnitNumber;

import java.util.Arrays;
import java.util.Collection;

/**
 */
public class SizeUtils {
    private static final String T_SUFFIX = "T";
    private static final String TB_SUFFIX = "TB";
    private static final String TIB_SUFFIX = "TiB";
    private static final String t_SUFFIX = "t";
    private static final String G_SUFFIX = "G";
    private static final String GB_SUFFIX = "GB";
    private static final String GIB_SUFFIX = "GiB";
    private static final String g_SUFFIX = "g";
    private static final String M_SUFFIX = "M";
    private static final String MB_SUFFIX = "MB";
    private static final String MIB_SUFFIX = "MiB";
    private static final String m_SUFFIX = "m";
    private static final String K_SUFFIX = "K";
    private static final String KB_SUFFIX = "KB";
    private static final String KIB_SUFFIX = "KiB";
    private static final String k_SUFFIX = "k";
    private static final String B_SUFFIX = "B";
    private static final String b_SUFFIX = "b";

    /**
     * <p>Check string is a valid size string with 2 characters size units</p>
     * <p>Valid size units list below:
     * {@link #TB_SUFFIX}, {@link #GB_SUFFIX}, {@link #MB_SUFFIX}, {@link #KB_SUFFIX}, {@link #B_SUFFIX}</p>
     * <p>Example:
     *   <li>1GB -> true
     *   <li>2MB -> true
     *   <li>3K -> false
     *   <li>4T -> false
     *   <li>5 -> false
     *   <li>-6GB -> false
     *   </li>
     * </p>
     * 
     * @param str a string contains integer and size units
     * @return if str is a valid size string with 2 characters size units.
     * @see #isSizeString(String) 
     */
    public static boolean isSizeString2(String str) {
        final UnitNumber numeric = NumberUtils.ofUnitNumber(str);
        return numeric == null ? false : isSize2(numeric);
    }

    /**
     * <p>Check string is a valid size string with 1 character size units,
     * or simply the positive value</p>
     * <p>Valid size units list below:
     * {@link #T_SUFFIX}, {@link #t_SUFFIX}, {@link #G_SUFFIX}, {@link #g_SUFFIX}, 
     * {@link #M_SUFFIX}, {@link #m_SUFFIX}, {@link #K_SUFFIX}, {@link #k_SUFFIX},
     * {@link #B_SUFFIX}, {@link #b_SUFFIX}, ""</p>
     * <p>Example:
     *   <li>1GB -> false
     *   <li>2MB -> false
     *   <li>3K -> true
     *   <li>4T -> true
     *   <li>5 -> true
     *   <li>-6G -> false
     *   </li>
     * </p>
     * 
     * @param str a string contains integer and size units
     * @return if str is a valid size string with 1 character size units, or simply the positive value
     * @see #isSizeString2(String) 
     */
    public static boolean isSizeString(String str) {
        final UnitNumber numeric = NumberUtils.ofUnitNumber(str);
        return numeric == null ? false : isSize(numeric);
    }

    public static boolean isSize2(UnitNumber numeric) {
        return isSize(numeric, Arrays.asList(TB_SUFFIX, GB_SUFFIX, MB_SUFFIX, KB_SUFFIX, B_SUFFIX));
    }

    public static boolean isSize(UnitNumber numeric) {
        return isSize(numeric, Arrays.asList(
                T_SUFFIX, t_SUFFIX, G_SUFFIX, g_SUFFIX, M_SUFFIX, m_SUFFIX, K_SUFFIX, k_SUFFIX, B_SUFFIX, b_SUFFIX, ""));
    }

    public static boolean isSize(UnitNumber numeric, Collection<String> matchedUnits) {
        if (numeric.number < 0) {
            return false;
        }
        return matchedUnits.contains(numeric.units);
    }

    public static long sizeStringToBytes(String str) {
        final UnitNumber numeric = NumberUtils.ofUnitNumberOrThrow(str);
        String suffix = numeric.units;
        long size = numeric.number;
        if (suffix.length() == 0) {
            return size;
        }

        switch (suffix) {
        case T_SUFFIX: case t_SUFFIX: case TB_SUFFIX: case TIB_SUFFIX:
            return SizeUnit.TERABYTE.toByte(size);
        case G_SUFFIX: case g_SUFFIX: case GB_SUFFIX: case GIB_SUFFIX:
            return SizeUnit.GIGABYTE.toByte(size);
        case M_SUFFIX: case m_SUFFIX: case MB_SUFFIX: case MIB_SUFFIX:
            return SizeUnit.MEGABYTE.toByte(size);
        case K_SUFFIX: case k_SUFFIX: case KB_SUFFIX: case KIB_SUFFIX:
            return SizeUnit.KILOBYTE.toByte(size);
        case B_SUFFIX: case b_SUFFIX:
            return SizeUnit.BYTE.toByte(size);
        }

        throw new RuntimeException("should not be here," + str);
    }
}
