package org.zstack.appliancevm;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.search.TypeField;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = ApplianceVmVO.class, collectionValueOfMethod="valueOf1",
        parent = {@Parent(inventoryClass = VmInstanceInventory.class, type = ApplianceVmConstant.APPLIANCE_VM_TYPE)})
@PythonClassInventory
public class ApplianceVmInventory extends VmInstanceInventory {
    @TypeField
    private String applianceVmType;
    private String managementNetworkUuid;
    private String defaultRouteL3NetworkUuid;
    private String status;
    private Integer agentPort;
    private String haStatus;

    protected ApplianceVmInventory(ApplianceVmVO vo) {
        super(vo);
        this.setApplianceVmType(vo.getApplianceVmType());
        this.setManagementNetworkUuid(vo.getManagementNetworkUuid());
        this.setDefaultRouteL3NetworkUuid(vo.getDefaultRouteL3NetworkUuid());
        this.setStatus(vo.getStatus().toString());
        this.setAgentPort(vo.getAgentPort());
        this.setHaStatus(vo.getHaStatus().toString());
    }

    public ApplianceVmInventory() {
    }

    public Integer getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(Integer agentPort) {
        this.agentPort = agentPort;
    }

    public String getManagementNetworkUuid() {
        return managementNetworkUuid;
    }

    public void setManagementNetworkUuid(String managementNetworkUuid) {
        this.managementNetworkUuid = managementNetworkUuid;
    }

    public static ApplianceVmInventory valueOf(ApplianceVmVO vo) {
        return new ApplianceVmInventory(vo);
    }

    public static List<ApplianceVmInventory> valueOf1(Collection<ApplianceVmVO> vos) {
        List<ApplianceVmInventory> invs = new ArrayList<ApplianceVmInventory>(vos.size());
        for (ApplianceVmVO vo : vos) {
            invs.add(new ApplianceVmInventory(vo));
        }
        return invs;
    }

    public String getApplianceVmType() {
        return applianceVmType;
    }

    public void setApplianceVmType(String applianceVmType) {
        this.applianceVmType = applianceVmType;
    }

    public String getDefaultRouteL3NetworkUuid() {
        return defaultRouteL3NetworkUuid;
    }

    public void setDefaultRouteL3NetworkUuid(String defaultRouteL3NetworkUuid) {
        this.defaultRouteL3NetworkUuid = defaultRouteL3NetworkUuid;
    }

    public VmNicInventory getManagementNic() {
        for (VmNicInventory nic : this.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(getManagementNetworkUuid())) {
                return nic;
            }
        }

        return null;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHaStatus() {
        return haStatus;
    }

    public void setHaStatus(String haStatus) {
        this.haStatus = haStatus;
    }

    public boolean isHaEnabled() {
        return !ApplianceVmHaStatus.NoHa.toString().equals(haStatus);
    }
}
