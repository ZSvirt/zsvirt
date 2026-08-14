package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryAddressPoolReply.class, inventoryClass = AddressPoolInventory.class)
@RestRequest(
        path = "/l3-networks/address-pools",
        optionalPaths = {"/l3-networks/address-pools/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAddressPoolReply.class
)
public class APIQueryAddressPoolMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList(String.format("uuid=" + uuid()));
    }

}
