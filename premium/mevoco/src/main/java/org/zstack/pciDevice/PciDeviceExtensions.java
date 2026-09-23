package org.zstack.pciDevice;

import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.Q;
import org.zstack.header.sriov.VmVfNicVO;
import org.zstack.header.sriov.VmVfNicVO_;
import org.zstack.header.vdpa.VmVdpaNicVO;
import org.zstack.header.vdpa.VmVdpaNicVO_;
import org.zstack.header.vm.VmMigrateHostConstraintExtensionPoint;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PciDeviceExtensions implements
        VmMigrateHostConstraintExtensionPoint {

    @Override
    @Transactional(readOnly = true)
    public List<String> getHostBoundVmUuids(@NonNull List<String> candidateVmUuids) {
        if (candidateVmUuids.isEmpty()) {
            return candidateVmUuids;
        }

        Set<String> hostBoundVmUuids = new HashSet<>();
        List<Tuple> pciDevices = Q.New(PciDeviceVO.class)
                .in(PciDeviceVO_.vmInstanceUuid, candidateVmUuids)
                .select(PciDeviceVO_.uuid, PciDeviceVO_.vmInstanceUuid)
                .listTuple();
        if (pciDevices.isEmpty()) {
            return new ArrayList<>(hostBoundVmUuids);
        }

        Set<String> nicPciDeviceUuids = new HashSet<>();
        nicPciDeviceUuids.addAll(Q.New(VmVfNicVO.class)
                .in(VmVfNicVO_.vmInstanceUuid, candidateVmUuids)
                .select(VmVfNicVO_.pciDeviceUuid)
                .listValues());
        nicPciDeviceUuids.addAll(Q.New(VmVdpaNicVO.class)
                .in(VmVdpaNicVO_.vmInstanceUuid, candidateVmUuids)
                .select(VmVdpaNicVO_.pciDeviceUuid)
                .listValues());

        pciDevices.stream()
                .filter(pci -> !nicPciDeviceUuids.contains(pci.get(0, String.class)))
                .map(pci -> pci.get(1, String.class))
                .forEach(hostBoundVmUuids::add);

        return new ArrayList<>(hostBoundVmUuids);
    }
}
