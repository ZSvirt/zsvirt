package org.zstack.header.protocol;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = NetworkRouterAreaRefVO.class)
@PythonClassInventory
@ExpandedQueries({

})
public class NetworkRouterAreaRefInventory implements Serializable {
    private String uuid;
    private String vRouterUuid;
    private String applianceVmType;
    private String routerAreaUuid;
    private String l3NetworkUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public NetworkRouterAreaRefInventory() {
    }

    public static NetworkRouterAreaRefInventory valueOf(NetworkRouterAreaRefVO vo) {
        NetworkRouterAreaRefInventory inv = new NetworkRouterAreaRefInventory();
        inv.uuid = vo.getUuid();
        inv.vRouterUuid = vo.getvRouterUuid();
        inv.l3NetworkUuid = vo.getL3NetworkUuid();
        inv.setApplianceVmType(vo.getApplianceVmType());
        inv.routerAreaUuid = vo.getRouterAreaUuid();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        return inv;
    }

    public static List<NetworkRouterAreaRefInventory> valueOf(Collection<NetworkRouterAreaRefVO> vos) {
        List<NetworkRouterAreaRefInventory> invs = new ArrayList<NetworkRouterAreaRefInventory>();
        for (NetworkRouterAreaRefVO vo : vos) {
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

    public String getvRouterUuid() {
        return vRouterUuid;
    }

    public void setvRouterUuid(String vRouterUuid) {
        this.vRouterUuid = vRouterUuid;
    }

    public String getRouterAreaUuid() {
        return routerAreaUuid;
    }

    public void setRouterAreaUuid(String routerAreaUuid) {
        this.routerAreaUuid = routerAreaUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
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

    public String getApplianceVmType() {
        return applianceVmType;
    }

    public void setApplianceVmType(String applianceVmType) {
        this.applianceVmType = applianceVmType;
    }
}
