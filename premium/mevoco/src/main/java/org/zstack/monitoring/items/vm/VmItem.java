package org.zstack.monitoring.items.vm;

import org.zstack.header.vm.VmInstanceVO;
import org.zstack.monitoring.items.Item;

/**
 * Created by xing5 on 2017/6/2.
 */
public interface VmItem extends Item {
    default String getResourceType() {
        return VmInstanceVO.class.getSimpleName();
    }
}
