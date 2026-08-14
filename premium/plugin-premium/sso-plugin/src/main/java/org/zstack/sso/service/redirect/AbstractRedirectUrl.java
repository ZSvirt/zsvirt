package org.zstack.sso.service.redirect;

import org.zstack.core.Platform;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: DaoDao
 * @Date: 2022/9/6
 */
public abstract class AbstractRedirectUrl {
    public static final String REDIRECT_SCHEMA = "schema";
    public static final String REDIRECT_IP = "ip";
    public static final String REDIRECT_PORT = "port";


    public static Map<String, Object> defaultRedirectUrlMap = new HashMap<>();

    static {
        defaultRedirectUrlMap.put(REDIRECT_SCHEMA, "http");
        defaultRedirectUrlMap.put(REDIRECT_IP, Platform.getCanonicalServerIp());
        defaultRedirectUrlMap.put(REDIRECT_PORT, "5000");
    }
}
