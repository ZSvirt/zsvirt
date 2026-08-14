package org.zstack.header.identity.role;

import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = RolePolicyVO.class)
public class RolePolicyInventory {
    private String roleUuid;
    private String actions;
    private String effect;
    private String resourceUuid;
    private Timestamp createDate;

    public static RolePolicyInventory valueOf(RolePolicyVO vo) {
        RolePolicyInventory inv = new RolePolicyInventory();
        inv.setRoleUuid(vo.getRoleUuid());
        inv.setActions(vo.getActions());
        inv.setEffect(vo.getEffect().toString());
        inv.setResourceUuid(vo.getResourceType());
        inv.setCreateDate(vo.getCreateDate());
        return inv;
    }

    public static List<RolePolicyInventory> valueOf(Collection<RolePolicyVO> vos) {
        return CollectionUtils.transform(vos, RolePolicyInventory::valueOf);
    }

    public String toStatement() {
        return RolePolicyStatement.toStringStatement(this);
    }

    public static String toStatement(RolePolicyVO vo) {
        return RolePolicyStatement.toStringStatement(vo);
    }

    public static List<String> toStatements(Collection<RolePolicyVO> vos) {
        return CollectionUtils.transform(vos, RolePolicyStatement::toStringStatement);
    }

    public String getRoleUuid() {
        return roleUuid;
    }

    public void setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
