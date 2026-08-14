package org.zstack.storage.device.fibreChannel;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.storageDevice.ScsiLunHostRefInventory;
import org.zstack.header.storageDevice.ScsiLunInventory;
import org.zstack.header.storageDevice.ScsiLunVmInstanceRefInventory;
import org.zstack.storage.device.iscsi.IscsiLunInventory;

import java.io.Serializable;
import java.util.*;

@PythonClassInventory
@Inventory(mappingVOClass = FiberChannelLunVO.class, collectionValueOfMethod = "valueOf2")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "fiberChannelStorage", inventoryClass = FiberChannelStorageInventory.class,
                foreignKey = "fiberChannelStorageUuid", expandedInventoryKey = "uuid"),
})
public class FiberChannelLunInventory extends ScsiLunInventory implements Serializable {
    private String fiberChannelStorageUuid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FiberChannelLunInventory that = (FiberChannelLunInventory) o;
        return getSerial().equals(that.getSerial());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getSerial() == null ? 0 : getSerial().hashCode());
        return result;
    }

    public FiberChannelLunInventory() {
    }

    public FiberChannelLunInventory(FiberChannelLunVO vo) {
        this.setName(vo.getName());
        this.setUuid(vo.getUuid());
        this.setFiberChannelStorageUuid(vo.getFiberChannelStorageUuid());
        this.setWwid(vo.getWwid());
        this.setVendor(vo.getVendor());
        this.setModel(vo.getModel());
        this.setWwn(vo.getWwn());
        this.setSerial(vo.getSerial());
        this.setType(vo.getType());
        this.setPath(vo.getPath());
        this.setSize(vo.getSize());
        this.setSource(vo.getSource());
        this.setMultipathDeviceUuid(vo.getMultipathDeviceUuid());
        this.setScsiLunHostRefs(ScsiLunHostRefInventory.valueOf1(vo.getScsiLunHostRefs()));
        this.setScsiLunVmInstanceRefs(ScsiLunVmInstanceRefInventory.valueOf1(vo.getScsiLunVmInstanceRefs()));
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public FiberChannelLunInventory(FiberChannelLunStruct s) {
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

    public static List<FiberChannelLunInventory> valueOf3(Collection<FiberChannelLunStruct> structs) {
        List<FiberChannelLunInventory> invs = new ArrayList<FiberChannelLunInventory>();
        for (FiberChannelLunStruct s : structs) {
            invs.add(new FiberChannelLunInventory(s));
        }

        return invs;
    }

    public static FiberChannelLunInventory valueOf(FiberChannelLunVO vo) {
        return new FiberChannelLunInventory(vo);
    }

    public static List<FiberChannelLunInventory> valueOf2(Collection<FiberChannelLunVO> vos) {
        List<FiberChannelLunInventory> invs = new ArrayList<FiberChannelLunInventory>();
        for (FiberChannelLunVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getFiberChannelStorageUuid() {
        return fiberChannelStorageUuid;
    }

    public void setFiberChannelStorageUuid(String fiberChannelStorageUuid) {
        this.fiberChannelStorageUuid = fiberChannelStorageUuid;
    }
}
