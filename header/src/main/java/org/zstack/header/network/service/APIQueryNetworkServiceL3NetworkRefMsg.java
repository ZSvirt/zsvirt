package org.zstack.header.network.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryNetworkServiceL3NetworkRefReply.class, inventoryClass = NetworkServiceL3NetworkRefInventory.class)
@RestRequest(
        path = "/l3-networks/network-services/refs",
        method = HttpMethod.GET,
        responseClass = APIQueryNetworkServiceL3NetworkRefReply.class
)
public class APIQueryNetworkServiceL3NetworkRefMsg extends APIQueryMessage {


    public static List<String> __example__() {
        return asList();
    }

}
