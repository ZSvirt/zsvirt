package org.zstack.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class Compresser {
    public static byte[] deflate(byte[] input) throws IOException {
        return deflate(input, 8192);
    }
    
    public static byte[] deflate(byte[] input, int bufferSize) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
        Deflater def = new Deflater();
        DeflaterOutputStream dos = new DeflaterOutputStream(out, def, bufferSize);
        dos.write(input, 0, input.length);
        dos.finish();
        dos.close();
        byte[] ret = out.toByteArray();
        out.close();
        return ret;
    }
    
    public static byte[] inflate(byte[] input, int bufferSize) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(input);
        Inflater inf = new Inflater();
        InflaterInputStream iis = new InflaterInputStream(in, inf, bufferSize);
        ByteArrayOutputStream out = new ByteArrayOutputStream(input.length * 5);
        for (int c = iis.read(); c != -1; c = iis.read()) {
            out.write(c);
        }
        in.close();
        iis.close();
        byte[] ret = out.toByteArray();
        out.close();
        return ret;
    }
    
    public static byte[] inflate(byte[] input) throws IOException {
        return inflate(input, 8192);
    }
}
