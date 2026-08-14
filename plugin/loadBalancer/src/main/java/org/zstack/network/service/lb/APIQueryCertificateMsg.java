package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by shixin on 03/22/2018.
 */
@AutoQuery(replyClass = APIQueryCertificateReply.class, inventoryClass = CertificateInventory.class)
@RestRequest(
        path = "/certificates",
        optionalPaths = {"/certificates/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryCertificateReply.class
)
public class APIQueryCertificateMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }

}
