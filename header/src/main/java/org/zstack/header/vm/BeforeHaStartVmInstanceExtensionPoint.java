package org.zstack.header.vm;

import org.zstack.header.core.Completion;

import java.util.List;

/**
 * Created by lining on 2017/7/6.
 */
public interface BeforeHaStartVmInstanceExtensionPoint {
    void beforeHaStartVmInstance(String vmUuid, String judgerClassName, List<String> softAvoidHostUuids, Completion completion);
}
