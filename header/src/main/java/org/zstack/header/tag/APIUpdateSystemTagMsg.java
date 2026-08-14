package org.zstack.header.tag;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by frank on 8/17/2015.
 */
@RestRequest(
        path = "/system-tags/{uuid}/actions",
        responseClass = APIUpdateSystemTagEvent.class,
        isAction = true,
        method = HttpMethod.PUT
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "SystemTagUuidToVmUuidResolver", field = "uuid")
public class APIUpdateSystemTagMsg extends APIMessage {
    @APIParam(resourceType = SystemTagVO.class)
    private String uuid;
    @APIParam
    private String tag;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
 
    public static APIUpdateSystemTagMsg __example__() {
        APIUpdateSystemTagMsg msg = new APIUpdateSystemTagMsg();
        msg.setUuid(uuid());
        msg.setTag("for-large-DB");
        return msg;
    }

}
