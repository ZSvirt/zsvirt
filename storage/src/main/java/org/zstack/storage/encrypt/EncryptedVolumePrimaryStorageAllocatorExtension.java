package org.zstack.storage.encrypt;

import org.zstack.core.db.Q;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_;
import org.zstack.header.storage.primary.PrimaryStorageFeature;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmAllocatePrimaryStorageExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.storage.primary.PrimaryStorageFeatureAllocatorExtensionPoint;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EncryptedVolumePrimaryStorageAllocatorExtension implements VmAllocatePrimaryStorageExtensionPoint,
        PrimaryStorageFeatureAllocatorExtensionPoint {
    private static final String ZHPS_PRIMARY_STORAGE_IDENTITY = "expon";
    private static final String ZHPS_PRIMARY_STORAGE_PROTOCOL = VolumeProtocol.Vhost.name();

    @Override
    public List<PrimaryStorageVO> allocatePrimaryStorage(Set<PrimaryStorageFeature> requiredFeatures,
                                                         List<PrimaryStorageVO> candidates) {
        if (!requiredFeatures.contains(PrimaryStorageFeature.ENCRYPTED_VOLUME) || candidates.isEmpty()) {
            return candidates;
        }

        List<String> psUuids = candidates.stream().map(PrimaryStorageVO::getUuid).collect(Collectors.toList());
        Set<String> unsupportedPsUuids = new HashSet<>(getEncryptedVolumeUnsupportedPrimaryStorageUuids(psUuids));
        candidates.removeIf(ps -> unsupportedPsUuids.contains(ps.getUuid()));
        return candidates;
    }

    @Override
    public void filterPrimaryStorageCandidates(VmInstanceSpec spec, List<String> rootPrimaryStorageUuids,
                                               boolean rootPrimaryStorageAutoAllocation) {
        if (!rootPrimaryStorageAutoAllocation || spec.getRootDisk() == null
                || !Boolean.TRUE.equals(spec.getRootDisk().getEncrypted()) || rootPrimaryStorageUuids.isEmpty()) {
            return;
        }

        Set<String> unsupportedPsUuids = new HashSet<>(
                getEncryptedVolumeUnsupportedPrimaryStorageUuids(rootPrimaryStorageUuids));
        rootPrimaryStorageUuids.removeIf(unsupportedPsUuids::contains);
    }

    public boolean isEncryptedVolumeUnsupportedPrimaryStorage(String psUuid) {
        return psUuid != null && !getEncryptedVolumeUnsupportedPrimaryStorageUuids(
                Collections.singletonList(psUuid)).isEmpty();
    }

    private List<String> getEncryptedVolumeUnsupportedPrimaryStorageUuids(List<String> psUuids) {
        return Q.New(ExternalPrimaryStorageVO.class)
                .select(ExternalPrimaryStorageVO_.uuid)
                .eq(ExternalPrimaryStorageVO_.identity, ZHPS_PRIMARY_STORAGE_IDENTITY)
                .eq(ExternalPrimaryStorageVO_.defaultProtocol, ZHPS_PRIMARY_STORAGE_PROTOCOL)
                .in(ExternalPrimaryStorageVO_.uuid, psUuids)
                .listValues();
    }
}
