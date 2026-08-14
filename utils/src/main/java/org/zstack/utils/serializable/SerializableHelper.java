package org.zstack.utils.serializable;

import java.io.*;

public class SerializableHelper {
    
    public static byte[] writeObject(Object obj) throws IOException {
        ByteArrayOutputStream bstream = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bstream);
        out.writeObject(obj);
        out.close();
        return bstream.toByteArray();
    }

    public static <T> T readObject(byte[] stream) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bstream = new ByteArrayInputStream(stream);
        ObjectInputStream in = new ObjectInputStream(bstream);
        T ret = (T)in.readObject();
        in.close();
        return ret;
    }

}
