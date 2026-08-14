package org.zstack.compute.vm.numa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.zstack.compute.vm.MevocoVmSystemTags;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.host.HostNumaNodeVO;
import org.zstack.header.host.HostNumaNodeVO_;
import org.zstack.header.vm.VmInstanceVO;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.zstack.core.Platform.i18m;

public class VmNumaFilterFlow implements HostAllocatorFilterExtensionPoint {

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        String vmUuid = spec.getVmInstance().getUuid();
        if (!MevocoVmSystemTags.VM_NUMA_ENABLE.hasTag(vmUuid)) {
            return;
        }
        VmInstanceVO pvm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        if (pvm.getHostUuid() == null) {
            return;
        }
        Set<String> currentHostNumaNodeMd5Set = getHostNumaNodeMd5Set(pvm.getHostUuid());
        for (HostCandidate candidate : candidates) {
            if (!getHostNumaNodeMd5Set(candidate.getUuid()).equals(currentHostNumaNodeMd5Set)) {
                candidate.markAsRejected(getClass(), i18m("vNuma vms need to be created on hosts with the same numa"));
            }
        }
    }

    private Set<String> getHostNumaNodeMd5Set(String hostUuid) {
        Set<String> hostNumaNodeMd5Set = new TreeSet<>();
        List<HostNumaNodeVO> hostNumaNodeVOs = Q.New(HostNumaNodeVO.class).eq(HostNumaNodeVO_.hostUuid, hostUuid).list();
        for (HostNumaNodeVO hostNumaNode : hostNumaNodeVOs) {
            hostNumaNodeMd5Set.add(DigestUtils.md5DigestAsHex((hostNumaNode.getNodeID() + hostNumaNode.getNodeCPUs()).getBytes()));
        }
        return hostNumaNodeMd5Set;
    }
}
