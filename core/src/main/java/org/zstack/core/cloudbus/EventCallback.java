package org.zstack.core.cloudbus;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:33 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class EventCallback<T> extends AbstractEventFacadeCallback {
    abstract protected void run(Map<String, String> tokens, T data);
}
