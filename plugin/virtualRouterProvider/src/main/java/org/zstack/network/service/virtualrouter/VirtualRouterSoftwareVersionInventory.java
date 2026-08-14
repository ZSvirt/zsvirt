package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VirtualRouterSoftwareVersionVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "applianceVm", inventoryClass = ApplianceVmInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "uuid"),
})
public class VirtualRouterSoftwareVersionInventory {
    private String uuid;
    private String softwareName;
    private String currentVersion;
    private String latestVersion;

    public static VirtualRouterSoftwareVersionInventory valueOf(VirtualRouterSoftwareVersionVO vo){
        VirtualRouterSoftwareVersionInventory inv = new VirtualRouterSoftwareVersionInventory();
        inv.setUuid(vo.getUuid());
        inv.setSoftwareName(vo.getSoftwareName());
        inv.setCurrentVersion(vo.getCurrentVersion());
        inv.setLatestVersion(vo.getLatestVersion());
        return inv;
    }

    public static List<VirtualRouterSoftwareVersionInventory> valueOf(Collection<VirtualRouterSoftwareVersionVO> vos){
        List<VirtualRouterSoftwareVersionInventory> invs = new ArrayList<>(vos.size());
        for (VirtualRouterSoftwareVersionVO vo : vos) {
            invs.add(VirtualRouterSoftwareVersionInventory.valueOf(vo));
        }
        return invs;
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSoftwareName() {
        return softwareName;
    }

    public void setSoftwareName(String softwareName) {
        this.softwareName = softwareName;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }
}
