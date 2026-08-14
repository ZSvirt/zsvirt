package org.zstack.utils;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created by frank on 3/3/2016.
 */
public class Digest {
    public static String getMD5(String filePath) {
        try {
            FileInputStream fis = new FileInputStream(new File(filePath));
            String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
            fis.close();
            return md5;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
