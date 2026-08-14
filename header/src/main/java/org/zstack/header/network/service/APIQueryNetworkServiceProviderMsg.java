package org.zstack.header.network.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryNetworkServiceProviderReply.class, inventoryClass = NetworkServiceProviderInventory.class)
@RestRequest(
        path = "/network-services/providers",
        method = HttpMethod.GET,
        responseClass = APIQueryNetworkServiceProviderReply.class
)
public class APIQueryNetworkServiceProviderMsg extends APIQueryMessage {

 
    public static List<String> __example__() {
        return asList();
    }

}
