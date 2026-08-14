package org.zstack.ipsec;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by shixin on 2016/11/3.
 */
@Inventory(mappingVOClass = IPsecL3NetworkRefVO.class)
public class IPsecL3NetworkRefInventory {
    private String uuid;
    private String connectionUuid;
    private String l3NetworkUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static IPsecL3NetworkRefInventory valueOf(IPsecL3NetworkRefVO vo) {
        IPsecL3NetworkRefInventory inv = new IPsecL3NetworkRefInventory();
        inv.setUuid(vo.getUuid());
        inv.setConnectionUuid(vo.getConnectionUuid());
        inv.setL3NetworkUuid(vo.getL3NetworkUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<IPsecL3NetworkRefInventory> valueOf(Collection<IPsecL3NetworkRefVO> vos) {
        List<IPsecL3NetworkRefInventory> invs = new ArrayList<>();
        for (IPsecL3NetworkRefVO vo : vos) {
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

    public String getConnectionUuid() {
        return connectionUuid;
    }

    public void setConnectionUuid(String connectionUuid) {
        this.connectionUuid = connectionUuid;
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
}
