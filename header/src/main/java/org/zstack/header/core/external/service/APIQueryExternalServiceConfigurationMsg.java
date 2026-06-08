package org.zstack.header.core.external.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 1:36 AM
 */
@AutoQuery(replyClass = APIQueryExternalServiceConfigurationReply.class, inventoryClass = ExternalServiceConfigurationInventory.class)
@RestRequest(
        path = "/external/service/configurations",
        optionalPaths = {"/external/service/configurations/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryExternalServiceConfigurationReply.class
)
public class APIQueryExternalServiceConfigurationMsg extends APIQueryMessage {
    public static List<String> __example__() {return Collections.singletonList("uuid=" + uuid());}
}
