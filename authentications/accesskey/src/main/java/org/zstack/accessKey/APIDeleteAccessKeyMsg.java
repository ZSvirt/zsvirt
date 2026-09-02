package org.zstack.accessKey;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/accesskeys/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAccessKeyEvent.class)
public class APIDeleteAccessKeyMsg extends APIDeleteMessage  {
    @APIParam(successIfResourceNotExisting = true, resourceType = AccessKeyVO.class)
    private String uuid;

    public static APIDeleteAccessKeyMsg __example__() {
        APIDeleteAccessKeyMsg ret = new APIDeleteAccessKeyMsg();
        ret.uuid = uuid();
        return ret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
