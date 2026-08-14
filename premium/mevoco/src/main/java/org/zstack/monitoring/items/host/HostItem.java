package org.zstack.monitoring.items.host;

import org.zstack.header.host.HostVO;
import org.zstack.monitoring.items.Item;

/**
 * Created by xing5 on 2017/6/2.
 */
public interface HostItem extends Item {
    default String getResourceType() {
        return HostVO.class.getSimpleName();
    }
}
