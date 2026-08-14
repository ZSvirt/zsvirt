package org.zstack.header.identity.role;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Inventory(mappingVOClass = RoleVO.class)
public class RoleInventory {
    private String uuid;
    private String name;
    private String description;
    private String type;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<String> policies;

    public RoleInventory() {
    }

    public static RoleInventory valueOf(RoleVO vo) {
        RoleInventory inv = new RoleInventory();
        inv.uuid = vo.getUuid();
        inv.name = vo.getName();
        inv.type = vo.getType().toString();
        inv.description = vo.getDescription();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        inv.policies = RolePolicyInventory.toStatements(vo.getPolicies());
        return inv;
    }

    public static List<RoleInventory> valueOf(Collection<RoleVO> vos) {
        return vos.stream().map(RoleInventory::valueOf).collect(Collectors.toList());
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getPolicies() {
        return policies;
    }

    public void setPolicies(List<String> policies) {
        this.policies = policies;
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

    public static RoleInventory __example__() {
        RoleInventory role = new RoleInventory();
        role.setName("role-1");
        role.setPolicies(asList(".header.volume.APICreateVolumeSnapshotMsg", ".header.volume.APIQueryVolumeMsg"));
        role.setDescription("role for test");
        role.setType(RoleType.Customized.toString());
        role.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        role.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        return role;
    }
}
