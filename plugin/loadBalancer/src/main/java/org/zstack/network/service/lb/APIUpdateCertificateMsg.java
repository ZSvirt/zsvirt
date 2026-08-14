package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by shixin on 04/12/2018.
 */
@RestRequest(
        path = "/certificates/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateCertificateEvent.class,
        isAction = true
)
public class APIUpdateCertificateMsg extends APICreateMessage {
    @APIParam(resourceType = CertificateVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static APIUpdateCertificateMsg __example__() {
        APIUpdateCertificateMsg msg = new APIUpdateCertificateMsg();
        msg.uuid = uuid();
        msg.setDescription("info");
        msg.setName("Test-Cer");

        return msg;
    }
}
