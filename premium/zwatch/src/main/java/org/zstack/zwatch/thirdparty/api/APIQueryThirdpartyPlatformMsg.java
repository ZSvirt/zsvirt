package org.zstack.zwatch.thirdparty.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformInventory;

import java.util.ArrayList;
import java.util.List;

@AutoQuery(replyClass = APIQueryThirdpartyPlatformReply.class, inventoryClass = ThirdpartyPlatformInventory.class)
@RestRequest(path = "/zwatch/third-party/platforms", optionalPaths = {"/zwatch/third-party/platforms/{uuid}"},
        responseClass = APIQueryThirdpartyPlatformReply.class, method = HttpMethod.GET)
public class APIQueryThirdpartyPlatformMsg extends APIQueryMessage {

    public static List<String> __example__() {
        List<String> ret = new ArrayList<>();
        ret.add(String.format("product=xsky"));
        return ret;
    }
}
