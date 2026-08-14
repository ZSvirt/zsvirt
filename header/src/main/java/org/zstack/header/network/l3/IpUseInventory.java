package org.zstack.header.network.l3;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = IpUseVO.class)
public class IpUseInventory {
    private String uuid;
    private String usedIpUuid;
    private String serviceId;
    private String use;
    private String details;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static IpUseInventory valueOf(IpUseVO vo) {
        IpUseInventory inv = new IpUseInventory();
        inv.setUse(vo.getUse());
        inv.setCreateDate(vo.getCreateDate());
        inv.setDetails(vo.getDetails());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setServiceId(vo.getServiceId());
        inv.setUsedIpUuid(vo.getUsedIpUuid());
        inv.setUuid(vo.getUuid());
        return inv;
    }

    public static List<IpUseInventory> valueOf(Collection<IpUseVO> vos) {
        List<IpUseInventory> invs = new ArrayList<IpUseInventory>();
        for (IpUseVO vo : vos) {
            invs.add(IpUseInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getUse() {
        return use;
    }

    public void setUse(String use) {
        this.use = use;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
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
