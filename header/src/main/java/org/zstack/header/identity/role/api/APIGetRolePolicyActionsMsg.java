package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by Wenhao.Zhang on 2024/08/30
 */
@RestRequest(
        path = "/identities/role/policy-actions",
        method = HttpMethod.GET,
        responseClass = APIGetRolePolicyActionsReply.class
)
public class APIGetRolePolicyActionsMsg extends APISyncCallMessage {
    @APIParam(required = false)
    private boolean showAllPolicies;

    public boolean isShowAllPolicies() {
        return showAllPolicies;
    }

    public void setShowAllPolicies(boolean showAllPolicies) {
        this.showAllPolicies = showAllPolicies;
    }

    public static APIGetRolePolicyActionsMsg __example__() {
        return new APIGetRolePolicyActionsMsg();
    }
}
