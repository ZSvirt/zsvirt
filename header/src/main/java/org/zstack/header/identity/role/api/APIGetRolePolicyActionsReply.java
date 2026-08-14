package org.zstack.header.identity.role.api;

import org.zstack.header.identity.role.RoleInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by Wenhao.Zhang on 2024/08/30
 */
@RestResponse(fieldsTo = "all")
public class APIGetRolePolicyActionsReply extends APIReply {
    @NoLogging
    private List<String> policies;
    @NoLogging
    private List<RoleInventory> roles;

    public List<String> getPolicies() {
        return policies;
    }

    public void setPolicies(List<String> policies) {
        this.policies = policies;
    }

    public List<RoleInventory> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleInventory> roles) {
        this.roles = roles;
    }

    public static APIGetRolePolicyActionsReply __example__() {
        APIGetRolePolicyActionsReply reply = new APIGetRolePolicyActionsReply();
        reply.setPolicies(list(
            ".header.identity.APIChangeResourceOwnerMsg",
            ".header.identity.APICheckResourcePermissionMsg",
            ".header.identity.APICreateAccountMsg",
            ".header.identity.APIDeleteAccountMsg",
            ".header.identity.APIGetAccountQuotaUsageMsg",
            ".header.identity.APIGetResourceAccountMsg",
            ".header.identity.APILogInByAccountMsg",
            ".header.identity.APILogOutMsg",
            ".header.identity.APIQueryAccountMsg",
            ".header.identity.APIQueryAccountResourceRefMsg",
            ".header.identity.APIQueryQuotaMsg",
            ".header.identity.APIRenewSessionMsg",
            ".header.identity.APIRevokeResourceSharingMsg",
            ".header.identity.APIShareResourceMsg",
            ".header.identity.APIUpdateAccountMsg",
            ".header.identity.APIUpdateQuotaMsg",
            ".header.identity.APIValidateSessionMsg"
        ));
        return reply;
    }
}
