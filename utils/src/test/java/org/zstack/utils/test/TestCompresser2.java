package org.zstack.utils.test;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.zstack.utils.Compresser;

import java.io.File;
import java.io.IOException;

public class TestCompresser2 {
    @Test
    public void test() throws IOException {
        String path = System.getProperty("file");
        String outpath = System.getProperty("out");
        File f = new File(path);
        File outFile = new File(outpath);
        String str = FileUtils.readFileToString(f);
        byte[] ret = Compresser.deflate(str.getBytes());
        System.out.println(String.format("before delfating: %s bytes", str.getBytes().length));
        System.out.println(String.format("after delfating: %s bytes", ret.length));
        byte[] outbyte = Compresser.inflate(ret);
        FileUtils.writeStringToFile(outFile, new String(outbyte));
    }
}
