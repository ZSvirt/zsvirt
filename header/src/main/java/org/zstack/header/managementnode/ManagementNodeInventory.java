package org.zstack.header.managementnode;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Inventory(mappingVOClass = ManagementNodeVO.class)
@PythonClassInventory
public class ManagementNodeInventory {
    private String uuid;
    private String hostName;
    private Timestamp joinDate;
    private Timestamp heartBeat;

    public static ManagementNodeInventory valueOf(ManagementNodeVO vo) {
        ManagementNodeInventory inv = new ManagementNodeInventory();
        inv.setHeartBeat(vo.getHeartBeat());
        inv.setHostName(vo.getHostName());
        inv.setJoinDate(vo.getJoinDate());
        inv.setUuid(vo.getUuid());
        return inv;
    }

    public static List<ManagementNodeInventory> valueOf(Collection<ManagementNodeVO> vos) {
        List<ManagementNodeInventory> lst = new ArrayList<ManagementNodeInventory>();
        for (ManagementNodeVO vo : vos) {
            lst.add(valueOf(vo));
        }
        return lst;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setJoinDate(Timestamp joinDate) {
        this.joinDate = joinDate;
    }

    public void setHeartBeat(Timestamp heartBeat) {
        this.heartBeat = heartBeat;
    }

    public Timestamp getJoinDate() {
        return joinDate;
    }

    public Timestamp getHeartBeat() {
        return heartBeat;
    }
}
