package org.zstack.guesttools;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = GuestToolsStateVO.class, collectionValueOfMethod = "valueOf1")
public class GuestToolsStateInventory implements Serializable {
    private String vmInstanceUuid;

    private String qgaState;

    private String zwatchState;
    private String version;
    private String platform;
    private String osType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected GuestToolsStateInventory(GuestToolsStateVO vo) {
        this.setVmInstanceUuid(vo.getVmInstanceUuid());
        this.setQgaState(vo.getQgaState().toString());
        this.setZwatchState(vo.getZwatchState().toString());
        this.setVersion(vo.getVersion());
        this.setPlatform(vo.getPlatform());
        this.setOsType(vo.getOsType());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static GuestToolsStateInventory valueOf(GuestToolsStateVO vo) {
        return new GuestToolsStateInventory(vo);
    }

    public static List<GuestToolsStateInventory> valueOf1(Collection<GuestToolsStateVO> vos) {
        List<GuestToolsStateInventory> invs = new ArrayList<GuestToolsStateInventory>(vos.size());
        for (GuestToolsStateVO vo : vos) {
            invs.add(GuestToolsStateInventory.valueOf(vo));
        }
        return invs;
    }

    public GuestToolsStateInventory() {
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getQgaState() {
        return qgaState;
    }

    public void setQgaState(String qgaState) {
        this.qgaState = qgaState;
    }

    public String getZwatchState() {
        return zwatchState;
    }

    public void setZwatchState(String zwatchState) {
        this.zwatchState = zwatchState;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
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
