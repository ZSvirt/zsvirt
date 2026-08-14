package org.zstack.network.service.vip;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author: zhanyong.miao
 * @date: 2019-05-20
 **/
@Inventory(mappingVOClass = VipNetworkServicesRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vip", inventoryClass = VipInventory.class,
                foreignKey = "vipUuid", expandedInventoryKey = "uuid")
})
public class VipNetworkServicesRefInventory implements Serializable {
    private String uuid;
    private String serviceType;
    private String vipUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public VipNetworkServicesRefInventory() {}
    public static VipNetworkServicesRefInventory valueOf (VipNetworkServicesRefVO vo) {
        VipNetworkServicesRefInventory inv = new VipNetworkServicesRefInventory();
        inv.uuid = vo.getUuid();
        inv.serviceType = vo.getServiceType();
        inv.vipUuid = vo.getVipUuid();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        return inv;
    }

    public static List<VipNetworkServicesRefInventory> valueOf (Collection<VipNetworkServicesRefVO> vos) {
        List<VipNetworkServicesRefInventory> invs = new ArrayList<>();
        for (VipNetworkServicesRefVO vo: vos) {
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

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
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
