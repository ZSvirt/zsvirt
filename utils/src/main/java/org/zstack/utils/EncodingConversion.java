package org.zstack.utils;

/**
 * Created by Administrator on 2017-04-01.
 */
public class EncodingConversion {
    public static String encodingToUnicode(String str){
        StringBuffer buf = new StringBuffer(str.length()*6);
        buf.setLength(0);

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            String tmp = Integer.toHexString(c + 0x10000).substring(1);
            buf.append("\\u").append(tmp);
        }
        return (new String(buf));
    }
}
