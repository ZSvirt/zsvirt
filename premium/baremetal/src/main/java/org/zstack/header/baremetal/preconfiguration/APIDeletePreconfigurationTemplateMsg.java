package org.zstack.header.baremetal.preconfiguration;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 2018-12-26.
 */
@RestRequest(
        path = "/baremetal/preconfigurations/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeletePreconfigurationTemplateEvent.class
)
public class APIDeletePreconfigurationTemplateMsg extends APIDeleteMessage implements PreconfigurationTemplateMessage {
    @APIParam(resourceType = PreconfigurationTemplateVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeletePreconfigurationTemplateMsg __example__() {
        APIDeletePreconfigurationTemplateMsg msg = new APIDeletePreconfigurationTemplateMsg();
        msg.setUuid(uuid(PreconfigurationTemplateVO.class));
        return msg;
    }

    @Override
    public String getTemplateUuid() {
        return uuid;
    }
}
