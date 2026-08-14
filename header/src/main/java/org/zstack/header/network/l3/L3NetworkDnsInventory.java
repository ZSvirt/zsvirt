package org.zstack.header.network.l3;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = L3NetworkDnsVO.class)
public class L3NetworkDnsInventory {
    private String l3NetworkUuid;
    private String dns;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static L3NetworkDnsInventory valueOf(L3NetworkDnsVO vo) {
        L3NetworkDnsInventory inv = new L3NetworkDnsInventory();
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setCreateDate(vo.getCreateDate());
        inv.setDns(vo.getDns());
        inv.setL3NetworkUuid(vo.getL3NetworkUuid());
        return inv;
    }

    public static List<L3NetworkDnsInventory> valueOf(Collection<L3NetworkDnsVO> vos) {
        List<L3NetworkDnsInventory> invs = new ArrayList<L3NetworkDnsInventory>();
        for (L3NetworkDnsVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
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
}
