package org.zstack.storage.device.nvme;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = NvmeLunHostRefVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "nvmeLun", inventoryClass = NvmeLunInventory.class,
                foreignKey = "nvmeLunUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "hostUuid", expandedInventoryKey = "uuid"),
})
public class NvmeLunHostRefInventory implements Serializable {
    @APINoSee
    private long id;

    private String nvmeLunUuid;

    private String hostUuid;

    private String path;

    private String hctl;

    private String locate;

    private String transport;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public NvmeLunHostRefInventory() {
    }

    public static NvmeLunHostRefInventory valueOf(NvmeLunHostRefVO vo) {
        final NvmeLunHostRefInventory inventory = new NvmeLunHostRefInventory();
        inventory.setId(vo.getId());
        inventory.setHostUuid(vo.getHostUuid());
        inventory.setNvmeLunUuid(vo.getNvmeLunUuid());
        inventory.setHctl(vo.getHctl());
        inventory.setPath(vo.getPath());
        inventory.setLocate(vo.getLocate() == null ? null : vo.getLocate().toString());
        inventory.setTransport(vo.getTransport());
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        return inventory;
    }

    public static List<NvmeLunHostRefInventory> valueOf1(Collection<NvmeLunHostRefVO> vos) {
        return CollectionUtils.transform(vos, NvmeLunHostRefInventory::valueOf);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public void setNvmeLunUuid(String nvmeLunUuid) {
        this.nvmeLunUuid = nvmeLunUuid;
    }

    public String getNvmeLunUuid() {
        return nvmeLunUuid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHctl() {
        return hctl;
    }

    public void setHctl(String hctl) {
        this.hctl = hctl;
    }

    public String getLocate() {
        return locate;
    }

    public void setLocate(String locate) {
        this.locate = locate;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
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
