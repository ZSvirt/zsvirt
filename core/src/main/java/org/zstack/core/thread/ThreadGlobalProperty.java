package org.zstack.core.thread;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class ThreadGlobalProperty {
    @GlobalProperty(name="ThreadFacade.maxThreadNum", defaultValue = "150")
    public static int MAX_THREAD_NUM;
}
