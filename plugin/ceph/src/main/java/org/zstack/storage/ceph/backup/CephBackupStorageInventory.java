package org.zstack.storage.ceph.backup;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.storage.ceph.CephConstants;

import javax.persistence.Column;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/27/2015.
 */
@Inventory(mappingVOClass = CephBackupStorageVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = BackupStorageInventory.class, type = CephConstants.CEPH_BACKUP_STORAGE_TYPE)}
)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "mons", inventoryClass = CephBackupStorageMonInventory.class,
        foreignKey = "uuid", expandedInventoryKey = "backupStorageUuid")
})
public class CephBackupStorageInventory extends BackupStorageInventory {
    private List<CephBackupStorageMonInventory> mons = new ArrayList<CephBackupStorageMonInventory>();
    private String fsid;
    private String poolName;
    private Long poolAvailableCapacity;
    private Long poolUsedCapacity;
    private Integer poolReplicatedSize;
    private Float poolDiskUtilization;
    private String poolSecurityPolicy;

    public CephBackupStorageInventory(CephBackupStorageVO vo) {
        super(vo);
        mons = CephBackupStorageMonInventory.valueOf(vo.getMons());
        fsid = vo.getFsid();
        poolName = vo.getPoolName();
        poolAvailableCapacity = vo.getPoolAvailableCapacity();
        poolUsedCapacity = vo.getPoolUsedCapacity();
        poolReplicatedSize = vo.getPoolReplicatedSize();
        poolSecurityPolicy = vo.getPoolSecurityPolicy();
        poolDiskUtilization = vo.getPoolDiskUtilization();
    }

    public CephBackupStorageInventory() {
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getFsid() {
        return fsid;
    }

    public void setFsid(String fsid) {
        this.fsid = fsid;
    }

    public static CephBackupStorageInventory valueOf(CephBackupStorageVO vo) {
        return new CephBackupStorageInventory(vo);
    }

    public static List<CephBackupStorageInventory> valueOf1(Collection<CephBackupStorageVO> vos) {
        List<CephBackupStorageInventory> invs = new ArrayList<CephBackupStorageInventory>();
        for (CephBackupStorageVO vo : vos) {
            invs.add(new CephBackupStorageInventory(vo));
        }

        return invs;
    }

    public List<CephBackupStorageMonInventory> getMons() {
        return mons;
    }

    public void setMons(List<CephBackupStorageMonInventory> mons) {
        this.mons = mons;
    }

    public Long getPoolAvailableCapacity() {
        return poolAvailableCapacity;
    }

    public void setPoolAvailableCapacity(Long poolAvailableCapacity) {
        this.poolAvailableCapacity = poolAvailableCapacity;
    }

    public Long getPoolUsedCapacity() {
        return poolUsedCapacity;
    }

    public void setPoolUsedCapacity(Long poolUsedCapacity) {
        this.poolUsedCapacity = poolUsedCapacity;
    }

    public Integer getPoolReplicatedSize() {
        return poolReplicatedSize;
    }

    public void setPoolReplicatedSize(Integer poolReplicatedSize) {
        this.poolReplicatedSize = poolReplicatedSize;
    }

    public Float getPoolDiskUtilization() {
        return poolDiskUtilization;
    }

    public void setPoolDiskUtilization(Float poolDiskUtilization) {
        this.poolDiskUtilization = poolDiskUtilization;
    }

    public String getPoolSecurityPolicy() {
        return poolSecurityPolicy;
    }

    public void setPoolSecurityPolicy(String poolSecurityPolicy) {
        this.poolSecurityPolicy = poolSecurityPolicy;
    }
}
