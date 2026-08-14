package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.role.RolePolicyStatement;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kayo on 2018/7/26.
 */
@RestRequest(path = "/identities/roles/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateRoleEvent.class)
public class APIUpdateRoleMsg extends APIMessage implements RoleMessage {
    @APIParam(resourceType = RoleVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(required = false)
    private List<Object> createPolicies;
    @APIParam(required = false)
    private boolean clearPoliciesBeforeUpdate = false;
    @APIParam(required = false)
    private List<Object> deletePolicies;

    @APINoSee
    private List<RolePolicyStatement> formatPoliciesToCreate;
    @APINoSee
    private List<RolePolicyStatement> formatPoliciesToDelete;

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

    public List<Object> getCreatePolicies() {
        return createPolicies;
    }

    public void setCreatePolicies(List<Object> createPolicies) {
        this.createPolicies = createPolicies;
    }

    public boolean isClearPoliciesBeforeUpdate() {
        return clearPoliciesBeforeUpdate;
    }

    public void setClearPoliciesBeforeUpdate(boolean clearPoliciesBeforeUpdate) {
        this.clearPoliciesBeforeUpdate = clearPoliciesBeforeUpdate;
    }

    public List<Object> getDeletePolicies() {
        return deletePolicies;
    }

    public void setDeletePolicies(List<Object> deletePolicies) {
        this.deletePolicies = deletePolicies;
    }

    public List<RolePolicyStatement> getFormatPoliciesToCreate() {
        return formatPoliciesToCreate;
    }

    public void setFormatPoliciesToCreate(List<RolePolicyStatement> formatPoliciesToCreate) {
        this.formatPoliciesToCreate = formatPoliciesToCreate;
    }

    public List<RolePolicyStatement> getFormatPoliciesToDelete() {
        return formatPoliciesToDelete;
    }

    public void setFormatPoliciesToDelete(List<RolePolicyStatement> formatPoliciesToDelete) {
        this.formatPoliciesToDelete = formatPoliciesToDelete;
    }

    public static APIUpdateRoleMsg __example__() {
        APIUpdateRoleMsg msg = new APIUpdateRoleMsg();
        msg.setUuid(uuid());
        msg.setName("role-1");
        msg.setDescription("role for test");

        return msg;
    }

    @Override
    public String getRoleUuid() {
        return uuid;
    }
}
