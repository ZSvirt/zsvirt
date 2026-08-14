package org.zstack.storage.device.nvme;

import org.jetbrains.annotations.NotNull;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.storageDevice.LunInventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@PythonClassInventory
@Inventory(mappingVOClass = NvmeLunVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "nvmeTarget", inventoryClass = NvmeTargetInventory.class,
                foreignKey = "nvmeTargetUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "nvmeLunHostRef", inventoryClass = NvmeLunHostRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "nvmeLunUuid"),
})
public class NvmeLunInventory extends LunInventory implements Serializable, Comparable<NvmeLunInventory> {
    private String nvmeTargetUuid;

    private List<NvmeLunHostRefInventory> nvmeLunHostRefs = new ArrayList<>();

    public NvmeLunInventory() {
    }

    public NvmeLunInventory(NvmeLunVO vo) {
        super(vo);
        this.setNvmeTargetUuid(vo.getNvmeTargetUuid());
        this.setNvmeLunHostRefs(NvmeLunHostRefInventory.valueOf1(vo.getNvmeLunHostRefs()));
    }

    public NvmeLunInventory(NvmeLunStruct s) {
        Collections.sort(s.wwids);
        this.setWwid(s.wwids.get(0));
        this.setVendor(s.vendor);
        this.setModel(s.model);
        this.setWwn(s.wwn);
        this.setSerial(s.serial);
        this.setType(s.type);
        this.setPath(s.path);
        this.setSize(s.size);
        this.setMultipathDeviceUuid(s.multipathDeviceUuid);
    }

    public static NvmeLunInventory valueOf(NvmeLunVO vo) {
        return new NvmeLunInventory(vo);
    }

    public static List<NvmeLunInventory> valueOf1(Collection<NvmeLunVO> vos) {
        return vos.stream().map(NvmeLunInventory::new).collect(Collectors.toList());
    }

    public String getNvmeTargetUuid() {
        return nvmeTargetUuid;
    }

    public void setNvmeTargetUuid(String nvmeTargetUuid) {
        this.nvmeTargetUuid = nvmeTargetUuid;
    }

    public void setNvmeLunHostRefs(List<NvmeLunHostRefInventory> nvmeLunHostRefs) {
        this.nvmeLunHostRefs = nvmeLunHostRefs;
    }

    public List<NvmeLunHostRefInventory> getNvmeLunHostRefs() {
        return nvmeLunHostRefs;
    }

    @Override
    public int compareTo(@NotNull NvmeLunInventory o) {
        return this.getSerial().compareTo(o.getSerial());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NvmeLunInventory)) return false;
        if (!super.equals(o)) return false;

        NvmeLunInventory that = (NvmeLunInventory) o;
        return getSerial().equals(that.getSerial());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getSerial().hashCode();
        return result;
    }
}
