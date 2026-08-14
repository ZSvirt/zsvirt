package org.zstack.core.cloudbus;

import java.util.Map;

/**
 */
public abstract class AutoOffEventCallback<T> extends AbstractEventFacadeCallback {
    abstract protected boolean run(Map<String, String> tokens, T data);
}
