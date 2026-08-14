package org.zstack.compute.affinityGroup;

/**
 * @Author: DaoDao
 * @Date: 2022/12/5
 */
public interface VmRelationAffinityGroupExtensionPoint {
    void afterAddVmToAffinityGroup(String agUuid, String vmUuid);

    void afterRemoveVmFromAffinityGroup(String agUuid, String vmUuid);
}
