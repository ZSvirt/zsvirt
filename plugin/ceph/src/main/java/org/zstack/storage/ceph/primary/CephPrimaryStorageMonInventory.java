package org.zstack.storage.ceph.primary;

import org.zstack.header.log.NoLogging;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/28/2015.
 */
@Inventory(mappingVOClass = CephPrimaryStorageMonVO.class)
public class CephPrimaryStorageMonInventory implements Serializable {
    private String hostname;
    private Integer monPort;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String primaryStorageUuid;
    private String monAddr;
    private String sshUsername;
    @NoLogging
    private String sshPassword;
    private Integer sshPort;
    private String status;
    private String monUuid;

    public static CephPrimaryStorageMonInventory valueOf(CephPrimaryStorageMonVO vo) {
        CephPrimaryStorageMonInventory inv = new CephPrimaryStorageMonInventory();
        inv.setHostname(vo.getHostname());
        inv.setMonPort(vo.getMonPort());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        inv.setSshPort(vo.getSshPort());
        inv.setSshUsername(vo.getSshUsername());
        inv.setSshPassword(vo.getSshPassword());
        inv.setMonUuid(vo.getUuid());
        inv.setStatus(vo.getStatus().toString());
        inv.setMonAddr(vo.getMonAddr());
        return inv;
    }

    public static List<CephPrimaryStorageMonInventory> valueOf(Collection<CephPrimaryStorageMonVO> vos) {
        List<CephPrimaryStorageMonInventory> invs = new ArrayList<CephPrimaryStorageMonInventory>();
        for (CephPrimaryStorageMonVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getMonAddr() {
        return monAddr;
    }

    public void setMonAddr(String monAddr) {
        this.monAddr = monAddr;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }
    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }
    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public Integer getMonPort() {
        return monPort;
    }

    public void setMonPort(Integer monPort) {
        this.monPort = monPort;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
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

    public String getMonUuid() {
        return monUuid;
    }

    public void setMonUuid(String monUuid) {
        this.monUuid = monUuid;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
