package org.zstack.storage.primary.local;

import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;

/**
 * Created by frank on 7/1/2015.
 */
@TagResourceType(PrimaryStorageVO.class)
@RestRequest(
        path = "/primary-storage/local-storage",
        method = HttpMethod.POST,
        responseClass = APIAddPrimaryStorageEvent.class,
        parameterName = "params"
)
public class APIAddLocalPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
    @Override
    public String getType() {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE;
    }

    public static APIAddLocalPrimaryStorageMsg __example__() {
        APIAddLocalPrimaryStorageMsg msg = new APIAddLocalPrimaryStorageMsg();

        msg.setZoneUuid(uuid());
        msg.setName("PS1");
        msg.setUrl("/zstack_ps");

        return msg;
    }

}
