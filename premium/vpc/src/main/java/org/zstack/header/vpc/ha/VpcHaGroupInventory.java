package org.zstack.header.vpc.ha;

import org.zstack.core.db.Q;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;

import javax.persistence.JoinColumn;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = VpcHaGroupVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vrRef", inventoryClass = VpcHaGroupApplianceVmRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "vpcHaRouterUuid", hidden = true)
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "applianceVm", expandedField = "vrRef.applianceVm"),
})

@PythonClassInventory
public class VpcHaGroupInventory {
    private String uuid;
    private String name;
    private String description;

    @Queryable(mappingClass = VpcHaGroupMonitorIpInventory.class,
            joinColumn = @JoinColumn(name = "vpcHaRouterUuid"))
    private List<VpcHaGroupMonitorIpInventory> monitors;

    @Queryable(mappingClass = VpcHaGroupApplianceVmRefInventory.class,
            joinColumn = @JoinColumn(name = "vpcHaRouterUuid"))
    private List<VpcHaGroupApplianceVmRefInventory> vrRefs;

    @Queryable(mappingClass = VpcHaGroupNetworkServiceRefInventory.class,
            joinColumn = @JoinColumn(name = "vpcHaRouterUuid"))
    private List<VpcHaGroupNetworkServiceRefInventory> services;

    @Queryable(mappingClass = VpcHaGroupVipRefInventory.class,
            joinColumn = @JoinColumn(name = "vpcHaRouterUuid"))
    private List<VpcHaGroupVipRefInventory> usedIps;

    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VpcHaGroupInventory valueOf(VpcHaGroupVO vo) {
        VpcHaGroupInventory inv = new VpcHaGroupInventory();
        inv.setUuid(vo.getUuid());
        inv.setDescription(vo.getDescription());
        inv.setName(vo.getName());
        inv.setMonitors(VpcHaGroupMonitorIpInventory.valueOf(vo.getMonitors()));
        inv.setServices(VpcHaGroupNetworkServiceRefInventory.valueOf(vo.getServices()));
        inv.setVrRefs(VpcHaGroupApplianceVmRefInventory.valueOf(vo.getVrs()));
        inv.setUsedIps(new ArrayList<>());
        /* make api result same to old result */
        List<String> vipUuids = vo.getServices().stream().filter(s -> s.getNetworkServiceName().equals(VipVO.class.getSimpleName())).map(VpcHaGroupNetworkServiceRefVO::getNetworkServiceUuid).collect(Collectors.toList());
        List<String> defaultL3Uuids = vo.getServices().stream().filter(s -> s.getNetworkServiceName().equals(VirtualRouterConstant.VR_DEFAULT_ROUTE_NETWORK)).map(VpcHaGroupNetworkServiceRefVO::getNetworkServiceUuid).collect(Collectors.toList());
        if (!vipUuids.isEmpty() && !defaultL3Uuids.isEmpty()) {
            List<VipVO> vipVos = Q.New(VipVO.class).in(VipVO_.uuid, vipUuids).eq(VipVO_.system, Boolean.TRUE)
                    .eq(VipVO_.l3NetworkUuid, defaultL3Uuids.get(0)).list();
            if (!vipVos.isEmpty()) {
                vipVos.forEach(vipVo -> {
                    VpcHaGroupVipRefInventory vip = new VpcHaGroupVipRefInventory();
                    vip.setIp(vipVo.getIp());
                    vip.setL3NetworkUuid(vipVo.getL3NetworkUuid());
                    vip.setNetmask(vipVo.getNetmask());
                    vip.setVipUuid(vipVo.getUuid());
                    vip.setVpcHaRouterUuid(vo.getUuid());
                    inv.getUsedIps().add(vip);
                });
            }
        }
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<VpcHaGroupInventory> valueOf(Collection<VpcHaGroupVO> vos) {
        List<VpcHaGroupInventory> lst = new ArrayList<VpcHaGroupInventory>(vos.size());
        for (VpcHaGroupVO vo : vos) {
            lst.add(VpcHaGroupInventory.valueOf(vo));
        }
        return lst;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public List<VpcHaGroupMonitorIpInventory> getMonitors() {
        return monitors;
    }

    public void setMonitors(List<VpcHaGroupMonitorIpInventory> monitors) {
        this.monitors = monitors;
    }

    public List<VpcHaGroupApplianceVmRefInventory> getVrRefs() {
        return vrRefs;
    }

    public void setVrRefs(List<VpcHaGroupApplianceVmRefInventory> vrRefs) {
        this.vrRefs = vrRefs;
    }

    public List<VpcHaGroupVipRefInventory> getUsedIps() {
        return usedIps;
    }

    public void setUsedIps(List<VpcHaGroupVipRefInventory> usedIps) {
        this.usedIps = usedIps;
    }

    public List<VpcHaGroupNetworkServiceRefInventory> getServices() {
        return services;
    }

    public void setServices(List<VpcHaGroupNetworkServiceRefInventory> services) {
        this.services = services;
    }
}
