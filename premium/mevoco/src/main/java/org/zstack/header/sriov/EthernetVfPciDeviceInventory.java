package org.zstack.header.sriov;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.pciDevice.PciDeviceInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shixin.ruan 2023/10/20
 */
@PythonClassInventory
@Inventory(mappingVOClass = EthernetVfPciDeviceVO.class, collectionValueOfMethod="valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "l3NetworkUuid", expandedInventoryKey = "uuid"),
})
public class EthernetVfPciDeviceInventory extends PciDeviceInventory {
    private String hostDevUuid;

    private String interfaceName;

    private String vmUuid;

    private String l3NetworkUuid;

    private EthernetVfStatus vfStatus;

    public EthernetVfPciDeviceInventory() {}

    public EthernetVfPciDeviceInventory(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public EthernetVfPciDeviceInventory(EthernetVfPciDeviceVO vo) {
        super(vo);
        this.hostDevUuid = vo.getHostDevUuid();
        this.interfaceName = vo.getInterfaceName();
        this.vmUuid = vo.getVmUuid();
        this.l3NetworkUuid = vo.getL3NetworkUuid();
        this.vfStatus = vo.getVfStatus();
    }

    public static EthernetVfPciDeviceInventory valueOf(EthernetVfPciDeviceVO vo) {
        return new EthernetVfPciDeviceInventory(vo);
    }

    public static List<EthernetVfPciDeviceInventory> valueOf1(List<EthernetVfPciDeviceVO> vos) {
        List<EthernetVfPciDeviceInventory> invs = new ArrayList<>();
        for (EthernetVfPciDeviceVO vo : vos) {
            invs.add(EthernetVfPciDeviceInventory.valueOf(vo));
        }
        return invs;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public EthernetVfStatus getVfStatus() {
        return vfStatus;
    }

    public void setVfStatus(EthernetVfStatus vfStatus) {
        this.vfStatus = vfStatus;
    }
}
