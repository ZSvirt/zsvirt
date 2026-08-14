package org.zstack.header.tpm.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import static org.zstack.utils.CollectionUtils.transform;

@PythonClassInventory
@Inventory(mappingVOClass = TpmVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "vmInstanceUuid", expandedInventoryKey = "uuid"),
})
public class TpmInventory implements Serializable {
    private String uuid;
    private String name;
    private String vmInstanceUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public TpmInventory() {
    }

    public static TpmInventory valueOf(TpmVO vo) {
        TpmInventory inv = new TpmInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getResourceName());
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<TpmInventory> valueOf(Collection<TpmVO> vos) {
        return transform(vos, TpmInventory::valueOf);
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

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
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

    public static TpmInventory __example__() {
        TpmInventory tpm = new TpmInventory();
        tpm.setUuid(DocUtils.createFixedUuid(TpmVO.class));
        tpm.setVmInstanceUuid(DocUtils.createFixedUuid(VmInstanceVO.class));
        tpm.setName("TPM-for-VM-" + tpm.getVmInstanceUuid());
        tpm.setCreateDate(DocUtils.timestamp());
        tpm.setLastOpDate(DocUtils.timestamp());
        return tpm;
    }
}
