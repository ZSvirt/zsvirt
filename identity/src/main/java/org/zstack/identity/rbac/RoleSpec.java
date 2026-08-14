package org.zstack.identity.rbac;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.identity.role.RolePolicyStatement;
import org.zstack.header.identity.role.RolePolicyVO;
import org.zstack.header.identity.role.RolePolicyVO_;
import org.zstack.header.identity.role.RoleType;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.api.APICreateRoleMsg;
import org.zstack.header.identity.role.api.APIUpdateRoleMsg;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionUtils.*;

public class RoleSpec {
    private String uuid;
    private boolean exists;
    private String name;
    private String description;
    private RoleType type;
    /**
     * Role Owner
     */
    private String accountUuid;
    private boolean clearPoliciesBeforeUpdate;
    private final List<RolePolicyStatement> policiesToCreate = new ArrayList<>();
    private final List<RolePolicyStatement> policiesToDelete = new ArrayList<>();

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
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

    public RoleType getType() {
        return type;
    }

    public void setType(RoleType type) {
        this.type = type;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public boolean isClearPoliciesBeforeUpdate() {
        return clearPoliciesBeforeUpdate;
    }

    public void setClearPoliciesBeforeUpdate(boolean clearPoliciesBeforeUpdate) {
        this.clearPoliciesBeforeUpdate = clearPoliciesBeforeUpdate;
    }

    public List<RolePolicyStatement> getPoliciesToCreate() {
        return policiesToCreate;
    }

    public List<RolePolicyStatement> getPoliciesToDelete() {
        return policiesToDelete;
    }

    public static RoleSpec valueOf(APICreateRoleMsg message) {
        RoleSpec spec = new RoleSpec();
        spec.setExists(false);
        spec.setUuid(message.getResourceUuid() == null ? Platform.getUuid() : message.getResourceUuid());
        spec.setName(message.getName());
        spec.setDescription(message.getDescription());
        spec.setType(RoleType.Customized);
        spec.setAccountUuid(message.getSession() == null ? null : message.getSession().getAccountUuid());
        if (message.getBaseOnRole() != null) {
            List<RolePolicyVO> basePolicies = Q.New(RolePolicyVO.class)
                    .eq(RolePolicyVO_.roleUuid, message.getBaseOnRole())
                    .list();
            spec.getPoliciesToCreate().addAll(transform(basePolicies, RolePolicyStatement::valueOf));
        }
        spec.getPoliciesToCreate().addAll(message.getFormatPolicies());
        return spec;
    }

    public static RoleSpec valueOf(APIUpdateRoleMsg message) {
        RoleSpec spec = new RoleSpec();
        spec.setExists(true);
        spec.setUuid(message.getRoleUuid());
        spec.setName(message.getName());
        spec.setDescription(message.getDescription());
        spec.getPoliciesToCreate().addAll(message.getFormatPoliciesToCreate());
        spec.getPoliciesToDelete().addAll(message.getFormatPoliciesToDelete());
        spec.setClearPoliciesBeforeUpdate(message.isClearPoliciesBeforeUpdate());
        return spec;
    }

    public RoleVO buildVOWithoutPolicies() {
        RoleVO role = new RoleVO();
        role.setUuid(getUuid());
        role.setName(getName());
        role.setResourceName(getName());
        role.setDescription(getDescription());
        role.setType(getType());
        role.setAccountUuid(getAccountUuid());
        return role;
    }
}
