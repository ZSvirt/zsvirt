package org.zstack.storage.snapshot;

import org.zstack.core.db.SQL;
import org.zstack.header.message.APIResourceScope;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.identity.rbac.AccessibleResourceChecker;

/** Read access inherited from the current owner of a snapshot group's VM. */
class VolumeSnapshotQueryAccess {
    static String includeOwnedVmSnapshots(Class<?> resourceType, String inventoryName,
                                          String primaryKey, String accountUuid, String resourceFilter) {
        String groups = "SELECT snapshotgroup.uuid FROM VolumeSnapshotGroupVO snapshotgroup" +
                " WHERE snapshotgroup.vmInstanceUuid IN (SELECT vmowner.resourceUuid FROM AccountResourceRefVO vmowner" +
                " WHERE vmowner.accountUuid = '" + accountUuid + "' AND vmowner.resourceType = 'VmInstanceVO' AND vmowner.type = 'Own')";
        String inheritedFilter;
        if (resourceType == VolumeSnapshotGroupVO.class) {
            inheritedFilter = String.format("%s.%s IN (%s)", inventoryName, primaryKey, groups);
        } else {
            inheritedFilter = String.format("%s.%s IN (SELECT snapshotref.volumeSnapshotUuid" +
                    " FROM VolumeSnapshotGroupRefVO snapshotref WHERE snapshotref.volumeSnapshotGroupUuid IN (%s))",
                    inventoryName, primaryKey, groups);
        }
        return String.format("(%s OR %s)", resourceFilter, inheritedFilter);
    }

    static boolean isSnapshotReadableByAccount(String snapshotUuid, String accountUuid) {
        if (AccessibleResourceChecker.forAccount(accountUuid).withResourceType(VolumeSnapshotVO.class)
                .isAccessible(snapshotUuid)) {
            return true;
        }

        String vmUuid = SQL.New("SELECT snapshotgroup.vmInstanceUuid FROM VolumeSnapshotGroupVO snapshotgroup," +
                " VolumeSnapshotGroupRefVO snapshotref, VolumeSnapshotVO snapshot" +
                " WHERE snapshotgroup.uuid = snapshotref.volumeSnapshotGroupUuid" +
                " AND snapshotref.volumeSnapshotUuid = snapshot.uuid AND snapshot.uuid = :snapshotUuid", String.class)
                .param("snapshotUuid", snapshotUuid)
                .find();
        return AccessibleResourceChecker.forAccount(accountUuid).withResourceType(VmInstanceVO.class)
                .withScope(APIResourceScope.MustOwner).isAccessible(vmUuid);
    }
}
