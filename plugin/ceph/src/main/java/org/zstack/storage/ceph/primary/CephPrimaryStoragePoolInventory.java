package org.zstack.storage.ceph.primary;

import org.zstack.header.search.Inventory;

import javax.persistence.Column;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2017/2/28.
 */
@Inventory(mappingVOClass = CephPrimaryStoragePoolVO.class)
public class CephPrimaryStoragePoolInventory {
    private String uuid;
    private String primaryStorageUuid;
    private String poolName;
    private String aliasName;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String type;
    private Long availableCapacity;
    private Long usedCapacity;
    private Long totalCapacity;
    private String securityPolicy;
    private Integer replicatedSize;
    private Float diskUtilization;

    public static CephPrimaryStoragePoolInventory valueOf(CephPrimaryStoragePoolVO vo) {
        CephPrimaryStoragePoolInventory inv = new CephPrimaryStoragePoolInventory();
        inv.uuid = vo.getUuid();
        inv.primaryStorageUuid = vo.getPrimaryStorageUuid();
        inv.poolName = vo.getPoolName();
        inv.description = vo.getDescription();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        inv.aliasName = vo.getAliasName();
        inv.type = vo.getType();
        inv.usedCapacity = vo.getUsedCapacity();
        inv.replicatedSize = vo.getReplicatedSize();
        inv.totalCapacity = vo.getTotalCapacity();
        inv.diskUtilization = vo.getDiskUtilization();
        inv.securityPolicy = vo.getSecurityPolicy();
        if (vo.getOsdGroup() != null) {
            inv.availableCapacity = vo.getOsdGroup().getAvailableCapacity();
        }
        return inv;
    }

    public static List<CephPrimaryStoragePoolInventory> valueOf(Collection<CephPrimaryStoragePoolVO> vos) {
        List<CephPrimaryStoragePoolInventory> invs = new ArrayList<>();
        for (CephPrimaryStoragePoolVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public Long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(Long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public Long getUsedCapacity() {
        return usedCapacity;
    }

    public void setUsedCapacity(Long usedCapacity) {
        this.usedCapacity = usedCapacity;
    }

    public Integer getReplicatedSize() {
        return replicatedSize;
    }

    public void setReplicatedSize(Integer replicatedSize) {
        this.replicatedSize = replicatedSize;
    }

    public Long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public String getSecurityPolicy() {
        return securityPolicy;
    }

    public void setSecurityPolicy(String securityPolicy) {
        this.securityPolicy = securityPolicy;
    }

    public Float getDiskUtilization() {
        return diskUtilization;
    }

    public void setDiskUtilization(Float diskUtilization) {
        this.diskUtilization = diskUtilization;
    }
}
