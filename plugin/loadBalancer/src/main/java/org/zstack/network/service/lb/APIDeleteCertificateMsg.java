package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by shixin on 03/22/2015.
 */
@RestRequest(
        path = "/certificates/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteCertificateEvent.class
)
public class APIDeleteCertificateMsg extends APIDeleteMessage {
    @APIParam(resourceType = CertificateVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIDeleteCertificateMsg __example__() {
        APIDeleteCertificateMsg msg = new APIDeleteCertificateMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
