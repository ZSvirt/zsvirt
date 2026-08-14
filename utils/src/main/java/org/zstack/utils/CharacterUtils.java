package org.zstack.utils;

import org.apache.commons.lang.CharUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: fubang
 * @Date: 2018/6/25
 */
public class CharacterUtils {
    public static boolean checkCharacter(String s) {
        return s.codePoints().allMatch(code -> CharUtils.isAsciiPrintable((char) code));
    }

    public static boolean checkCharactersByRegex(String regex, String s) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(s);
        return matcher.matches();
    }
}
