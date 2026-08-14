package org.zstack.utils;

import java.io.IOException;
import java.net.ServerSocket;

public class WebUtils {
    public static Integer getFreePort(){
        Integer port = null;
        try {
            ServerSocket s = new ServerSocket(0);
            port = s.getLocalPort();
            s.close();
        } catch (IOException ignored) {}
        return port;
    }
}
