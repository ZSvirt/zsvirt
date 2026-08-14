package org.zstack.header;

import java.util.Map;

/**
 * Created by xing5 on 2017/3/8.
 */
public interface HasThreadContext {
    default Map<String, String> getThreadContext() {
        return null;
    }

    default Map<Object, Object> getTaskContext() {
        return null;
    }
}
