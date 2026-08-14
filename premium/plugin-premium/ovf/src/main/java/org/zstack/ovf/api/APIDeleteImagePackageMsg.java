package org.zstack.ovf.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.ovf.datatype.ImagePackageVO;

/**
 * Created by Qi Le on 2022/4/26
 */
@RestRequest(
        path = "/image-packages/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteImagePackageEvent.class
)
public class APIDeleteImagePackageMsg extends APIDeleteMessage {
    @APIParam(resourceType = ImagePackageVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteImagePackageMsg __example__() {
        APIDeleteImagePackageMsg msg = new APIDeleteImagePackageMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
