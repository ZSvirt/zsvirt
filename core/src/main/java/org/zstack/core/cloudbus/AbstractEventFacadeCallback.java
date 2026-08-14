package org.zstack.core.cloudbus;

import org.zstack.core.Platform;

/**
 * Created by xing5 on 2016/6/26.
 */
public abstract class AbstractEventFacadeCallback {
    protected String uniqueIdentity = Platform.getUuid();
}
