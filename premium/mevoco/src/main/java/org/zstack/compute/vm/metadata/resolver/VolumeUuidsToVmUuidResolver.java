package org.zstack.compute.vm.metadata.resolver;

import org.zstack.core.db.Q;
import org.zstack.header.vm.metadata.VmUuidFromApiResolver;
import org.zstack.header.volume.VolumeAO_;
import org.zstack.header.volume.VolumeVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * List&lt;volumeUuid&gt; → VolumeVO.vmInstanceUuid (fallback lastVmInstanceUuid).
 *
 * <p>Batch-optimised: uses SQL IN instead of N+1 single queries.</p>
 */
public class VolumeUuidsToVmUuidResolver implements VmUuidFromApiResolver {

    @Override
    public String resolveVmUuid(String fieldValue) {
        if (fieldValue == null) {
            return null;
        }

        String vmUuid = Q.New(VolumeVO.class)
                .eq(VolumeAO_.uuid, fieldValue)
                .select(VolumeAO_.vmInstanceUuid)
                .findValue();

        if (vmUuid == null) {
            vmUuid = Q.New(VolumeVO.class)
                    .eq(VolumeAO_.uuid, fieldValue)
                    .select(VolumeAO_.lastVmInstanceUuid)
                    .findValue();
        }

        return vmUuid;
    }

    @Override
    public List<String> batchResolveVmUuids(List<String> fieldValues) {
        if (fieldValues == null || fieldValues.isEmpty()) {
            return Collections.emptyList();
        }

        // First pass: get vmInstanceUuid for all volumes
        List<String> vmUuids = Q.New(VolumeVO.class)
                .in(VolumeAO_.uuid, fieldValues)
                .select(VolumeAO_.vmInstanceUuid)
                .listValues();

        Set<String> result = vmUuids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Second pass: for volumes without vmInstanceUuid, try lastVmInstanceUuid
        List<String> lastVmUuids = Q.New(VolumeVO.class)
                .in(VolumeAO_.uuid, fieldValues)
                .isNull(VolumeAO_.vmInstanceUuid)
                .select(VolumeAO_.lastVmInstanceUuid)
                .listValues();
        lastVmUuids.stream()
                .filter(Objects::nonNull)
                .forEach(result::add);

        return new ArrayList<>(result);
    }
}
