package org.zstack.storage.ceph.backup;

import org.zstack.header.log.NoLogging;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/27/2015.
 */
@Inventory(mappingVOClass = CephBackupStorageMonVO.class)
public class CephBackupStorageMonInventory implements Serializable {
    private String hostname;
    private Integer monPort;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String backupStorageUuid;
    private String monAddr;
    private Integer sshPort;
    private String status;
    private String sshUsername;
    @NoLogging
    private String sshPassword;
    private String monUuid;

    public static CephBackupStorageMonInventory valueOf(CephBackupStorageMonVO vo) {
        CephBackupStorageMonInventory inv = new CephBackupStorageMonInventory();
        inv.setHostname(vo.getHostname());
        inv.setMonPort(vo.getMonPort());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setBackupStorageUuid(vo.getBackupStorageUuid());
        inv.setSshPort(vo.getSshPort());
        inv.setSshUsername(vo.getSshUsername());
        inv.setSshPassword(vo.getSshPassword());
        inv.setMonUuid(vo.getUuid());
        inv.setStatus(vo.getStatus().toString());
        inv.setMonAddr(vo.getMonAddr());
        return inv;
    }

    public static List<CephBackupStorageMonInventory> valueOf(Collection<CephBackupStorageMonVO> vos) {
        List<CephBackupStorageMonInventory> invs = new ArrayList<CephBackupStorageMonInventory>();
        for (CephBackupStorageMonVO vo : vos) {
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

    public String getSshUsername() {

        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }
    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }
    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public Integer getMonPort() {
        return monPort;
    }

    public void setMonPort(Integer monPort) {
        this.monPort = monPort;
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    public String getMonUuid() {
        return monUuid;
    }

    public void setMonUuid(String monUuid) {
        this.monUuid = monUuid;
    }
}
