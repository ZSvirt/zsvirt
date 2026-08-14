package org.zstack.ipsec;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2016/11/3.
 */
@Inventory(mappingVOClass = IPsecPeerCidrVO.class)
public class IPsecPeerCidrInventory {
    private String uuid;
    private String cidr;
    private String connectionUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static IPsecPeerCidrInventory valueOf(IPsecPeerCidrVO vo) {
        IPsecPeerCidrInventory inv = new IPsecPeerCidrInventory();
        inv.setUuid(vo.getUuid());
        inv.setConnectionUuid(vo.getConnectionUuid());
        inv.setCidr(vo.getCidr());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<IPsecPeerCidrInventory> valueOf(Collection<IPsecPeerCidrVO> vos) {
        List<IPsecPeerCidrInventory> invs = new ArrayList<>();
        for (IPsecPeerCidrVO vo : vos) {
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

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public String getConnectionUuid() {
        return connectionUuid;
    }

    public void setConnectionUuid(String connectionUuid) {
        this.connectionUuid = connectionUuid;
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
