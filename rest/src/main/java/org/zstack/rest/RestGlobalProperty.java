package org.zstack.rest;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/12/9.
 */
@GlobalPropertyDefinition
public class RestGlobalProperty {
    @GlobalProperty(name="RestServer.maxCachedApiResults", defaultValue = "2000")
    public static int MAX_CACHED_API_RESULTS;

    @GlobalProperty(name="RestServer.rateLimits", defaultValue = "12000")
    public static int REST_RATE_LIMITS;
}
