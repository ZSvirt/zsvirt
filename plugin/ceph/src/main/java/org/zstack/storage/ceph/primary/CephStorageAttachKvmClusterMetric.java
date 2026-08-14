package org.zstack.storage.ceph.primary;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephSystemTags;
import org.zstack.storage.primary.StorageAttachClusterMetric;

import static org.zstack.core.Platform.argerr;

public class CephStorageAttachKvmClusterMetric implements StorageAttachClusterMetric {

    @Override
    public void checkSupport(String psUuid, String clusterUuid) {
    }

    @Override
    public String getPrimaryStorageType() {
        return CephConstants.CEPH_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public String getClusterHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }
}