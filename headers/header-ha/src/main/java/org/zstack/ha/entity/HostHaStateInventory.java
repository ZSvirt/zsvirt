package org.zstack.ha.entity;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: DaoDao
 * @Date: 2023/4/14
 */
@Inventory(mappingVOClass = HostHaStateVO.class)
public class HostHaStateInventory {
    private String hostUuid;
    private String primaryStorageUuid;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static HostHaStateInventory valueOf(HostHaStateVO vo) {
        HostHaStateInventory inv = new HostHaStateInventory();
        inv.setHostUuid(vo.getHostUuid());
        inv.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        inv.setState(vo.getState().toString());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<HostHaStateInventory> valueOf(Collection<HostHaStateVO> vos) {
        List<HostHaStateInventory> invs = new ArrayList<HostHaStateInventory>();
        for (HostHaStateVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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
}
