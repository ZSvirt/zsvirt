package org.zstack.storage.ceph.primary;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = CephOsdGroupVO.class, collectionValueOfMethod = "valueOf1")
public class CephOsdGroupInventory implements Serializable {
    private String primaryStorageUuid;
    private String osds;
    private long availableCapacity;
    private long availablePhysicalCapacity;
    private long totalPhysicalCapacity;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String uuid;

    protected CephOsdGroupInventory(CephOsdGroupVO vo) {
        this.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        this.setOsds(vo.getOsds());
        this.setAvailableCapacity(vo.getAvailableCapacity());
        this.setAvailablePhysicalCapacity(vo.getAvailablePhysicalCapacity());
        this.setTotalPhysicalCapacity(vo.getTotalPhysicalCapacity());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setUuid(vo.getUuid());
    }

    public static CephOsdGroupInventory valueOf(CephOsdGroupVO vo) {
        return new CephOsdGroupInventory(vo);
    }

    public static List<CephOsdGroupInventory> valueOf1(Collection<CephOsdGroupVO> vos) {
        List<CephOsdGroupInventory> invs = new ArrayList<CephOsdGroupInventory>(vos.size());
        for (CephOsdGroupVO vo : vos) {
            invs.add(CephOsdGroupInventory.valueOf(vo));
        }
        return invs;
    }

    public CephOsdGroupInventory() {
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String $paramName) {
        primaryStorageUuid = $paramName;
    }

    public String getOsds() {
        return osds;
    }

    public void setOsds(String $paramName) {
        osds = $paramName;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public long getAvailablePhysicalCapacity() {
        return availablePhysicalCapacity;
    }

    public void setAvailablePhysicalCapacity(long $paramName) {
        availablePhysicalCapacity = $paramName;
    }

    public long getTotalPhysicalCapacity() {
        return totalPhysicalCapacity;
    }

    public void setTotalPhysicalCapacity(long $paramName) {
        totalPhysicalCapacity = $paramName;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp $paramName) {
        createDate = $paramName;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp $paramName) {
        lastOpDate = $paramName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String $paramName) {
        uuid = $paramName;
    }
}
