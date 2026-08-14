package org.zstack.network.service.vip;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:34 PM
 * To change this template use File | Settings | File Templates.
 */
@AutoQuery(replyClass = APIQueryVipReply.class, inventoryClass = VipInventory.class)
@RestRequest(
        path = "/vips",
        optionalPaths = {"/vips/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVipReply.class
)
public class APIQueryVipMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }

}
