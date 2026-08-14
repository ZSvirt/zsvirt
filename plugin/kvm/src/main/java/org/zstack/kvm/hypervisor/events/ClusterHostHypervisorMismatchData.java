package org.zstack.kvm.hypervisor.events;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.NeedJsonSchema;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.hypervisor.datatype.KvmHostHypervisorMetadataVO;
import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoVO;

/**
 * Cluster host hypervisor version not match event data.
 * 
 * Cluster is used as the unit of event reporting instead of host
 * to reduce the frequency of event reporting.
 * 
 * Created by Wenhao.Zhang on 23/03/06
 */
@NeedJsonSchema
public class ClusterHostHypervisorMismatchData {
    public static final String PATH = "cluster/hypervisor/mismatch";
    /**
     * @see ClusterVO#getUuid()
     */
    private String clusterUuid;
    /**
     * @see KvmHypervisorInfoVO#getHypervisor()
     * @see KvmHostHypervisorMetadataVO#getHypervisor()
     * @see KVMConstant#VIRTUALIZER_QEMU_KVM
     */
    private String hypervisorType;
    /**
     * Total amount of host in the specific cluster whose hypervisor is expired
     */
    private int hostCount;

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public int getHostCount() {
        return hostCount;
    }

    public void setHostCount(int hostCount) {
        this.hostCount = hostCount;
    }
}
