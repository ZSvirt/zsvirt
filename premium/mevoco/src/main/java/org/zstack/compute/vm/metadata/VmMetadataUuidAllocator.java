package org.zstack.compute.vm.metadata;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.storage.snapshot.VolumeSnapshotEO;
import org.zstack.header.storage.snapshot.VolumeSnapshotEO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeEO;
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeEO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceTreeVO;
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceTreeVO_;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceEO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class VmMetadataUuidAllocator {
    private static final int DEFAULT_MAX_ATTEMPTS = 100;

    public enum ResourceType {
        VM,
        VOLUME,
        SNAPSHOT,
        SNAPSHOT_TREE,
        SNAPSHOT_GROUP,
        SNAPSHOT_REFERENCE_TREE
    }

    private final Supplier<String> candidateSupplier;
    private final BiPredicate<ResourceType, String> databaseConflictChecker;
    private final int maxAttempts;
    private final Set<String> reservedUuids = new LinkedHashSet<>();

    public VmMetadataUuidAllocator(Supplier<String> candidateSupplier,
                                   BiPredicate<ResourceType, String> databaseConflictChecker,
                                   int maxAttempts) {
        if (candidateSupplier == null) {
            throw new IllegalArgumentException("candidateSupplier cannot be null");
        }
        if (databaseConflictChecker == null) {
            throw new IllegalArgumentException("databaseConflictChecker cannot be null");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be greater than zero");
        }

        this.candidateSupplier = candidateSupplier;
        this.databaseConflictChecker = databaseConflictChecker;
        this.maxAttempts = maxAttempts;
    }

    public static VmMetadataUuidAllocator databaseBacked() {
        return databaseBacked(Platform::getUuid);
    }

    public static VmMetadataUuidAllocator databaseBacked(Supplier<String> candidateSupplier) {
        return new VmMetadataUuidAllocator(
                candidateSupplier,
                VmMetadataUuidAllocator::uuidExistsInDatabase,
                DEFAULT_MAX_ATTEMPTS);
    }

    public void reserveAll(Collection<String> uuids) {
        if (uuids == null) {
            return;
        }

        for (String uuid : uuids) {
            if (uuid != null && !uuid.trim().isEmpty()) {
                reservedUuids.add(uuid);
            }
        }
    }

    public String allocate(ResourceType resourceType) {
        if (resourceType == null) {
            throw new IllegalArgumentException("resourceType cannot be null");
        }

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String candidate = candidateSupplier.get();
            if (candidate == null || candidate.trim().isEmpty()) {
                continue;
            }
            if (reservedUuids.contains(candidate) || databaseConflictChecker.test(resourceType, candidate)) {
                continue;
            }

            reservedUuids.add(candidate);
            return candidate;
        }

        throw new IllegalStateException(String.format(
                "failed to allocate a non-conflicting UUID after %d attempts", maxAttempts));
    }

    private static boolean uuidExistsInDatabase(ResourceType resourceType, String uuid) {
        if (Q.New(ResourceVO.class).eq(ResourceVO_.uuid, uuid).isExists()) {
            return true;
        }

        switch (resourceType) {
            case VM:
                return Q.New(VmInstanceEO.class).eq(VmInstanceEO_.uuid, uuid).isExists();
            case VOLUME:
                return !SQL.New("SELECT vo.uuid FROM VolumeEO vo WHERE vo.uuid = :uuid", String.class)
                        .param("uuid", uuid)
                        .list().isEmpty();
            case SNAPSHOT:
                return Q.New(VolumeSnapshotEO.class).eq(VolumeSnapshotEO_.uuid, uuid).isExists()
                        || Q.New(VolumeSnapshotGroupRefVO.class)
                        .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, uuid)
                        .isExists();
            case SNAPSHOT_TREE:
                return Q.New(VolumeSnapshotTreeEO.class).eq(VolumeSnapshotTreeEO_.uuid, uuid).isExists();
            case SNAPSHOT_GROUP:
                return Q.New(VolumeSnapshotGroupVO.class).eq(VolumeSnapshotGroupVO_.uuid, uuid).isExists();
            case SNAPSHOT_REFERENCE_TREE:
                return Q.New(VolumeSnapshotReferenceTreeVO.class)
                        .eq(VolumeSnapshotReferenceTreeVO_.uuid, uuid)
                        .isExists();
            default:
                throw new IllegalArgumentException("unsupported metadata resource type: " + resourceType);
        }
    }
}
