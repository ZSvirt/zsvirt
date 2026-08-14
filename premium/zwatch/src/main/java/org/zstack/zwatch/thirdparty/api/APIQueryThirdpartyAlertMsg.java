package org.zstack.zwatch.thirdparty.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyOriginalAlertInventory;

import java.util.ArrayList;
import java.util.List;

@AutoQuery(replyClass = APIQueryThirdpartyAlertReply.class, inventoryClass = ThirdpartyOriginalAlertInventory.class)
@RestRequest(
        path = "/zwatch/third-party/alerts",
        optionalPaths = {"/zwatch/third-party/alerts/{uuid}"},
        responseClass = APIQueryThirdpartyAlertReply.class,
        method = HttpMethod.GET)
public class APIQueryThirdpartyAlertMsg extends APIQueryMessage {

    public static List<String> __example__() {
        List<String> ret = new ArrayList<>();
        ret.add(String.format("uuid=d4904ace98f834e7bf3485376742133f"));
        return ret;
    }
}
