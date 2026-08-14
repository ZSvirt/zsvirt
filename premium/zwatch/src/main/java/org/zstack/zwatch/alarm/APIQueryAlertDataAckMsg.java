package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.ArrayList;
import java.util.List;

@AutoQuery(replyClass = APIQueryAlertDataAckReply.class, inventoryClass = AlertDataAckInventory.class)
@RestRequest(
        path = "/zwatch/alert-histories/acknowledgments",
        optionalPaths = {"/zwatch/alert-histories/acknowledgments/{alertDataUuid}"},
        responseClass = APIQueryAlertDataAckReply.class,
        method = HttpMethod.GET)
public class APIQueryAlertDataAckMsg extends APIQueryMessage {
    public static List<String> __example__() {
        List<String> ret = new ArrayList<>();
        ret.add(String.format("name=VM"));
        return ret;
    }
}
