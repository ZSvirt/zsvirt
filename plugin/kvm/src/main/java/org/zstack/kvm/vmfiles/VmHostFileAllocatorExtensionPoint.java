package org.zstack.kvm.vmfiles;

import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.core.db.Q;

import com.google.common.base.Objects;

import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18m;

public class VmHostFileAllocatorExtensionPoint implements HostAllocatorFilterExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmHostFileAllocatorExtensionPoint.class);

    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        if (spec == null) {
            return;
        }

        String vmOperation = spec.getVmOperation();
        if (vmOperation == null || !VmInstanceConstant.VmOperation.Start.toString().equals(vmOperation)) {
            return;
        }

        if (spec.getVmInstance() == null) {
            return;
        }

        String vmUuid = spec.getVmInstance().getUuid();
        String lastHostUuid = spec.getVmInstance().getLastHostUuid();

        List<VmHostFileType> files = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                .eq(VmHostFileVO_.hostUuid, lastHostUuid)
                .select(VmHostFileVO_.type)
                .notNull(VmHostFileVO_.changeDate)
                .listValues();
        if (files.isEmpty()) {
            return;
        }

        String types = files.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));

        for (HostCandidate c : candidates) {
            if (!Objects.equal(lastHostUuid, c.getUuid())) {
                c.markAsRejected(getClass(),
                        i18m("only allowed start on last host: unsynchronized %s VM host files exist", types));
            }
        }
    }
}
