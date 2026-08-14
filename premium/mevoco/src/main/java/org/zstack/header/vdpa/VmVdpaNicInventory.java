package org.zstack.header.vdpa;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.pciDevice.PciDeviceInventory;

import java.util.ArrayList;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = VmVdpaNicVO.class, collectionValueOfMethod = "valueOf")
@ExpandedQueries({@ExpandedQuery(
        expandedField = "pciDevice", inventoryClass = PciDeviceInventory.class,
        foreignKey = "pciDeviceUuid", expandedInventoryKey = "uuid"),
})
public class VmVdpaNicInventory extends VmNicInventory {
    private String pciDeviceUuid;

    private String lastPciDeviceUuid;

    private String srcPath;

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }

    public String getLastPciDeviceUuid() {
        return lastPciDeviceUuid;
    }

    public void setLastPciDeviceUuid(String lastPciDeviceUuid) {
        this.lastPciDeviceUuid = lastPciDeviceUuid;
    }

    public String getSrcPath() {
        return srcPath;
    }

    public void setSrcPath(String srcPath) {
        this.srcPath = srcPath;
    }

    public VmVdpaNicInventory() {

    }

    public VmVdpaNicInventory(VmVdpaNicVO vo) {
        super(vo);
        this.pciDeviceUuid = vo.getPciDeviceUuid();
        this.lastPciDeviceUuid = vo.getLastPciDeviceUuid();
        this.srcPath = vo.getSrcPath();
    }

    public static VmVdpaNicInventory valueOf(VmVdpaNicVO vo) {
        return new VmVdpaNicInventory(vo);
    }

    public static List<VmVdpaNicInventory> valueOf(List<VmVdpaNicVO> vos) {
        List<VmVdpaNicInventory> invs = new ArrayList<>();
        for (VmVdpaNicVO vo : vos) {
            invs.add(VmVdpaNicInventory.valueOf(vo));
        }
        return invs;
    }
}
