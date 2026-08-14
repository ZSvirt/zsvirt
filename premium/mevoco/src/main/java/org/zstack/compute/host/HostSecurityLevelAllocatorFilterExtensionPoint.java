package org.zstack.compute.host;

import org.zstack.compute.vm.MevocoVmSystemTags;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18m;
import static org.zstack.utils.CollectionUtils.*;

public class HostSecurityLevelAllocatorFilterExtensionPoint implements HostAllocatorFilterExtensionPoint {
    private CLogger logger = Utils.getLogger(HostSecurityLevelAllocatorFilterExtensionPoint.class);

    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        if (!MevocoGlobalConfig.ENABLE_SECURITY_LEVEL.value(Boolean.class)) {
            return;
        }

        String securityLevel = MevocoVmSystemTags.SECURITY_LEVEL.getTokenByResourceUuid(
                spec.getVmInstance().getUuid(), MevocoVmSystemTags.SECURITY_LEVEL_TOKEN);

        List<Tuple> tuples = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid, VmInstanceVO_.hostUuid)
                .in(VmInstanceVO_.hostUuid, transform(candidates, HostCandidate::getUuid))
                .listTuple();
        Map<String, List<Tuple>> hostTupleMap = groupBy(tuples, tuple -> tuple.get(1, String.class));

        for (HostCandidate host : candidates) {
            List<String> vmUuids = transform(
                    hostTupleMap.getOrDefault(host.getUuid(), Collections.emptyList()),
                    tuple -> tuple.get(0, String.class));
            if (isEmpty(vmUuids)) {
                continue;
            }

            List<String> securityLevelsOnHost = vmUuids.stream().filter(vmUuid -> MevocoVmSystemTags.SECURITY_LEVEL.hasTag(vmUuid)).map(vmUuid -> MevocoVmSystemTags.SECURITY_LEVEL.getTokenByResourceUuid(
                    vmUuid, MevocoVmSystemTags.SECURITY_LEVEL_TOKEN)).collect(Collectors.toList());

            if (securityLevelsOnHost.isEmpty() && securityLevel == null) {
                // no security level exists
                continue;
            }

            if (!securityLevelsOnHost.isEmpty() && securityLevel != null) {
                // current vm has security level but vms on host have security level
                if (!securityLevelsOnHost.contains(securityLevel)) {
                    host.markAsRejected(getClass(), i18m("vm security level not consistent with vms running on host"));
                }

                continue;
            }

            // if not either of the vms have security level, filter the host
            host.markAsRejected(getClass(),
                    i18m("security level on host are %s, but current vm has security level %s",
                            securityLevelsOnHost, securityLevel));
        }
    }
}
