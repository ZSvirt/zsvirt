package org.zstack.zwatch.thirdparty.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.thirdparty.entity.SNSEndpointThirdpartyAlertHistoryInventory;

import java.util.ArrayList;
import java.util.List;

@AutoQuery(
        replyClass = APIQuerySNSEndpointThirdpartyAlertHistoryReply.class,
        inventoryClass = SNSEndpointThirdpartyAlertHistoryInventory.class)
@RestRequest(
        method = HttpMethod.GET,
        path = "/zwatch/third-party/alert-publish-histories",
        responseClass = APIQuerySNSEndpointThirdpartyAlertHistoryReply.class)
public class APIQuerySNSEndpointThirdpartyAlertHistoryMsg extends APIQueryMessage {

    public static List<String> __example__() {
        List<String> ret = new ArrayList<>();
        ret.add(String.format("endpointUuid=%s", uuid()));
        return ret;
    }
}
