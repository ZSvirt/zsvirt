package org.zstack.header.sshkeypair;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmInstanceInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = SshKeyPairRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "vmInstanceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "sshKeyPair", inventoryClass = SshKeyPairInventory.class,
                foreignKey = "sshKeyPairUuid", expandedInventoryKey = "uuid")
})
public class SshKeyPairRefInventory {
    private long id;
    private String resourceUuid;
    private String sshKeyPairUuid;
    private Timestamp createDate;

    public static SshKeyPairRefInventory valueOf(SshKeyPairRefVO vo) {
        SshKeyPairRefInventory inv = new SshKeyPairRefInventory();
        inv.setId(vo.getId());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setSshKeyPairUuid(vo.getSshKeyPairUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<SshKeyPairRefInventory> valueOf(Collection<SshKeyPairRefVO> vos) {
        List<SshKeyPairRefInventory> invs = new ArrayList<>();
        for (SshKeyPairRefVO vo: vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getSshKeyPairUuid() {
        return sshKeyPairUuid;
    }

    public void setSshKeyPairUuid(String sshKeyPairUuid) {
        this.sshKeyPairUuid = sshKeyPairUuid;
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

    private Timestamp lastOpDate;

}
