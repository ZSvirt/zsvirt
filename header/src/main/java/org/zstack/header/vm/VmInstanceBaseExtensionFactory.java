package org.zstack.header.vm;

import java.util.List;

/**
 * Created by xing5 on 2016/10/31.
 */
public interface VmInstanceBaseExtensionFactory {
    VmInstance getVmInstance(VmInstanceVO vo);

    List<Class> getMessageClasses();
}
