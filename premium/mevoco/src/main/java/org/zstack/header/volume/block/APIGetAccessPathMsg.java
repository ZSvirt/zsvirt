package org.zstack.header.volume.block;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;

/**
 * @author shenjin
 * @date 2023/6/14 10:20
 */
@RestRequest(
        path = "/block-volumes/access/path",
        method = HttpMethod.GET,
        responseClass = APIGetAccessPathReply.class
)
public class APIGetAccessPathMsg extends APISyncCallMessage {
    @APIParam(required = true, resourceType = PrimaryStorageVO.class)
    String primaryStorageUuid;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public static APIGetAccessPathMsg __example__() {
        APIGetAccessPathMsg msg = new APIGetAccessPathMsg();
        msg.setPrimaryStorageUuid(uuid());
        return msg;
    }
}
