package org.zstack.header.sriov;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.pciDevice.PciDeviceInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by GuoYi on 11/28/19.
 */
@PythonClassInventory
@Inventory(mappingVOClass = VmVfNicVO.class, collectionValueOfMethod="valueOf1")
@ExpandedQueries({@ExpandedQuery(
        expandedField = "pciDevice", inventoryClass = PciDeviceInventory.class,
        foreignKey = "pciDeviceUuid", expandedInventoryKey = "uuid"),
})
public class VmVfNicInventory extends VmNicInventory {
    private String pciDeviceUuid;
    private String haState;

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }

    public String getHaState() {
        return haState;
    }

    public void setHaState(String haState) {
        this.haState = haState;
    }

    public VmVfNicInventory() {
    }

    public VmVfNicInventory(VmVfNicVO vo) {
        super(vo);
        this.pciDeviceUuid = vo.getPciDeviceUuid();
        this.haState = vo.getHaState().toString();
    }

    public static VmVfNicInventory valueOf(VmVfNicVO vo) {
        return new VmVfNicInventory(vo);
    }

    public static List<VmVfNicInventory> valueOf1(List<VmVfNicVO> vos) {
        List<VmVfNicInventory> invs = new ArrayList<>();
        for (VmVfNicVO vo : vos) {
            invs.add(VmVfNicInventory.valueOf(vo));
        }
        return invs;
    }
}
