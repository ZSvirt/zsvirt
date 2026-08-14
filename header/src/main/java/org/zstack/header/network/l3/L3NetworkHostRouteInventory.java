package org.zstack.header.network.l3;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = L3NetworkHostRouteVO.class)
public class L3NetworkHostRouteInventory {
    private Long   id;
    private String l3NetworkUuid;
    private String prefix;
    private String nexthop;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static L3NetworkHostRouteInventory valueOf(L3NetworkHostRouteVO vo) {
        L3NetworkHostRouteInventory inv = new L3NetworkHostRouteInventory();
        inv.setId(vo.getId());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setCreateDate(vo.getCreateDate());
        inv.setPrefix(vo.getPrefix());
        inv.setNexthop(vo.getNexthop());
        inv.setL3NetworkUuid(vo.getL3NetworkUuid());
        return inv;
    }

    public static List<L3NetworkHostRouteInventory> valueOf(Collection<L3NetworkHostRouteVO> vos) {
        List<L3NetworkHostRouteInventory> invs = new ArrayList<L3NetworkHostRouteInventory>();
        for (L3NetworkHostRouteVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getNexthop() {
        return nexthop;
    }

    public void setNexthop(String nexthop) {
        this.nexthop = nexthop;
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
