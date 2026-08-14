package org.zstack.iam1.api.ensemble;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;
import org.zstack.iam1.entity.ensemble.ResourceEnsembleInventory;

import java.util.concurrent.TimeUnit;

/**
 * Created by Wenhao.Zhang on 2024/08/06
 */
@RestRequest(
        path = "/iam1/resource-ensemble",
        optionalPaths = {"/iam1/resource-ensemble/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIGetResourceEnsembleMembersReply.class
)
@DefaultTimeout(timeunit = TimeUnit.MINUTES, value = 5)
public class APIGetResourceEnsembleMembersMsg extends APISyncCallMessage {
    @APIParam(resourceType = ResourceVO.class, scope = APIParam.SCOPE_ALLOWED_SHARING)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIGetResourceEnsembleMembersMsg __example__() {
        APIGetResourceEnsembleMembersMsg msg = new APIGetResourceEnsembleMembersMsg();
        msg.setUuid("14c61568f49a45759c9a75c8fea4f854");
        return msg;
    }
}
