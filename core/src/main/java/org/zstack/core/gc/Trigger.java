package org.zstack.core.gc;

import java.util.Map;

/**
 * Created by xing5 on 2017/3/3.
 */
public interface Trigger {
    boolean trigger(Map tokens, Object data);
}
