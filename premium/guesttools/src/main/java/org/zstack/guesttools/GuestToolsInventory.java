package org.zstack.guesttools;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by GuoYi on 2019-09-19.
 */
@Inventory(mappingVOClass = GuestToolsVO.class)
@PythonClassInventory
public class GuestToolsInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String managementNodeUuid;
    private String architecture;
    private String hypervisorType;
    private String version;
    private String agentType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public GuestToolsInventory() {
    }

    public static GuestToolsInventory valueOf(GuestToolsVO vo) {
        GuestToolsInventory inv = new GuestToolsInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setManagementNodeUuid(vo.getManagementNodeUuid());
        inv.setArchitecture(vo.getArchitecture());
        inv.setHypervisorType(vo.getHypervisorType());
        inv.setVersion(vo.getVersion());
        inv.setAgentType(vo.getAgentType());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<GuestToolsInventory> valueOf(Collection<GuestToolsVO> vos) {
        List<GuestToolsInventory> invs = new ArrayList<>();
        for (GuestToolsVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public static GuestToolsInventory __example__() {
        GuestToolsInventory inv = new GuestToolsInventory();
        inv.setUuid(DocUtils.createFixedUuid(GuestToolsVO.class));
        inv.setManagementNodeUuid(DocUtils.createFixedUuid(ManagementNodeVO.class));
        inv.setArchitecture("x86_64");
        inv.setHypervisorType("KVM");
        inv.setVersion("1.0.0");
        inv.setCreateDate(new Timestamp(DocUtils.date));
        inv.setLastOpDate(new Timestamp(DocUtils.date));
        return inv;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
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
