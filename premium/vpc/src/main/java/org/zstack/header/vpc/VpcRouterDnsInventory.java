package org.zstack.header.vpc;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VpcRouterDnsVO.class)
public class VpcRouterDnsInventory {
    private Long id;
    private String vpcRouterUuid;
    private String dns;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VpcRouterDnsInventory valueOf(VpcRouterDnsVO vo) {
        VpcRouterDnsInventory inv = new VpcRouterDnsInventory();
        inv.setId(vo.getId());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setCreateDate(vo.getCreateDate());
        inv.setDns(vo.getDns());
        inv.setVpcRouterUuid(vo.getVpcRouterUuid());
        return inv;
    }

    public static List<VpcRouterDnsInventory> valueOf(Collection<VpcRouterDnsVO> vos) {
        List<VpcRouterDnsInventory> invs = new ArrayList<VpcRouterDnsInventory>();
        for (VpcRouterDnsVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getVpcRouterUuid() {
        return vpcRouterUuid;
    }

    public void setVpcRouterUuid(String vpcRouterUuid) {
        this.vpcRouterUuid = vpcRouterUuid;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
