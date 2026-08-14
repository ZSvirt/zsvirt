package org.zstack.storage.ceph;

import org.zstack.storage.ceph.backup.CephBackupStorageBase;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lining on 2018/11/7.
 */
public class CephCapacity {
    private String fsid;
    private Long totalCapacity;
    private Long availableCapacity;
    private final List<CephPoolCapacity> poolCapacities = new ArrayList<>();
    private String cephManufacturer;

    public CephCapacity() {

    }

    public CephCapacity(String fsid, CephPrimaryStorageBase.AgentResponse rsp) {
        this.fsid = fsid;
        this.availableCapacity = rsp.getAvailableCapacity();
        this.totalCapacity = rsp.getTotalCapacity();
        this.setPoolCapacities(rsp.getPoolCapacities());
        this.cephManufacturer = rsp.getType();
    }

    public CephCapacity(String fsid, CephBackupStorageBase.AgentResponse rsp) {
        this.fsid = fsid;
        this.availableCapacity = rsp.getAvailableCapacity();
        this.totalCapacity = rsp.getTotalCapacity();
        this.setPoolCapacities(rsp.getPoolCapacities());
        this.cephManufacturer = rsp.getType();
    }

    public Long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public Long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(Long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public List<CephPoolCapacity> getPoolCapacities() {
        return poolCapacities;
    }

    public void setPoolCapacities(List<CephPoolCapacity> poolCapacities) {
        this.poolCapacities.clear();
        if (poolCapacities != null) {
            this.poolCapacities.addAll(poolCapacities);
        }
    }

    public boolean isXsky() {
        return CephConstants.CEPH_MANUFACTURER_XSKY.equals(cephManufacturer);
    }

    public boolean isEnterpriseCeph() {
        return CephConstants.CEPH_MANUFACTURER_XSKY.equals(cephManufacturer) ||
                CephConstants.CEPH_MANUFACTURER_SANDSTONE.equals(cephManufacturer);
    }

    public String getCephManufacturer() {
        return cephManufacturer;
    }

    public void setCephManufacturer(String cephManufacturer) {
        this.cephManufacturer = cephManufacturer;
    }

    public String getFsid() {
        return fsid;
    }

    public void setFsid(String fsid) {
        this.fsid = fsid;
    }
}
