package org.zstack.header.sshkeypair;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.zone.ZoneInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = SshKeyPairVO.class)
public class SshKeyPairInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String publicKey;

    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static SshKeyPairInventory valueOf(SshKeyPairVO vo) {
        SshKeyPairInventory inv = new SshKeyPairInventory();
        inv.setName(vo.getName());
        inv.setUuid(vo.getUuid());
        inv.setDescription(vo.getDescription());
        inv.setPublicKey(vo.getPublicKey());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<SshKeyPairInventory> valueOf(Collection<SshKeyPairVO> vos) {
        List<SshKeyPairInventory> invs = new ArrayList<>();
        for (SshKeyPairVO vo: vos) {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
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
