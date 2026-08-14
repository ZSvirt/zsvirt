package org.zstack.compute.affinityGroup;

import org.zstack.header.affinitygroup.APIDeleteAffinityGroupMsg;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.compute.affinityGroup
 * @date 2021/2/22 2:50 PM
 */
public interface DeleteAffinityGroupExtensionPoint {
    void beforeDeleteAffinityGroup(String affinityGroupUuid);

    void afterDeleteAffinityGroup(String affinityGroupUuid);
}
