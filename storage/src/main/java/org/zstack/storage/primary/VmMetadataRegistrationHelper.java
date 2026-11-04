package org.zstack.storage.primary;

import org.zstack.header.storage.primary.VmMetadataRegistrationStatus;
import org.zstack.header.storage.primary.VmMetadataScanEntry;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Applies database facts to scan entries without removing files or changing registration state. */
final class VmMetadataRegistrationHelper {
    static void enrich(List<VmMetadataScanEntry> entries, Map<String, String> existingVmRootPrimaryStorageUuids,
                       String scannedPrimaryStorageUuid) {
        for (VmMetadataScanEntry entry : entries) {
            if (entry.getVmUuid() == null || entry.getVmUuid().isEmpty()) {
                continue;
            }

            if (VmMetadataRegistrationStatus.UUID_CONFLICT.name().equals(entry.getRegistrationStatus())) {
                // Storage-specific scans can identify conflicts beyond primary-storage ownership,
                // such as an independent LocalStorage copy on another host.
                continue;
            }

            String existingPrimaryStorageUuid = existingVmRootPrimaryStorageUuids.get(entry.getVmUuid());
            if (existingPrimaryStorageUuid == null) {
                entry.setRegistrationStatus(VmMetadataRegistrationStatus.UNREGISTERED.name());
            } else if (Objects.equals(existingPrimaryStorageUuid, scannedPrimaryStorageUuid)) {
                entry.setRegistrationStatus(VmMetadataRegistrationStatus.REGISTERED.name());
            } else {
                entry.setRegistrationStatus(VmMetadataRegistrationStatus.UUID_CONFLICT.name());
            }
        }
    }
}
