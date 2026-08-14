package org.zstack.header.vm;

import org.zstack.header.search.Inventory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = TemplatedVmInstanceCacheVO.class)
public class TemplatedVmInstanceCacheInventory {
    private long id;
    private String templatedVmInstanceUuid;
    private String cacheVmInstanceUuid;

    public static TemplatedVmInstanceCacheInventory valueOf(TemplatedVmInstanceCacheVO vo) {
        TemplatedVmInstanceCacheInventory inventory = new TemplatedVmInstanceCacheInventory();
        inventory.setId(vo.getId());
        inventory.setTemplatedVmInstanceUuid(vo.getTemplatedVmInstanceUuid());
        inventory.setCacheVmInstanceUuid(vo.getCacheVmInstanceUuid());
        return inventory;
    }

    public static List<TemplatedVmInstanceCacheInventory> valueOf(Collection<TemplatedVmInstanceCacheVO> vos) {
        return vos.stream().map(TemplatedVmInstanceCacheInventory::valueOf).collect(Collectors.toList());
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

    public String getCacheVmInstanceUuid() {
        return cacheVmInstanceUuid;
    }

    public void setCacheVmInstanceUuid(String cacheVmInstanceUuid) {
        this.cacheVmInstanceUuid = cacheVmInstanceUuid;
    }
}