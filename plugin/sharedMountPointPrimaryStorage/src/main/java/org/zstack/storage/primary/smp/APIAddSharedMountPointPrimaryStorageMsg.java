package org.zstack.storage.primary.smp;

import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;

/**
 * Created by xing5 on 2016/3/27.
 */
@TagResourceType(PrimaryStorageVO.class)
@RestRequest(
        path = "/primary-storage/smp",
        method = HttpMethod.POST,
        responseClass = APIAddPrimaryStorageEvent.class,
        parameterName = "params"
)
public class APIAddSharedMountPointPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
    public APIAddSharedMountPointPrimaryStorageMsg() {
        this.setType(SMPConstants.SMP_TYPE);
    }

    @Override
    public String getType() {
        return SMPConstants.SMP_TYPE;
    }
 
    public static APIAddSharedMountPointPrimaryStorageMsg __example__() {
        APIAddSharedMountPointPrimaryStorageMsg msg = new APIAddSharedMountPointPrimaryStorageMsg();
        msg.setName("smp");
        msg.setUrl("/smp_mountpoint");
        msg.setZoneUuid(uuid());
        return msg;
    }

}
