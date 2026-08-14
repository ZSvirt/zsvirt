package org.zstack.core.cloudbus;

/**
 * Created by xing5 on 2016/6/26.
 */
public abstract class EventRunnable extends AbstractEventFacadeCallback {
    abstract protected void run();
}
