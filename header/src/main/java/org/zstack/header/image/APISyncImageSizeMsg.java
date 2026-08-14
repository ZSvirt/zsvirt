package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/5/6.
 */
@RestRequest(
        path = "/images/{uuid}/actions",
        responseClass = APISyncImageSizeEvent.class,
        method = HttpMethod.PUT,
        isAction = true
)
public class APISyncImageSizeMsg extends APIMessage implements ImageMessage {
    @APIParam(resourceType = ImageVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getImageUuid() {
        return uuid;
    }
 
    public static APISyncImageSizeMsg __example__() {
        APISyncImageSizeMsg msg = new APISyncImageSizeMsg();

        msg.setUuid(uuid());

        return msg;
    }

}
