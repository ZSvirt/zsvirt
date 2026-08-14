package org.zstack.header.vm.metadata;

/**
 * Service interface for marking VM metadata as dirty.
 * <p>
 * Implementations live in the premium module (VmMetadataDirtyMarker).
 * The zstack core module uses this interface via {@code @Autowired(required = false)}
 * so that the feature degrades gracefully when the premium module is not loaded.
 */
public interface VmMetadataDirtyService {
    /**
     * Mark the VM's metadata as dirty so that it will be flushed
     * to primary storage on the next poll cycle.
     *
     * @param vmInstanceUuid the VM whose metadata changed
     * @return true if the mark was actually written (feature enabled), false otherwise
     */
    boolean markDirty(String vmInstanceUuid);

    /**
     * Mark the VM's metadata as dirty, optionally flagging a storage-structure change
     * (e.g. volume attach/detach, snapshot, migration).
     *
     * @param vmInstanceUuid         the VM whose metadata changed
     * @param storageStructureChange true if the change affects storage topology
     * @return true if the mark was actually written (feature enabled), false otherwise
     */
    boolean markDirty(String vmInstanceUuid, boolean storageStructureChange);
}
