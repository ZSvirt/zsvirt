package org.zstack.header.vm;

import org.zstack.header.search.Inventory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = TemplatedVmInstanceRefVO.class)
public class TemplatedVmInstanceRefInventory {
    private long id;
    private String templatedVmInstanceUuid;
    private String vmInstanceUuid;


    public static TemplatedVmInstanceRefInventory valueOf(TemplatedVmInstanceRefVO vo) {
        TemplatedVmInstanceRefInventory inventory = new TemplatedVmInstanceRefInventory();
        inventory.setId(vo.getId());
        inventory.setTemplatedVmInstanceUuid(vo.getTemplatedVmInstanceUuid());
        inventory.setVmInstanceUuid(vo.getVmInstanceUuid());
        return inventory;
    }

    public static List<TemplatedVmInstanceRefInventory> valueOf(Collection<TemplatedVmInstanceRefVO> vos) {
        return vos.stream().map(TemplatedVmInstanceRefInventory::valueOf).collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTemplatedVmInstanceUuid() {
        return templatedVmInstanceUuid;
    }

    public void setTemplatedVmInstanceUuid(String templatedVmInstanceUuid) {
        this.templatedVmInstanceUuid = templatedVmInstanceUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
