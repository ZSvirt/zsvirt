package org.zstack.vpc;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.vrouterRoute.VRouterRouteTableVO;

@RestRequest(
        path = "/vpc/virtual-routers/get-vpc-candidate",
        method = HttpMethod.GET,
        responseClass = APIGetRouteTableVpcVRouterCandidateReply.class
)
public class APIGetRouteTableVpcVRouterCandidateMsg extends APIGetMessage {
    @APIParam(required = false, resourceType = VRouterRouteTableVO.class)
    private String tableUuid;

    public String getTableUuid() {
        return tableUuid;
    }

    public void setTableUuid(String tableUuid) {
        this.tableUuid = tableUuid;
    }

    public static APIGetRouteTableVpcVRouterCandidateMsg __example__() {
        APIGetRouteTableVpcVRouterCandidateMsg msg = new APIGetRouteTableVpcVRouterCandidateMsg();
        return msg;
    }
}

